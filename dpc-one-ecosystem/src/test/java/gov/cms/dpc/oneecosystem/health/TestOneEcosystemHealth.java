package gov.cms.dpc.oneecosystem.health;

import com.codahale.metrics.health.HealthCheck;
import gov.cms.dpc.oneecosystem.client.OneEcosystemClient;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TestOneEcosystemHealth {

    @Test
    void testHealth() {
        OneEcosystemClient client = mock(OneEcosystemClient.class);
        when(client.healthy()).thenReturn(true);
        OneEcosystemHealthCheck sharedBackendHealthCheck = new OneEcosystemHealthCheck(client);

        HealthCheck.Result check = sharedBackendHealthCheck.check();
        assertTrue(check.isHealthy());
    }

    @Test
    void testUnhealthy() {
        OneEcosystemClient client = mock(OneEcosystemClient.class);
        when(client.healthy()).thenReturn(false);
        OneEcosystemHealthCheck sharedBackendHealthCheck = new OneEcosystemHealthCheck(client);

        HealthCheck.Result check = sharedBackendHealthCheck.check();
        assertFalse(check.isHealthy());
    }

    @Test
    void testTimeout() {
        OneEcosystemClient client = mock(OneEcosystemClient.class);
        when(client.healthy()).thenThrow(new RuntimeException("an unexpected error!"));
        OneEcosystemHealthCheck sharedBackendHealthCheck = new OneEcosystemHealthCheck(client);

        HealthCheck.Result check = sharedBackendHealthCheck.check();
        assertFalse(check.isHealthy());
    }
}
