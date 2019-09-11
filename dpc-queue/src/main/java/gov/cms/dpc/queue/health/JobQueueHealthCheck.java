package gov.cms.dpc.queue.health;

import com.codahale.metrics.health.HealthCheck;
import gov.cms.dpc.queue.JobQueueInterface;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Simple check for validating at the {@link JobQueueInterface} is healthy
 */
@Singleton
public class JobQueueHealthCheck extends HealthCheck {

    private final JobQueueInterface queue;

    @Inject
    public JobQueueHealthCheck(JobQueueInterface queue) {
        this.queue = queue;
    }

    @Override
    public Result check() {
        try {
            this.queue.assertHealthy();
            return Result.healthy();
        } catch (Exception e) {
            return Result.unhealthy(e.getMessage());
        }
    }
}
