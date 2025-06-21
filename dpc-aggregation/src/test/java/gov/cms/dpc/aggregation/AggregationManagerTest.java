package gov.cms.dpc.aggregation;

import gov.cms.dpc.aggregation.engine.AggregationEngine;
import gov.cms.dpc.common.utils.CurrentEngineState;
import gov.cms.dpc.testing.BufferedLoggerHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(BufferedLoggerHandler.class)
class AggregationManagerTest {

    private AggregationEngine engine;

    @BeforeEach
    void setup() {
        engine = mock(AggregationEngine.class);
        Mockito.reset(engine);
    }

    @Test
    void testShutdown() throws InterruptedException {
        CurrentEngineState state = new CurrentEngineState();

        // Create a new thread that waits for the engine state to become STOPPING, then sets it to STOPPED.  This mimics
        // what happens when the aggregation engine finishes its last batch.
        ExecutorService service = Executors.newFixedThreadPool(2);
        service.execute(() -> {
            synchronized (state) {
                while (state.getState() != CurrentEngineState.States.STOPPING) {
                    try {
                        state.wait();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                state.setState(CurrentEngineState.States.STOPPED);
            }
        });

        // Calling stop() sets status to STOPPING and waits for it to become STOPPED, then calles engine.stop()
        new AggregationManager(engine, state).stop();
        verify(engine).stop();
    }
}
