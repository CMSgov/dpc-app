package gov.cms.dpc.aggregation;

import gov.cms.dpc.aggregation.engine.AggregationEngine;
import gov.cms.dpc.aggregation.engine.CurrentEngineState;
import gov.cms.dpc.testing.BufferedLoggerHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(BufferedLoggerHandler.class)
class AggregationManagerTest {

    private AggregationEngine engine;
    private CurrentEngineState state;

    @BeforeEach
    void setup() {
        engine = mock(AggregationEngine.class);
        Mockito.reset(engine);
    }

    @Test
    void testShutdown() {
        new AggregationManager(engine, state).stop();
        verify(engine).stop();
    }
}
