package gov.cms.dpc.aggregation.health;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import gov.cms.dpc.aggregation.engine.AggregationEngine;
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
