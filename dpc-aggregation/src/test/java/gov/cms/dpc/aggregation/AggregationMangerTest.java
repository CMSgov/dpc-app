package gov.cms.dpc.aggregation;

import gov.cms.dpc.aggregation.engine.AggregationEngine;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

public class AggregationMangerTest {

    @Test
    public void testStartup() {
        final AggregationEngine engine = mock(AggregationEngine.class);
        new AggregationManager(engine).start();
        // Should not have interacted with the engine.
        verifyZeroInteractions(engine);
    }

    @Test
    public void testShutdown() {
        final AggregationEngine engine = mock(AggregationEngine.class);
        new AggregationManager(engine).stop();

        verify(engine).stop();
    }
}
