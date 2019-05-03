package gov.cms.dpc.api.health;

import com.codahale.metrics.health.HealthCheck;
import gov.cms.dpc.api.client.AttributionServiceClient;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Simple check for validating at the {@link gov.cms.dpc.common.interfaces.AttributionEngine} is healthy
 */
@Singleton
public class AttributionHealthCheck extends HealthCheck {

    private final AttributionServiceClient client;

    @Inject
    public AttributionHealthCheck(AttributionServiceClient client) {
        this.client = client;
    }

    @Override
    protected Result check() throws Exception {
        if (client.isHealthy()) {
            return Result.healthy();
        }

        return Result.healthy("Cannot reach Attribution Service");
    }
}
