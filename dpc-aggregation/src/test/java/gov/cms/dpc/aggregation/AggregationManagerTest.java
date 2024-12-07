package gov.cms.dpc.aggregation;

import gov.cms.dpc.aggregation.engine.AggregationEngine;
import gov.cms.dpc.testing.BufferedLoggerHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(BufferedLoggerHandler.class)
@DisplayName("Aggregation Engine management")
class AggregationManagerTest {

    private AggregationEngine engine;

    @BeforeEach
    void setup() {
        engine = mock(AggregationEngine.class);
        Mockito.reset(engine);
    }

    @Test
    @DisplayName("Shut down aggregation engine 🥳")
    void testShutdown() {
        new AggregationManager(engine).stop();
        verify(engine).stop();
    }
}
