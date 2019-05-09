package gov.cms.dpc.aggregation.engine;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import com.typesafe.config.Config;
import gov.cms.dpc.aggregation.bbclient.BlueButtonClient;
import gov.cms.dpc.common.annotations.ExportPath;
import gov.cms.dpc.queue.JobQueue;
import gov.cms.dpc.queue.JobStatus;
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
import java.security.interfaces.RSAPrivateKey;

public class AggregationEngine implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(AggregationEngine.class);
    private static final char DELIM = '\n';

    final String exportPath;
    private final JobQueue queue;
    private final BlueButtonClient bbclient;
    private final FhirContext context;
    private final RetryConfig retryConfig;
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
        return String.format("%s/%s.ndjson", exportPath, JobModel.outputFileName(jobID, resourceType));
    }

    /**
     * Form the full file name of an output file
     *
     * @param jobID
     * @param resourceType
     */
    public String formErrorFilePath(UUID jobID, ResourceType resourceType) {
        return String.format("%s/%s.ndjson", exportPath, JobModel.errorFileName(jobID, resourceType));
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

        // Guard against an empty bene list
        if (attributedBeneficiaries.isEmpty()) {
            logger.error("Cannot execute Job {} with no beneficiaries", jobID);
            this.queue.completeJob(jobID, JobStatus.FAILED, job.getJobResults());
            return;
        }

        logger.debug("Has {} attributed beneficiaries", attributedBeneficiaries.size());
        try {
            for (JobResult jobResult: job.getJobResults()) {
                final var resourceType = jobResult.getResourceType();
                if (!JobModel.isValidResourceType(resourceType)) {
                    throw new JobQueueFailure(job.getJobID(), "Unexpected resource type: " + resourceType.toString());
                }

                try (final var writer = new FileOutputStream(formOutputFilePath(job.getJobID(), resourceType));
                    final var errorWriter = new ByteArrayOutputStream()) {

                    // Process the job for the specified resource type
                    workResource(writer, errorWriter, job, jobResult);
                    writer.flush();

                    // Write our errors if present
                    if (jobResult.getErrorCount() > 0) {
                        try (final var errorFile = new FileOutputStream(formErrorFilePath(job.getJobID(), resourceType))) {
                            errorFile.write(errorWriter.toByteArray());
                            errorFile.flush();
                        }
                    }
                }
            }
            this.queue.completeJob(jobID, JobStatus.COMPLETED, job.getJobResults());
        } catch (Exception e) {
            logger.error("Cannot process job {}", jobID, e);
            this.queue.completeJob(jobID, JobStatus.FAILED, job.getJobResults());
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
        final IParser parser = context.newJsonParser();

        Observable.fromIterable(job.getPatients())
                .flatMap(patient -> this.fetchResource(patient, resourceType, parser))
                .subscribeOn(Schedulers.io())
                // If an error gets signaled, log it and send an empty observable, signalling that we should continue processing the next patient
                .doOnError(e -> logger.error("Error: ", e))
                .onErrorResumeNext(Observable.empty())
                .blockingSubscribe(str -> {
                    try {
                        logger.trace("Writing {}.", str);
                        writer.write(str.getBytes(StandardCharsets.UTF_8));
                        writer.write(DELIM);
                    } catch (IOException e) {
                        throw new JobQueueFailure(job.getJobID(), e);
                    }
                });
        final var resourceType = jobResult.getResourceType();
        job.getPatients()
                .stream()
                .map(patientId -> requestResource(job, resourceType, patientId))
                .forEach(resource -> writeResource(job, jobResult, writer, errorWriter, parser, resource));
    }

    /**
     * Request a resource from Blue Button
     *
     * @param job - The context for this request
     * @param resourceType - The type of resource to request
     * @param patientId - The patient
     * @return the resource requested or a operation outcome when an error occurs
     */
    protected Resource requestResource(JobModel job, ResourceType resourceType, String patientId) {
        try {
            switch (resourceType) {
                case Patient:
                    return this.bbclient.requestPatientFromServer(patientId);
                case ExplanationOfBenefit:
                    return this.bbclient.requestEOBBundleFromServer(patientId);
                default:
                    throw new JobQueueFailure(job.getJobID(), "Unexpected resource type: " + resourceType.toString());
            }
        } catch (ResourceNotFoundException ex) {
            final var details = "Patient not found in Blue Button";
            return formOperationOutcome(patientId, details);
        } catch (BaseServerResponseException ex) {
            final var details = String.format("Blue Button error: HTTP status: %s", ex.getStatusCode());
            return formOperationOutcome(patientId, details);
        }
    }


    /**
     * Write the resource into the appropriate streams.
     *
     * @param job - the context for this work
     * @param jobResult - the resource type that is being written
     * @param mainWriter - the main stream for successful resources
     * @param errorWriter - the error stream for operational outcomes
     * @param parser - the serializer to use should be Json
     * @param resource - the resource to write out
     */
    protected void writeResource(JobModel job, JobResult jobResult, OutputStream mainWriter, OutputStream errorWriter, IParser parser, Resource resource) {
        try {
            final String str = parser.encodeResourceToString(resource);
            if (resource.getResourceType() == ResourceType.OperationOutcome) {
                logger.debug("Writing {} to error file", str);
                errorWriter.write(str.getBytes(StandardCharsets.UTF_8));
                errorWriter.write(DELIM);
                jobResult.incrementErrorCount();
            } else {
                logger.debug("Writing {} to file", str);
                mainWriter.write(str.getBytes(StandardCharsets.UTF_8));
                mainWriter.write(DELIM);
                jobResult.incrementCount();
            }
        } catch (IOException e) {
            throw new JobQueueFailure(job.getJobID(), e);
        }
    }

    /**
     * Create a OperationId which is an used to create an XPath to resource
     *
     * @param patientID - the patient id that
     * @param details - The details to put into the outcome
     * @return an operation outcome
     */
    protected OperationOutcome formOperationOutcome(String patientID, String details) {
        final var location = List.of(new StringType("Patient"), new StringType("id"), new StringType(patientID));
        final var outcome = new OperationOutcome();
        outcome.addIssue()
                .setSeverity(OperationOutcome.IssueSeverity.ERROR)
                .setCode(OperationOutcome.IssueType.EXCEPTION)
                .setDetails(new CodeableConcept().setText(details))
                .setLocation(location);
        return outcome;
    }

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
                .subscribe(this::workExportJob);
    }

    /**
     * Wrapper method for dispatching the job and handling any errors that would cause the job to fail
     *
     * @param workPair - job {@link Pair} with {@link UUID} or {@link JobModel}
     */
    private void workExportJob(Pair<UUID, JobModel> workPair) {
        final JobModel model = workPair.getRight();
        final UUID jobID = workPair.getLeft();
        logger.debug("Has job {}. Working.", jobID);
        List<String> attributedBeneficiaries = model.getPatients();

        if (!attributedBeneficiaries.isEmpty()) {
            logger.debug("Has {} attributed beneficiaries", attributedBeneficiaries.size());
            try {
                this.completeJob(model);
            } catch (Exception e) {
                logger.error("Cannot process job {}", jobID, e);
                this.queue.completeJob(jobID, JobStatus.FAILED);
            }
        } else {
            logger.error("Cannot execute Job {} with no beneficiaries", jobID);
            this.queue.completeJob(jobID, JobStatus.FAILED);
        }
    }

    /**
     * Fetches the given resource from the {@link BlueButtonClient} and converts it from FHIR-JSON to a String
     *
     * @param identifier   - {@link String} patient ID
     * @param resourceType - {@link ResourceType} to fetch from BlueButton
     * @param parser       - {@link IParser} FHIR parser to use for JSON conversion
     * @return - {@link Observable} of {@link String} to pass back to reactive loop
     */
    private Observable<String> fetchResource(String identifier, ResourceType resourceType, IParser parser) {
        Retry retry = Retry.of("bb-resource-fetcher", this.retryConfig);
        RetryTransformer<Resource> retryTransformer = RetryTransformer.of(retry);

        return Observable.fromCallable(() -> {
            switch (resourceType) {
                case Patient:
                    return this.bbclient.requestPatientFromServer(identifier);
                case ExplanationOfBenefit:
                    return this.bbclient.requestEOBBundleFromServer(identifier);
                default:
                    throw new IllegalArgumentException("Unexpected resource type: " + resourceType.toString());
            }
        })
                .compose(retryTransformer)
                .doOnNext(p -> logger.debug("Fetching {}", p))
                .doOnError(e -> logger.error("Error fetching from BB.", e))
                .map(parser::encodeResourceToString);
    }
}
