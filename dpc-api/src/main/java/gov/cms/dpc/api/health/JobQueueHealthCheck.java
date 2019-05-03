package gov.cms.dpc.api.health;

import com.codahale.metrics.health.HealthCheck;
import gov.cms.dpc.queue.JobQueue;

import javax.inject.Inject;

public class JobQueueHealthCheck extends HealthCheck {
    private final JobQueue queue;

    @Inject
    public JobQueueHealthCheck(JobQueue queue) {
        this.queue = queue;
    }

    @Override
    protected Result check() throws Exception {
        this.queue.isHealthy();
        return Result.healthy();
    }
}
