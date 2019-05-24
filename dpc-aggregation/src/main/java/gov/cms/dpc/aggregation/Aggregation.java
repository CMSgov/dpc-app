//package gov.cms.dpc.aggregation;
//
//import gov.cms.dpc.aggregation.engine.AggregationEngine;
//import io.dropwizard.lifecycle.Managed;
//
//import javax.inject.Inject;
//
//public class Aggregation implements Managed {
//
//    private final Thread thread;
//    private final AggregationEngine engine;
//
//    @Inject
//    public Aggregation(AggregationEngine engine) {
//        this.engine = engine;
//        thread = new Thread(this.engine);
//    }
//
//
//    @Override
//    public void start() throws Exception {
//        thread.start();
//    }
//
//    @Override
//    public void stop() throws Exception {
//        this.engine.stop();
//    }
//}