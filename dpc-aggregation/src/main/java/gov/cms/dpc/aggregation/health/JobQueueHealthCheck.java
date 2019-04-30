package gov.cms.dpc.aggregation.health;

import com.codahale.metrics.health.HealthCheck;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class JobQueueHealthCheck extends HealthCheck {

    @Inject
    public JobQueueHealthCheck() {
    }

    @Override
    protected Result check() throws Exception {
        return Result.unhealthy("Nope, no good");
    }
}
