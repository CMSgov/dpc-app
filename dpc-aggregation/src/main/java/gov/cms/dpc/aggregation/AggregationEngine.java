package gov.cms.dpc.aggregation;

import gov.cms.dpc.common.interfaces.AttributionEngine;
import gov.cms.dpc.common.models.JobModel;
import gov.cms.dpc.queue.JobQueue;
import gov.cms.dpc.queue.JobStatus;
import gov.cms.dpc.queue.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class AggregationEngine<T> implements Runnable {

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

        while (run) {
            final Optional<Pair<UUID, Object>> uuid = this.queue.workJob();
            if (uuid.isEmpty()) {
                try {
                    logger.debug("No job, waiting 2 seconds");
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                final JobModel model = (JobModel) uuid.get().getRight();
                logger.debug("Has job {}. Working.", uuid.get().getLeft());
                final Optional<Set<String>> attributedBeneficiaries = this.engine.getAttributedBeneficiaries(model.getProviderID());
                if (attributedBeneficiaries.isPresent()) {
                    logger.debug("Has {} attributed beneficiaries", attributedBeneficiaries.get().size());
                    // Job is done
                    this.queue.completeJob(uuid.get().getLeft(), JobStatus.COMPLETED);
                } else {
                    this.queue.completeJob(uuid.get().getLeft(), JobStatus.FAILED);
                }


            }
        }
        logger.info("Shutting down aggregation engine");
    }

    public void stop() {
        this.run = false;
    }
}
