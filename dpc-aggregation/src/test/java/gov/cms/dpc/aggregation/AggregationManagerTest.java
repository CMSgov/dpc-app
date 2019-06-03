package gov.cms.dpc.aggregation;

import gov.cms.dpc.aggregation.engine.AggregationEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AggregationManagerTest {

    private AggregationEngine engine;

    @BeforeEach
    void setup() {
        engine = mock(AggregationEngine.class);
        Mockito.reset(engine);
    }

    @Test
<<<<<<< HEAD
    void testShutdown() {
=======
    public void testStartup() {
        new AggregationManager(engine).start();
        // Should not have interacted with the engine.
        //verifyZeroInteractions(engine);
    }

    @Test
    public void testShutdown() {
        final AggregationEngine engine = mock(AggregationEngine.class);
>>>>>>> Made requestCoverageFromServer return an Observable
        new AggregationManager(engine).stop();
        verify(engine).stop();
    }
}
