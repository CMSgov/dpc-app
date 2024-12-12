package gov.cms.dpc.aggregation;

import gov.cms.dpc.aggregation.engine.AggregationEngine;
import io.dropwizard.lifecycle.Managed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;

public class AggregationManager implements Managed {

    private static final Logger logger = LoggerFactory.getLogger(AggregationManager.class);

    private final Thread thread;
    private final AggregationEngine engine;

    @Inject
    public AggregationManager(AggregationEngine engine) {
        logger.info("Instantiating Aggregation Manager");
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
        this.engine.stop();
    }
}