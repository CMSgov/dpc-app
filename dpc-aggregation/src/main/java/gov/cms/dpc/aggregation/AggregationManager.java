package gov.cms.dpc.aggregation;

import gov.cms.dpc.aggregation.engine.AggregationEngine;
import gov.cms.dpc.aggregation.engine.CurrentEngineState;
import io.dropwizard.lifecycle.Managed;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class AggregationManager implements Managed {

    private static final Logger logger = LoggerFactory.getLogger(AggregationManager.class);

    private final Thread thread;
    private final AggregationEngine engine;
    private final CurrentEngineState engineState;

    @Inject
    public AggregationManager(AggregationEngine engine, CurrentEngineState engineState) {
        logger.info("Instantiating Aggregation Manager");
        this.engineState = engineState;
        this.engine = engine;
        thread = new Thread(this.engine);
    }

    @Override
    public void start() {
        logger.debug("Starting Aggregation thread");
        thread.start();
    }

    @Override
    public void stop() {
        logger.debug("Stopping Aggregation thread");
        System.out.println("Current state: " + engineState.getState());
        // Wait for stopped status
        this.engine.stop();
    }
}
