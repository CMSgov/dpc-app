package gov.cms.dpc.aggregation.engine;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.typesafe.config.Config;
import gov.cms.dpc.aggregation.bbclient.BlueButtonClient;
import gov.cms.dpc.queue.JobQueue;
import gov.cms.dpc.queue.JobStatus;
import gov.cms.dpc.queue.Pair;
import gov.cms.dpc.queue.models.JobModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class AggregationEngine implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(AggregationEngine.class);
    private static final char DELIM = '\n';

    private final JobQueue queue;
    private final BlueButtonClient bbclient;
    private final FhirContext context;
    private final String exportPath;
    private volatile boolean run = true;

    @Inject
    public AggregationEngine(BlueButtonClient bbclient, JobQueue queue, Config config) {
        this.queue = queue;
        this.bbclient = bbclient;
        this.context = FhirContext.forDstu3();
        this.exportPath = config.getString("exportPath");
    }

    @Override
    public void run() {

        while (run) {
            final Optional<Pair<UUID, JobModel>> workPair = this.queue.workJob();
            if (workPair.isEmpty()) {
                try {
                    logger.debug("No job, waiting 2 seconds");
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    logger.error("Interrupted. {}", e.getMessage());
                    Thread.currentThread().interrupt();
                }
            } else {
                final JobModel model = workPair.get().getRight();
                final UUID jobID = workPair.get().getLeft();
                logger.info("Processing job {}, exporting to: {}.", jobID, this.exportPath);
                List<String> attributedBeneficiaries = model.getPatients();

                if (!attributedBeneficiaries.isEmpty()) {
                    logger.debug("Has {} attributed beneficiaries", attributedBeneficiaries.size());
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
        try (final FileOutputStream writer = new FileOutputStream(String.format("%s/%s.ndjson", exportPath, jobID.toString()))) {
            workResource(writer, job);
            writer.flush();
        }
    }

    private void workResource(FileOutputStream writer, JobModel job) {
        final IParser parser = context.newJsonParser();
        job.getPatients()
                .stream()
                .map(this.bbclient::requestPatientFromServer)
                .map(parser::encodeResourceToString)
                .forEach(str -> {
                    try {
                        logger.debug("Writing {} to file", str);
                        writer.write(str.getBytes(StandardCharsets.UTF_8));
                        writer.write(DELIM);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
    }
}
