package gov.cms.dpc.aggregation;

import com.codahale.metrics.health.HealthCheckRegistry;
import gov.cms.dpc.testing.BufferedLoggerHandler;
import gov.cms.dpc.testing.IntegrationTest;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.DropwizardTestSupport;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.SortedSet;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

@IntegrationTest
@ExtendWith(BufferedLoggerHandler.class)
public class AggregationServiceTest {

    private static final DropwizardTestSupport<DPCAggregationConfiguration> APPLICATION = new DropwizardTestSupport<>(DPCAggregationService.class, null, ConfigOverride.config("server.applicationConnectors[0].port", "7777"));

    @BeforeAll
    static void start() {
        APPLICATION.before();
    }

    @AfterAll
    static void stop() {
        APPLICATION.after();
    }

    @Test
    void testHealthChecks() {
        final HealthCheckRegistry checks = APPLICATION.getEnvironment().healthChecks();
        final SortedSet<String> names = checks.getNames();

        // Ensure that the various healthchecks are propagated from the modules
        assertAll(() -> assertTrue(names.contains("BlueButtonHealthCheck"), "Should have BB health check"));

        // Everything should be true
        checks.runHealthChecks().forEach((key, value) -> assertTrue(value.isHealthy(), String.format("Healthcheck: %s is not ok.", key)));
    }
}
