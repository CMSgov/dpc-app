package gov.cms.dpc.attribution.health;

import com.codahale.metrics.health.HealthCheck;
import gov.cms.dpc.common.interfaces.AttributionEngine;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class RosterEngineHealthCheck extends HealthCheck {

    private final AttributionEngine engine;

    @Inject
    RosterEngineHealthCheck(AttributionEngine engine) {
        this.engine = engine;
    }

    @Override
    protected Result check() {
        try {
            engine.assertHealthy();
            return Result.healthy();
        } catch (Exception e) {
            return Result.unhealthy(e.getMessage());
        }
    }
}
