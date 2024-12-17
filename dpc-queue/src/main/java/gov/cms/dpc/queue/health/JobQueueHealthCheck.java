package gov.cms.dpc.queue.health;

import com.codahale.metrics.health.HealthCheck;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import gov.cms.dpc.queue.IJobQueue;
import gov.cms.dpc.queue.annotations.AggregatorID;

import java.util.UUID;

/**
 * Simple check for validating at the {@link IJobQueue} is healthy
 */
@Singleton
public class JobQueueHealthCheck extends HealthCheck {

    private final IJobQueue queue;

    private final UUID aggregatorID;

    @Inject
    public JobQueueHealthCheck(IJobQueue queue, @AggregatorID UUID aggregatorID) {
        this.queue = queue;
        this.aggregatorID = aggregatorID;
    }

    @Override
    public Result check() {
        try {
            this.queue.assertHealthy(aggregatorID);
            return Result.healthy();
        } catch (Exception e) {
            return Result.unhealthy(e.getMessage());
        }
    }
}
