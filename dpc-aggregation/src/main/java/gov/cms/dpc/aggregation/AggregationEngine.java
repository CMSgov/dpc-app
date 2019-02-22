package gov.cms.dpc.aggregation;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import gov.cms.dpc.common.interfaces.AttributionEngine;
import gov.cms.dpc.aggregation.bbclient.BlueButtonClient;
import gov.cms.dpc.common.models.JobModel;
import gov.cms.dpc.queue.JobQueue;
import gov.cms.dpc.queue.JobStatus;
import gov.cms.dpc.queue.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

public class AggregationEngine<T> implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(AggregationEngine.class);

    private final AttributionEngine engine;
    private final JobQueue queue;
    private final BlueButtonClient bbclient;
    private final FhirContext context;
    private volatile boolean run = true;

    @Inject
    public AggregationEngine(AttributionEngine engine, JobQueue queue, BlueButtonClient bbclient) {
        this.engine = engine;
        this.queue = queue;
        this.bbclient = bbclient;
        this.context = FhirContext.forDstu3();
    }

    @Override
    public void run() {

        while (run) {
            final Optional<Pair<UUID, Object>> workPair = this.queue.workJob();
            if (workPair.isEmpty()) {
                try {
                    logger.debug("No job, waiting 2 seconds");
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                final JobModel model = (JobModel) workPair.get().getRight();
                final UUID jobID = workPair.get().getLeft();
                logger.debug("Has job {}. Working.", jobID);
                final Optional<Set<String>> attributedBeneficiaries = this.engine.getAttributedBeneficiaries(model.getProviderID());
                if (attributedBeneficiaries.isPresent()) {
                    logger.debug("Has {} attributed beneficiaries", attributedBeneficiaries.get().size());
                    try {
                        this.workJob(jobID, model);
                        this.queue.completeJob(jobID, JobStatus.COMPLETED);
                    } catch (Exception e) {
                        logger.error("Cannot process job {}", jobID, e);
                        this.queue.completeJob(jobID, JobStatus.FAILED);
                    }
                } else {
                    logger.error("Cannot execute Job {} with no beneficiaries", jobID);
                    this.queue.completeJob(jobID, JobStatus.FAILED);
                }
            }
        }
        logger.info("Shutting down aggregation engine");
    }

    public void stop() {
        this.run = false;
    }

    private void workJob(UUID jobID, JobModel job) throws IOException {
        final IParser parser = context.newJsonParser();
        final File tempFile = File.createTempFile(jobID.toString(), ".ndjson", new File("/tmp"));
        logger.debug("Writing results to {}", tempFile.getAbsolutePath());
        try (final FileWriter fileWriter = new FileWriter(tempFile)) {
            try (final BufferedWriter bufferedWriter = new BufferedWriter(fileWriter)) {
                job.getBeneficiaries()
                        .stream()
                        .map(this.bbclient::requestFHIRFromServer)
                        .map(parser::encodeResourceToString)
                        .forEach(str -> {
                            try {
                                logger.debug("Writing {} to file", str);
                                bufferedWriter.write(str);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });
                bufferedWriter.flush();
            }
        }
    }
}
