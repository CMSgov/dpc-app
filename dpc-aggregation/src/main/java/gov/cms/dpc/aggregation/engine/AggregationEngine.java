package gov.cms.dpc.aggregation.engine;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.typesafe.config.Config;
import gov.cms.dpc.aggregation.bbclient.BlueButtonClient;
import gov.cms.dpc.common.models.JobModel;
import gov.cms.dpc.queue.JobQueue;
import gov.cms.dpc.queue.JobStatus;
import gov.cms.dpc.queue.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class AggregationEngine implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(AggregationEngine.class);
    private static final char delim = '\n';

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
                Set<String> attributedBeneficiaries = model.getBeneficiaries();

                if (!attributedBeneficiaries.isEmpty()) {
                    logger.debug("Has {} attributed beneficiaries",attributedBeneficiaries.size());
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
        try (final FileOutputStream writer = new FileOutputStream(String.format("%s/%s.ndjson", exportPath, jobID.toString()))) {
            job.getBeneficiaries()
                    .stream()
                    .map(this.bbclient::requestPatientFromServer)
                    .map(parser::encodeResourceToString)
                    .forEach(str -> {
                        try {
                            logger.debug("Writing {} to file", str);
                            writer.write(str.getBytes(StandardCharsets.UTF_8));
                            writer.write(delim);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
            writer.flush();
        }
    }
}
