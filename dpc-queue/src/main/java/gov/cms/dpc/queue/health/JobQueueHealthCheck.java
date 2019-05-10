package gov.cms.dpc.queue.health;

import com.codahale.metrics.health.HealthCheck;
import gov.cms.dpc.queue.JobQueue;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Simple check for validating at the {@link JobQueue} is healthy
 */
@Singleton
public class JobQueueHealthCheck extends HealthCheck {

    private final JobQueue queue;

    @Inject
    public JobQueueHealthCheck(JobQueue queue) {
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
