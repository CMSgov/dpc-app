package gov.cms.dpc.api.health;

import com.codahale.metrics.health.HealthCheck;
import gov.cms.dpc.common.interfaces.AttributionEngine;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Simple check for validating at the {@link gov.cms.dpc.common.interfaces.AttributionEngine} is healthy
 */
@Singleton
public class AttributionHealthCheck extends HealthCheck {

    private final AttributionEngine engine;

    @Inject
    public AttributionHealthCheck(AttributionEngine engine) {
        this.engine = engine;
    }

    @Override
    protected Result check() throws Exception {
        if (engine.isHealthy()) {
            return Result.healthy();
        }

        return Result.healthy("Cannot reach Attribution Service");
    }
}
