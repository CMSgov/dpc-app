package gov.cms.dpc.aggregation.engine;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import gov.cms.dpc.aggregation.bbclient.BlueButtonClient;
import gov.cms.dpc.common.annotations.ExportPath;
import gov.cms.dpc.queue.JobQueue;
import gov.cms.dpc.queue.JobStatus;
import gov.cms.dpc.queue.Pair;
import gov.cms.dpc.queue.exceptions.JobQueueFailure;
import gov.cms.dpc.queue.models.JobModel;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.transformer.RetryTransformer;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.dstu3.model.ResourceType;
import gov.cms.dpc.queue.models.JobResult;
import org.hl7.fhir.dstu3.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class AggregationEngine implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(AggregationEngine.class);
    private static final char DELIM = '\n';

    final String exportPath;
    private final JobQueue queue;
    private final BlueButtonClient bbclient;
    private final FhirContext context;
    private final RetryConfig retryConfig;
    private final IParser jsonParser;
    private Disposable subscribe;

    /**
     * Create an engine
     *
     * @param bbclient    - the BlueButton client to use
     * @param queue       - the Job queue that will direct the work done
     * @param exportPath  - The {@link ExportPath} to use for writing the output files
     * @param retryConfig - {@link RetryConfig} injected config for setting up retry handler
     */
    @Inject
    public AggregationEngine(BlueButtonClient bbclient, JobQueue queue, @ExportPath String exportPath, RetryConfig retryConfig) {
        this.queue = queue;
        this.bbclient = bbclient;
        this.context = FhirContext.forDstu3();
        this.exportPath = exportPath;
        this.retryConfig = retryConfig;
        this.jsonParser = this.context.newJsonParser();
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

    /**
     * Form the full file name of an output file
     *
     * @param jobID
     * @param resourceType
     */
    public String formErrorFilePath(UUID jobID, ResourceType resourceType) {
        return String.format("%s/%s.ndjson", exportPath, JobModel.formErrorFileName(jobID, resourceType));
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

        final Disposable iterableSubscriber = Observable.fromIterable(job.getJobResults())
                .subscribe(jobResult -> completeResource(job, jobResult),
                        error -> {
                            logger.error("Cannot process job {}", jobID, error);
                            this.queue.completeJob(jobID, JobStatus.FAILED, job.getJobResults());
                        },
                        () -> this.queue.completeJob(jobID, JobStatus.COMPLETED, job.getJobResults()));

        // Kill the subscriber when we exit, otherwise we'll leak resources
        iterableSubscriber.dispose();
    }

    /**
     * Handle the file aspects of a resource
     *
     * @param job - Job that is executing
     * @param jobResult - The results for a current resource
     * @throws IOException - File operation execeptions
     */
    protected void completeResource(JobModel job, JobResult jobResult) throws IOException {
        final var resourceType = jobResult.getResourceType();
        final var jobID = jobResult.getJobID();

        if (!JobModel.isValidResourceType(resourceType)) {
            throw new JobQueueFailure(jobID, "Unexpected resource type: " + resourceType.toString());
        }

        try (final var writer = new ByteArrayOutputStream(); final var errorWriter = new ByteArrayOutputStream()) {
            // Process the job for the specified resource type
            workResource(writer, errorWriter, job, jobResult);
            writeToFile(writer.toByteArray(), formOutputFilePath(jobID, resourceType));
            writeToFile(errorWriter.toByteArray(), formErrorFilePath(jobID, resourceType));
        }
    }

    /**
     * Process a single resourceType. Write a single provider NDJSON file as well as operational errors.
     *
     * @param writer - the stream to write results
     * @param errorWriter - the stream to write operational resources
     * @param job - the job to process
     * @param jobResult - the result of the work on the resource type.
     */
    protected void workResource(OutputStream writer, OutputStream errorWriter, JobModel job, JobResult jobResult) {
        Observable.fromIterable(job.getPatients())
                .flatMap(patient -> this.fetchResource(job.getJobID(), patient, jobResult.getResourceType()))
                .subscribeOn(Schedulers.io())
                .blockingSubscribe(resource -> writeResource(jobResult, writer, errorWriter, resource));
    }

    /**
     * Write the resource into the appropriate streams.
     *
     * @param jobResult - increment counts in this result
     * @param mainWriter - the main stream for successful resources
     * @param errorWriter - the error stream for operational outcome resources
     * @param resource - the resource to write out
     */
    protected void writeResource(JobResult jobResult, OutputStream mainWriter, OutputStream errorWriter, Resource resource) {
        try {
            String description;
            OutputStream writer;
            if (ResourceType.OperationOutcome.equals(resource.getResourceType())) {
                description = "Writing {} to error file";
                writer = errorWriter;
                jobResult.incrementErrorCount();
            } else {
                description = "Writing {} to file";
                writer = mainWriter;
                jobResult.incrementCount();
            }

            final String str = jsonParser.encodeResourceToString(resource);
            logger.trace(description, str);
            writer.write(str.getBytes(StandardCharsets.UTF_8));
            writer.write(DELIM);
        } catch (IOException e) {
            throw new JobQueueFailure(jobResult.getJobID(), e);
        }
    }

    /**
     * Fetches the given resource from the {@link BlueButtonClient} and converts it from FHIR-JSON to Resource. The
     * resource may be a type requested or it may be an operational outcome;
     *
     * @param jobID - {@link UUID} jobID
     * @param patientID   - {@link String} patient ID
     * @param resourceType - {@link ResourceType} to fetch from BlueButton
     * @return - {@link Observable} of {@link Resource} to pass back to reactive loop.
     */
    private Observable<Resource> fetchResource(UUID jobID, String patientID, ResourceType resourceType) {
        Retry retry = Retry.of("bb-resource-fetcher", this.retryConfig);
        RetryTransformer<Resource> retryTransformer = RetryTransformer.of(retry);

        return Observable.fromCallable(() -> {
            logger.debug("Fetching patient {} from Blue Button", patientID);
            switch (resourceType) {
                case Patient:
                    return this.bbclient.requestPatientFromServer(patientID);
                case ExplanationOfBenefit:
                    return this.bbclient.requestEOBBundleFromServer(patientID);
                case Coverage:
                    return this.bbclient.requestCoverageFromServer(patientID);
                default:
                    throw new JobQueueFailure(jobID, "Unexpected resource type: " + resourceType.toString());
            }
        })
        // Turn errors into retries
        .compose(retryTransformer)
        // Turn errors into OperationalOutcomes
        .onErrorReturn(ex -> {
            logger.error("Error fetching from Blue Button", ex);
            return formOperationOutcome(patientID, ex);
        });
    }

    /**
     * Create a OperationalOutcome resource from an exception
     *
     * @param patientID - the id of the patient involved in the error
     * @param ex - the exception to turn into a Operational Outcome
     * @return an operation outcome
     */
    private OperationOutcome formOperationOutcome(String patientID, Throwable ex) {
        String details;
        if (ex instanceof ResourceNotFoundException) {
            details = "Patient not found in Blue Button";
        } else if (ex instanceof BaseServerResponseException) {
            final var serverException = (BaseServerResponseException)ex;
            details = String.format("Blue Button error: HTTP status: %s", serverException.getStatusCode());
        } else {
            details = String.format("Internal error: %s", ex.getMessage());
        }

        final var location = List.of(new StringType("Patient"), new StringType("id"), new StringType(patientID));
        final var outcome = new OperationOutcome();
        outcome.addIssue()
                .setSeverity(OperationOutcome.IssueSeverity.ERROR)
                .setCode(OperationOutcome.IssueType.EXCEPTION)
                .setDetails(new CodeableConcept().setText(details))
                .setLocation(location);
        return outcome;
    }

    /**
     * Write a array of bytes to a file. Name the file according to the supplied name
     *
     * @param bytes - Bytes to write
     * @param fileName - The fileName to write too
     * @throws IOException
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
