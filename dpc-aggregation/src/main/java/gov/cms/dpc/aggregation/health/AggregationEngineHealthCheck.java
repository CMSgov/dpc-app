package gov.cms.dpc.aggregation.health;

import gov.cms.dpc.aggregation.engine.AggregationEngine;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import ru.vyarus.dropwizard.guice.module.installer.feature.health.NamedHealthCheck;

@Singleton
public class AggregationEngineHealthCheck extends NamedHealthCheck {

    private final AggregationEngine aggregationEngine;

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

    @Override
    public String getName() {
        return "aggregation-engine";
    }
}
