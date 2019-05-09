package gov.cms.dpc.attribution.health;

import com.codahale.metrics.health.HealthCheck;
import gov.cms.dpc.common.interfaces.AttributionEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class RosterEngineHealthCheck extends HealthCheck {

    private final static Logger logger = LoggerFactory.getLogger(RosterEngineHealthCheck.class);

    private final AttributionEngine engine;

    @Inject
    RosterEngineHealthCheck(AttributionEngine engine) {
        this.engine = engine;
    }

    @Override
    protected Result check() throws Exception {
        try {
            engine.assertHealthy();
            return Result.healthy();
        } catch (Exception e) {
            logger.error("Roster engine failed healthcheck", e);
            return Result.unhealthy(e.getMessage());
        }
    }
}
