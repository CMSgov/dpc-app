package gov.cms.dpc.aggregation.engine;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.typesafe.config.Config;
import gov.cms.dpc.bluebutton.client.BlueButtonClient;
import gov.cms.dpc.common.annotations.ExportPath;
import gov.cms.dpc.queue.JobQueue;
import gov.cms.dpc.queue.JobStatus;
import gov.cms.dpc.queue.Pair;
import gov.cms.dpc.queue.exceptions.JobQueueFailure;
import gov.cms.dpc.queue.models.JobModel;
import gov.cms.dpc.queue.models.JobResult;
import io.github.resilience4j.retry.RetryConfig;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.Subject;
import io.reactivex.subjects.UnicastSubject;
import org.hl7.fhir.dstu3.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.CipherOutputStream;
import javax.inject.Inject;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The top level of the Aggregation Engine
 */
public class AggregationEngine implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(AggregationEngine.class);
    private static final char DELIM = '\n';

    private final String exportPath;
    private final JobQueue queue;
    private final BlueButtonClient bbclient;
    private final RetryConfig retryConfig;
    private final Config config;
    private final IParser jsonParser;
    private final int resourcesPerFile;
    private Disposable subscribe;
    private boolean encryptionEnabled;

    /**
     * Create an engine
     *
     * @param bbclient    - {@link BlueButtonClient } to use
     * @param queue       - {@link JobQueue} that will direct the work done
     * @param fhirContext - {@link FhirContext} for DSTU3 resources
     * @param exportPath  - The {@link ExportPath} to use for writing the output files
     * @param retryConfig - {@link RetryConfig} injected config for setting up retry handler
     */
    @Inject
    public AggregationEngine(BlueButtonClient bbclient, JobQueue queue, FhirContext fhirContext, @ExportPath String exportPath, Config config, RetryConfig retryConfig) {
        this.queue = queue;
        this.bbclient = bbclient;
        this.exportPath = exportPath;
        this.config = config;
        this.retryConfig = retryConfig;
        this.jsonParser = fhirContext.newJsonParser();
        this.resourcesPerFile = config.hasPath("resourcesPerFile") ? config.getInt("resourcesPerFile") : 1000;
        this.encryptionEnabled = config.hasPath("encryption.enabled") && config.getBoolean("encryption.enabled");
    }

    /**
     * Run the engine. Part of the Runnable interface.
     */
    @Override
    public void run() {
        // Run loop
        logger.info("Starting aggregation engine with exportPath:\"{}\"", exportPath);
        this.pollQueue();
    }

    /**
     * Stop the engine
     */
    public void stop() {
        logger.info("Shutting down aggregation engine");
        this.subscribe.dispose();
    }

   /**
     * Form the full file name of an output file
     * @param jobID        - {@link UUID} ID of export job
     * @param resourceType - {@link ResourceType} to append to filename
     * @param sequence     - batch sequence number
     * @return return the path
     */
    String formOutputFilePath(UUID jobID, ResourceType resourceType, int sequence) {
        return String.format("%s/%s.ndjson", exportPath, JobResult.formOutputFileName(jobID, resourceType, sequence));
    }

    String formEncryptedOutputFilePath(UUID jobID, ResourceType resourceType, int sequence) {
        return String.format("%s/%s.ndjson.enc", exportPath, JobResult.formOutputFileName(jobID, resourceType, sequence));
    }

    String formEncryptedMetadataPath(UUID jobID, ResourceType resourceType, int sequence) {
        return String.format("%s/%s-metadata.json", exportPath, JobResult.formOutputFileName(jobID, resourceType, sequence));
    }

    /**
     * The main run-loop of the engine
     */
    private void pollQueue() {
        subscribe = Observable.fromCallable(this.queue::workJob)
                .doOnNext(job -> logger.trace("Polling queue for job"))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .repeatWhen(completed -> {
                    logger.debug("No job, retrying in 2 seconds");
                    return completed.delay(2, TimeUnit.SECONDS);
                })
                .doOnError(e -> logger.error("Error", e))
                .subscribe(this::workExportJob,
                        error -> logger.error("Unable to complete job.", error));
    }

    /**
     * Wrapper method for dispatching the job and handling any errors that would cause the job to fail
     *
     * @param workPair - job {@link Pair} with {@link UUID} or {@link JobModel}
     */
    private void workExportJob(Pair<UUID, JobModel> workPair) {
        final JobModel model = workPair.getRight();
        completeJob(model);
    }

    /**
     * Work a single job in the queue to completion
     *
     * @param job - the job to execute
     */
    void completeJob(JobModel job) {
        final UUID jobID = job.getJobID();
        logger.info("Processing job {}, exporting to: {}.", jobID, this.exportPath);

        List<String> attributedBeneficiaries = job.getPatients();
        logger.debug("Has {} attributed beneficiaries", attributedBeneficiaries.size());

        final AtomicInteger errorCounter = new AtomicInteger();
        final Subject<Resource> errorSubject = UnicastSubject.<Resource>create().toSerialized();
        final Observable<JobResult> errorResult = errorSubject
                .buffer(resourcesPerFile)
                .map(batch -> writeBatch(job, ResourceType.OperationOutcome, errorCounter, batch));

        final var results = new ArrayList<JobResult>();
        Observable.fromIterable(job.getResourceTypes())
                .flatMap(resourceType -> completeResource(job, resourceType, errorSubject))
                .doFinally(errorSubject::onComplete)
                .concatWith(errorResult)
                .blockingSubscribe(
                        // onNext
                        results::add,
                        // onError
                        error -> {
                            logger.error("FAILED job {}", jobID, error);
                            this.queue.completeJob(jobID, JobStatus.FAILED, List.of());
                        },
                        // onComplete
                        () -> {
                            logger.info("COMPLETED job {}", jobID);
                            this.queue.completeJob(jobID, JobStatus.COMPLETED, results);
                        });

    }

    /**
     * Fetch and write a specific resource type
     * @param job context
     * @param resourceType to process
     * @param errorSubject to record errors from Blue Button
     * @return A new job result observable
     */
    private Observable<JobResult> completeResource(JobModel job, ResourceType resourceType, Subject<Resource> errorSubject) {
        final var counter = new AtomicInteger();
        final var fetcher = new ResourceFetcher(bbclient, retryConfig, job.getJobID(), resourceType, errorSubject);
        return Observable.fromIterable(job.getPatients())
                .subscribeOn(Schedulers.io())
                .flatMap(fetcher::fetchResources)
                .concatMap(fetcher::unpackBundles)
                .buffer(resourcesPerFile)
                .map(batch -> writeBatch(job, resourceType, counter, batch));
    }

    /**
     * Write a batch of resources to a file. Encrypt if the encryption is enabled.
     *
     * @param job context
     * @param resourceType to write
     * @param batch is the list of resources to write
     * @param counter is general counter for batch number
     * @return The JobResult associated with this file
     */
    private JobResult writeBatch(JobModel job, ResourceType resourceType, AtomicInteger counter, List<Resource> batch) {
        try {
            final var jobID = job.getJobID();
            final var byteStream = new ByteArrayOutputStream();
            final var sequence = counter.getAndIncrement();

            OutputStream writer = encryptionEnabled ? formCipherStream(byteStream, job, resourceType, sequence): byteStream;
            String outputPath = encryptionEnabled ? formEncryptedOutputFilePath(jobID, resourceType, sequence): formOutputFilePath(jobID, resourceType, sequence);
            for (var resource: batch) {
                final String str = jsonParser.encodeResourceToString(resource);
                writer.write(str.getBytes(StandardCharsets.UTF_8));
                writer.write(DELIM);
            }
            writer.flush();
            writer.close();
            writeToFile(byteStream.toByteArray(), outputPath);

            logger.debug("Finished writing to '{}'", outputPath);
            return new JobResult(jobID, resourceType, sequence, batch.size());
        } catch(IOException ex) {
            throw new JobQueueFailure(job.getJobID(), "IO error writing a resource", ex);
        } catch(SecurityException ex) {
            throw new JobQueueFailure(job.getJobID(), "Error encrypting a resource", ex);
        } catch(Exception ex) {
            throw new JobQueueFailure(job.getJobID(), "General failure consuming a resource", ex);
        }
    }

    /**
     * Build an encrypting stream that contains an inner stream.
     *
     * @param writer is the inner stream to write to
     * @param job is the context including the RSA key
     * @param resourceType is the type of resource being written
     * @param sequence is the batch sequence being written
     * @return a output stream to write to
     * @throws GeneralSecurityException if there is something wrong with the encryption config
     * @throws IOException if there is something wrong with the file io.
     */
    private OutputStream formCipherStream(OutputStream writer, JobModel job, ResourceType resourceType, int sequence) throws GeneralSecurityException, IOException {
        final var metadataPath = formEncryptedMetadataPath(job.getJobID(), resourceType, sequence);
        try(final CipherBuilder cipherBuilder = new CipherBuilder(config);
            final FileOutputStream metadataWriter = new FileOutputStream(metadataPath)) {
            cipherBuilder.generateKeyMaterial();
            final String json = cipherBuilder.getMetadata(job.getRsaPublicKey());
            metadataWriter.write(json.getBytes(StandardCharsets.UTF_8));
            return new CipherOutputStream(writer, cipherBuilder.formCipher());
        }
    }

    /**
     * Write a array of bytes to a file. Name the file according to the supplied name
     *
     * @param bytes - Bytes to write
     * @param fileName - The fileName to write too
     * @throws IOException - If the write fails
     */
    private void writeToFile(byte[] bytes, String fileName) throws IOException {
        if (bytes.length == 0) {
            return;
        }
        try (final var outputFile = new FileOutputStream(fileName)) {
            outputFile.write(bytes);
            outputFile.flush();
        }
    }
}
