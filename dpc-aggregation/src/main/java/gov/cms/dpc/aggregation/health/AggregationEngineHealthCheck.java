package gov.cms.dpc.aggregation.health;

import com.codahale.metrics.health.HealthCheck;
import gov.cms.dpc.aggregation.engine.AggregationEngine;
import gov.cms.dpc.queue.annotations.AggregatorID;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.UUID;

@Singleton
public class AggregationEngineHealthCheck extends HealthCheck {

    private AggregationEngine aggregationEngine;
    private UUID aggregatorID;

    @Inject
    public AggregationEngineHealthCheck(@AggregatorID UUID aggregatorID, AggregationEngine aggregationEngine) {
        this.aggregatorID = aggregatorID;
        this.aggregationEngine = aggregationEngine;
    }

    @Override
    public Result check() {
        Result result = Result.healthy();
        if (aggregationEngine.isRunning() && aggregationEngine.inError()) {
            result = Result.unhealthy("Aggregation Engine instance: " + aggregatorID + " in error state");
        }
        return result;
    }
}
