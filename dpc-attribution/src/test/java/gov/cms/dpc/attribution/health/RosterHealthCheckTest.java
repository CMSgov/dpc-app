package gov.cms.dpc.attribution.health;

import com.codahale.metrics.health.HealthCheck;
import gov.cms.dpc.attribution.exceptions.AttributionException;
import gov.cms.dpc.attribution.jdbi.RosterEngine;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

class RosterHealthCheckTest {

    private static final String MESSAGE = "I'm thrown";

    @Test
    void testHealthy() {

        final RosterEngine rosterEngine = mock(RosterEngine.class);

        final RosterEngineHealthCheck healthCheck = new RosterEngineHealthCheck(rosterEngine);
        assertTrue(healthCheck.check().isHealthy(), "Should be healthy");
    }

    @Test
    void testUnhealthy() {

        final RosterEngine rosterEngine = mock(RosterEngine.class);

        doThrow(new AttributionException(MESSAGE)).when(rosterEngine).assertHealthy();

        final RosterEngineHealthCheck healthCheck = new RosterEngineHealthCheck(rosterEngine);
        final HealthCheck.Result checked = healthCheck.check();
        assertAll(() -> assertFalse(checked.isHealthy(), "Should be healthy"),
                () -> assertEquals(MESSAGE, checked.getMessage(), "Message should match"));
    }
}
