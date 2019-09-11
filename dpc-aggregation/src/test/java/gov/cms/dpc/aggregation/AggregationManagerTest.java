package gov.cms.dpc.aggregation;

import gov.cms.dpc.aggregation.engine.AggregationEngineV2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AggregationManagerTest {

    private AggregationEngineV2 engine;

    @BeforeEach
    void setup() {
        engine = mock(AggregationEngineV2.class);
        Mockito.reset(engine);
    }

    @Test
    void testShutdown() {
        new AggregationManager(engine).stop();
        verify(engine).stop();
    }
}
