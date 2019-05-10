package gov.cms.dpc.aggregation.engine;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import com.typesafe.config.Config;
import gov.cms.dpc.aggregation.bbclient.BlueButtonClient;
import gov.cms.dpc.queue.JobQueue;
import gov.cms.dpc.queue.JobStatus;
import gov.cms.dpc.queue.exceptions.JobQueueFailure;
import gov.cms.dpc.queue.models.JobModel;
import gov.cms.dpc.queue.models.JobResult;
import org.hl7.fhir.dstu3.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class AggregationEngine implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(AggregationEngine.class);
    private static final char DELIM = '\n';
    private static final Integer WAIT_TIME = 2000;

    private final JobQueue queue;
    private final BlueButtonClient bbclient;
    private final FhirContext context;
    protected final String exportPath;
    private volatile boolean run = true;

    /**
     * Create an engine
     * @param bbclient - the BlueButton client to use
     * @param queue - the Job queue that will direct the work done
     * @param config - the configuration for the engine
     */
    @Inject
    public AggregationEngine(BlueButtonClient bbclient, JobQueue queue, Config config) {
        this.queue = queue;
        this.bbclient = bbclient;
        this.context = FhirContext.forDstu3();
        this.exportPath = config.getString("exportPath");
    }

    /**
     * Run the engine. Part of the Runnable interface.
     */
    @Override
    public void run() {
        // Run loop
        logger.info("Starting aggregation engine with exportPath:\"{}\"", exportPath);
        while (run) {
            this.queue.workJob().ifPresentOrElse(pair -> {
                completeJob(pair.getRight());
            }, () -> {
                try {
                    logger.debug("No job, waiting {} milliseconds", WAIT_TIME);
                    Thread.sleep(WAIT_TIME);
                } catch (InterruptedException e) {
                    logger.error("Interrupted. {}", e.getMessage());
                    Thread.currentThread().interrupt();
                }
            });
        }
        logger.info("Shutting down aggregation engine");
    }

    /**
     * Stop the engine
     */
    public void stop() {
        this.run = false;
    }

    /**
     * Form the full file name of an output file
     *
     * @param jobID
     * @param resourceType
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
            if (ResourceType.OperationOutcome.equals(resource.getResourceType())) {
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
}
