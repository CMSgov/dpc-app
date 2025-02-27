package gov.cms.dpc.aggregation.health;

import gov.cms.dpc.aggregation.engine.AggregationEngine;
import ru.vyarus.dropwizard.guice.module.installer.feature.health.NamedHealthCheck;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class AggregationEngineHealthCheck extends NamedHealthCheck {

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

    @Override
    public String getName() {
        return "aggregation-engine";
    }
}
