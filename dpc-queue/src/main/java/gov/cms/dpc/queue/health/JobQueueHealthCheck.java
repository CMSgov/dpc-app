package gov.cms.dpc.queue.health;

import com.codahale.metrics.health.HealthCheck;
import gov.cms.dpc.queue.JobQueueInterface;
import gov.cms.dpc.queue.annotations.AggregatorID;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.UUID;

/**
 * Simple check for validating at the {@link JobQueueInterface} is healthy
 */
@Singleton
public class JobQueueHealthCheck extends HealthCheck {

    private final JobQueueInterface queue;

    private final UUID aggregatorID;

    @Inject
    public JobQueueHealthCheck(JobQueueInterface queue, @AggregatorID UUID aggregatorID) {
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
