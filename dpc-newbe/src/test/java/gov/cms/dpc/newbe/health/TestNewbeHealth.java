package gov.cms.dpc.newbe.health;

import com.codahale.metrics.health.HealthCheck;
import gov.cms.dpc.newbe.client.NewbeClient;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TestNewbeHealth {

    @Test
    void testHealth() {
        NewbeClient client = mock(NewbeClient.class);
        when(client.healthy()).thenReturn(true);
        NewbeHealthCheck newbeHealthCheck = new NewbeHealthCheck(client);

        HealthCheck.Result check = newbeHealthCheck.check();
        assertTrue(check.isHealthy());
    }

    @Test
    void testUnhealthy() {
        NewbeClient client = mock(NewbeClient.class);
        when(client.healthy()).thenReturn(false);
        NewbeHealthCheck newbeHealthCheck = new NewbeHealthCheck(client);

        HealthCheck.Result check = newbeHealthCheck.check();
        assertFalse(check.isHealthy());
    }

    @Test
    void testTimeout() {
        NewbeClient client = mock(NewbeClient.class);
        when(client.healthy()).thenThrow(new RuntimeException("an unexpected error!"));
        NewbeHealthCheck newbeHealthCheck = new NewbeHealthCheck(client);

        HealthCheck.Result check = newbeHealthCheck.check();
        assertFalse(check.isHealthy());
    }
}
