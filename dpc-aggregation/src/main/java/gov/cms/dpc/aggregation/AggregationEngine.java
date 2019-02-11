package gov.cms.dpc.aggregation;

import gov.cms.dpc.attribution.AttributionEngine;
import gov.cms.dpc.queue.JobQueue;
import gov.cms.dpc.queue.JobStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class AggregationEngine implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(AggregationEngine.class);

    private final AttributionEngine engine;
    private final JobQueue queue;
    private volatile boolean run = true;

    @Inject
    public AggregationEngine(AttributionEngine engine, JobQueue queue) {
        this.engine = engine;
        this.queue = queue;
    }

    @Override
    public void run() {

        while(run) {
            final Optional<UUID> uuid = this.queue.workJob();
            if (uuid.isEmpty()) {
                try {
                    logger.debug("No job, waiting 2 seconds");
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                logger.debug("Has job {}. Working.", uuid.get());
                final Set<String> attributedBeneficiaries = this.engine.getAttributedBeneficiaries(uuid.toString());
                logger.debug("Has {} attributed beneficiaries", attributedBeneficiaries.size());
                // Job is done
                this.queue.completeJob(uuid.get(), JobStatus.COMPLETED);
            }
        }
        logger.info("Shutting down aggregation engine");
    }

    public void stop() {
        this.run = false;
    }
}
