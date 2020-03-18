package gov.cms.dpc.aggregation.health;

import com.codahale.metrics.health.HealthCheck;
import gov.cms.dpc.aggregation.engine.AggregationEngine;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class AggregationEngineHealthCheck extends HealthCheck {

    private AggregationEngine aggregationEngine;

    @Inject
    public AggregationEngineHealthCheck(AggregationEngine aggregationEngine) {
        this.aggregationEngine = aggregationEngine;
    }

    @Override
    public Result check() {
        Result result = Result.healthy();
        if (!aggregationEngine.isRunning()) {
            result = Result.unhealthy("Aggregation Engine instance: " + aggregationEngine.getAggregatorID() + " in error state");
        }
        return result;
    }
}
