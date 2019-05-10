package gov.cms.dpc.api.health;

import com.codahale.metrics.health.HealthCheck;
import gov.cms.dpc.common.interfaces.AttributionEngine;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

class AttributionHealthCheckTest {


    private static final String MESSAGE = "Unhealthy";

    @Test
    void testHealthy() {

        final AttributionEngine engine = mock(AttributionEngine.class);
        final AttributionHealthCheck check = new AttributionHealthCheck(engine);
        assertTrue(check.check().isHealthy(), "Should be healthy");
    }

    @Test
    void testUnhealthy() {
        final AttributionEngine engine = mock(AttributionEngine.class);
        doThrow(new RuntimeException(MESSAGE)).when(engine).assertHealthy();
        final HealthCheck.Result result = new AttributionHealthCheck(engine).check();

        assertAll(() -> assertFalse(result.isHealthy(), "Should not be healthy"),
                () -> assertEquals(MESSAGE, result.getMessage(), "Should pass the failure message"));
    }
}
