package gov.cms.dpc.aggregation.engine;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.typesafe.config.Config;
import gov.cms.dpc.aggregation.bbclient.BlueButtonClient;
import gov.cms.dpc.queue.JobQueue;
import gov.cms.dpc.queue.JobStatus;
import gov.cms.dpc.queue.Pair;
import gov.cms.dpc.queue.exceptions.JobQueueFailure;
import gov.cms.dpc.queue.models.JobModel;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.inject.Inject;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.interfaces.RSAPrivateKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
        return String.format("%s/%s.error.ndjson", exportPath, JobModel.outputFileName(jobID, resourceType));
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
            this.queue.completeJob(jobID, JobStatus.FAILED, List.of());
            return;
        }

        logger.debug("Has {} attributed beneficiaries", attributedBeneficiaries.size());
        try {
            final var erringTypes = new ArrayList<ResourceType>();
            for (ResourceType resourceType: job.getResourceTypes()) {
                if (!JobModel.isValidResourceType(resourceType)) {
                    throw new JobQueueFailure(job.getJobID(), "Unexpected resource type: " + resourceType.toString());
                }

                try (final var writer = new FileOutputStream(formOutputFilePath(job.getJobID(), resourceType));
                    final var errorWriter = new ByteArrayOutputStream()) {
                    workResource(writer, errorWriter, job, resourceType);
                    writer.flush();

                    // Write our errors if present
                    if (errorWriter.size() > 0) {
                        try (final var errorFile = new FileOutputStream(formErrorFilePath(job.getJobID(), resourceType))) {
                            errorFile.write(errorWriter.toByteArray());
                            errorFile.flush();
                        }
                        erringTypes.add(resourceType);
                    }
                }
            }
            this.queue.completeJob(jobID, JobStatus.COMPLETED, erringTypes);
        } catch (Exception e) {
            logger.error("Cannot process job {}", jobID, e);
            this.queue.completeJob(jobID, JobStatus.FAILED, List.of());
        }
    }


    /**
     * Write out a single provider NDJSON file for a single resource type
     *
     * @param writer - the stream to write to
     * @param job - the job to process
     * @param resourceType - the FHIR resource type to write out
     */
    protected void workResource(OutputStream writer, OutputStream errorWriter, JobModel job, ResourceType resourceType) {
        final IParser parser = context.newJsonParser();

        job.getPatients()
                .stream()
                .map(patientId -> {
                    switch (resourceType) {
                        case Patient:
                            return this.bbclient.requestPatientFromServer(patientId);
                        case ExplanationOfBenefit:
                            return this.bbclient.requestEOBBundleFromServer(patientId);
                        default:
                            throw new JobQueueFailure(job.getJobID(), "Unexpected resource type: " + resourceType.toString());
                    }
                })
                .map(parser::encodeResourceToString)
                .forEach(str -> {
                    try {
                        logger.debug("Writing {} to file", str);
                        writer.write(str.getBytes(StandardCharsets.UTF_8));
                        writer.write(DELIM);
                    } catch (IOException e) {
                        throw new JobQueueFailure(job.getJobID(), e);
                    }
                });
    }
}
