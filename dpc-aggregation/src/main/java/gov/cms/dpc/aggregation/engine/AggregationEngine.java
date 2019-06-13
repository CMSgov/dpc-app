package gov.cms.dpc.aggregation.engine;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import com.typesafe.config.Config;
import gov.cms.dpc.bluebutton.client.BlueButtonClient;
import gov.cms.dpc.common.annotations.ExportPath;
import gov.cms.dpc.queue.JobQueue;
import gov.cms.dpc.queue.JobStatus;
import gov.cms.dpc.queue.Pair;
import gov.cms.dpc.queue.exceptions.JobQueueFailure;
import gov.cms.dpc.queue.models.JobModel;
import gov.cms.dpc.queue.models.JobResult;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.reactivex.Emitter;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

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
     * @param context     - {@link FhirContext} for DSTU3 resources
     * @param exportPath  - The {@link ExportPath} to use for writing the output files
     * @param retryConfig - {@link RetryConfig} injected config for setting up retry handler
     */
    @Inject
    public AggregationEngine(BlueButtonClient bbclient, JobQueue queue, FhirContext context, @ExportPath String exportPath, Config config, RetryConfig retryConfig) {
        this.queue = queue;
        this.bbclient = bbclient;
        this.exportPath = exportPath;
        this.config = config;
        this.retryConfig = retryConfig;
        this.jsonParser = context.newJsonParser();
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
     *
     * @param jobID        - {@link UUID} ID of export job
     * @param resourceType - {@link ResourceType} to append to filename
     */
    public String formOutputFilePath(UUID jobID, ResourceType resourceType) {
        return String.format("%s/%s.ndjson", exportPath, JobModel.formOutputFileName(jobID, resourceType));
    }

    public String formEncryptedOutputFilePath(UUID jobID, ResourceType resourceType) {
        return String.format("%s/%s.ndjson.enc", exportPath, JobModel.formOutputFileName(jobID, resourceType));
    }

    public String formEncryptedMetadataPath(UUID jobID, ResourceType resourceType) {
        return String.format("%s/%s-metadata.json", exportPath, JobModel.formOutputFileName(jobID, resourceType));
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
    public void completeJob(JobModel job) {
        final UUID jobID = job.getJobID();
        logger.info("Processing job {}, exporting to: {}.", jobID, this.exportPath);

        List<String> attributedBeneficiaries = job.getPatients();
        logger.debug("Has {} attributed beneficiaries", attributedBeneficiaries.size());

        final Subject<Resource> errorSubject = UnicastSubject.<Resource>create().toSerialized();
        final Observable<JobResult> errorResult = errorSubject
                .buffer(resourcesPerFile)
                .map(batch -> writeBatch(job, ResourceType.OperationOutcome, batch));

        Observable.fromIterable(job.getResourceTypes())
                .flatMap(resourceType -> completeResource(job, resourceType, errorSubject))
                .doFinally(errorSubject::onComplete)
                .concatWith(errorResult)
                .blockingSubscribe(
                        // onNext
                        job::addJobResult,
                        // onError
                        error -> {
                            logger.error("Cannot process job {}", jobID, error);
                            this.queue.completeJob(jobID, JobStatus.FAILED, job.getJobResults());
                        },
                        // onComplete
                        () -> {
                            this.queue.completeJob(jobID, JobStatus.COMPLETED, job.getJobResults());
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
        return Observable.fromIterable(job.getPatients())
                .flatMap(patient -> this.fetchResources(job.getJobID(), patient, resourceType, errorSubject))
                .subscribeOn(Schedulers.io())
                .concatMap(this::unpackBundles)
                .buffer(resourcesPerFile)
                .map(batch -> writeBatch(job, resourceType, batch));
    }

    /**
     * Fetches the given resource from the {@link BlueButtonClient} and converts it from FHIR-JSON to Resource. The
     * resource may be a type requested or it may be an operational outcome;
     *
     * @param jobID        - {@link UUID} jobID
     * @param patientID    - {@link String} patient ID
     * @param resourceType - {@link ResourceType} to fetch from BlueButton
     * @return - {@link Observable} of {@link Resource} to pass back to reactive loop.
     */
    private Observable<Resource> fetchResources(UUID jobID, String patientID, ResourceType resourceType, Subject<Resource> errorSubject) {
        return Observable.create(emitter -> {
            try {
                // replace with a switch expression in Java 12
                Function<String, Resource> bbMethod;
                switch (resourceType) {
                    case Patient:
                        bbMethod = this.bbclient::requestPatientFromServer;
                        break;
                    case ExplanationOfBenefit:
                        bbMethod = this.bbclient::requestEOBFromServer;
                        break;
                    case Coverage:
                        bbMethod = this.bbclient::requestCoverageFromServer;
                        break;
                    default:
                        throw new JobQueueFailure(jobID, "Unexpected resource type: " + resourceType.toString());
                }

                // Fetch the resource in a retry loop
                logger.debug("Fetching {} from BlueButton on thread {} for {}", resourceType.toString(), Thread.currentThread().getName(), jobID);
                Retry retry = Retry.of("bb-resource-fetcher", this.retryConfig);
                final var fetchFirstDecorated = Retry.decorateFunction(retry, bbMethod);
                final Resource firstResource = fetchFirstDecorated.apply(patientID);
                emitter.onNext(firstResource);

                // If this is a bundle, fetch the next bundles
                if (firstResource.getResourceType() == ResourceType.Bundle) {
                    fetchAllNextBundles(emitter, (Bundle)firstResource, resourceType, patientID, errorSubject);
                }

                // All done
                emitter.onComplete();
            } catch (JobQueueFailure ex) {
                // Fatal for this job
                emitter.onError(ex);
            } catch(Exception ex){
                // Otherwise, capture the BB error.
                // Per patient errors are not fatal for the job, just turn them into operation outcomes
                logger.error("Error fetching from Blue Button for a patient", ex);
                errorSubject.onNext(formOperationOutcome(resourceType, patientID, ex));
                emitter.onComplete();
            }
        });
    }

    /**
     *  Fetch the all the next bundles if there are any.
     *
     * @param emitter to write the bundles to
     * @param firstBundle to get the next link
     * @param resourceType that we are fetching
     * @param patientID for this patient
     * @param errorSubject write operational error here
     */
    private void fetchAllNextBundles(Emitter<Resource> emitter,
                                     Bundle firstBundle,
                                     ResourceType resourceType,
                                     String patientID,
                                     Subject<Resource> errorSubject) {
        for (var bundle = firstBundle; bundle.getLink(Bundle.LINK_NEXT) != null; ) {
            Retry retry = Retry.of("bb-resource-fetcher", this.retryConfig);
            final var decorated = Retry.decorateFunction(retry, this.bbclient::requestNextBundleFromServer);
            try {
                logger.debug("Fetching next {} from BlueButton on thread {}", resourceType.toString(), Thread.currentThread().getName());
                bundle = decorated.apply(bundle);
                emitter.onNext(bundle);
            } catch (Exception ex) {
                errorSubject.onNext(formOperationOutcome(resourceType, patientID, ex));
                logger.error("Error fetching the next bundle from Blue Button", ex);
            }
        }
    }

    /**
     * Check for bundle resources. Unpack bundle resources into their constituent resources, otherwise do nothing. Used
     * in conjuction with flatMap or concatMap operators.
     *
     * @param resource - The resource to examine and possibly unpack
     * @return A stream of resources
     */
    private Observable<Resource> unpackBundles(Resource resource) {
        if (resource.getResourceType() != ResourceType.Bundle) {
            return Observable.just(resource);
        }
        final Bundle bundle = (Bundle)resource;
        final Resource[] entries = bundle.getEntry().stream().map(Bundle.BundleEntryComponent::getResource).toArray(Resource[]::new);
        return Observable.fromArray(entries);
    }

    /**
     * Write a batch of resources to a file. Encrypt if the encryption is enabled.
     *
     * @param job context
     * @param resourceType to write
     * @param batch is the list of resources to write
     * @return The JobResult associated with this file
     */
    private JobResult writeBatch(JobModel job, ResourceType resourceType, List<Resource> batch) {
        try {
            final var jobID = job.getJobID();
            final var jobResult = new JobResult(jobID, resourceType, 0, batch.size());
            final var byteStream = new ByteArrayOutputStream();

            OutputStream writer = byteStream;
            String outputPath = formOutputFilePath(jobID, resourceType);
            if (encryptionEnabled) {
                outputPath = formEncryptedOutputFilePath(jobID, resourceType);
                writer = formCipherStream(writer, job, resourceType);
            }

            for (var resource: batch) {
                final String str = jsonParser.encodeResourceToString(resource);
                writer.write(str.getBytes(StandardCharsets.UTF_8));
                writer.write(DELIM);
            }
            writer.flush();
            writer.close();
            writeToFile(byteStream.toByteArray(), outputPath);

            logger.debug("Finished writing to '{}' on thread {}", outputPath, Thread.currentThread().getName());
            return jobResult;
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
     * @return a output stream to write to
     * @throws GeneralSecurityException if there is something wrong with the encryption config
     * @throws IOException if there is something wrong with the file io.
     */
    private OutputStream formCipherStream(OutputStream writer, JobModel job, ResourceType resourceType) throws GeneralSecurityException, IOException {
        final var metadataPath = formEncryptedMetadataPath(job.getJobID(), resourceType);
        try(final CipherBuilder cipherBuilder = new CipherBuilder(config);
            final FileOutputStream metadataWriter = new FileOutputStream(metadataPath)) {
            cipherBuilder.generateKeyMaterial();
            final String json = cipherBuilder.getMetadata(job.getRsaPublicKey());
            metadataWriter.write(json.getBytes(StandardCharsets.UTF_8));
            return new CipherOutputStream(writer, cipherBuilder.formCipher());
        }
    }

    /**
     * Create a OperationalOutcome resource from an exception with a patient
     *
     * @param resourceType - the resource type that was trying to be fetched
     * @param patientID - the id of the patient involved in the error
     * @param ex        - the exception to turn into a Operational Outcome
     * @return an operation outcome
     */
    private OperationOutcome formOperationOutcome(ResourceType resourceType, String patientID, Throwable ex) {
        String details;
        if (ex instanceof ResourceNotFoundException) {
            details = String.format("%s resource not found in Blue Button for id: %s", resourceType.toString(), patientID);
        } else if (ex instanceof BaseServerResponseException) {
            final var serverException = (BaseServerResponseException) ex;
            details = String.format("Blue Button error fetching %s resource. HTTP return code: %s", resourceType.toString(), serverException.getStatusCode());
        } else {
            details = String.format("Internal error: %s", ex.getMessage());
        }

        final var patientLocation = List.of(new StringType("Patient"), new StringType("id"), new StringType(patientID));
        final var outcome = new OperationOutcome();
        outcome.addIssue()
                .setSeverity(OperationOutcome.IssueSeverity.ERROR)
                .setCode(OperationOutcome.IssueType.EXCEPTION)
                .setDetails(new CodeableConcept().setText(details))
                .setLocation(patientLocation);
        return outcome;
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
