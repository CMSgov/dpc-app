package gov.cms.dpc.attribution.cli;

import gov.cms.dpc.attribution.DPCAttributionConfiguration;
import gov.cms.dpc.attribution.DPCAttributionService;
import gov.cms.dpc.testing.DummyJarLocation;
import gov.cms.dpc.testing.IntegrationTest;
import io.dropwizard.core.cli.Cli;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.testing.POJOConfigurationFactory;
import io.dropwizard.util.JarLocation;
import org.jooq.conf.Settings;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.sql.Connection;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;

@IntegrationTest
class SeedCommandTest {

    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;
    private final ByteArrayOutputStream stdErr = new ByteArrayOutputStream();

    private Cli cli;

    @BeforeEach
    void setup() {
        final JarLocation location = new DummyJarLocation();

        // Configure bootstrap - adapted from DropwizardTestSupport
        DPCAttributionService app = new DPCAttributionService();
        Bootstrap<DPCAttributionConfiguration> bs = new Bootstrap<>(app) {
            @Override
            public void run(DPCAttributionConfiguration configuration, Environment environment) throws Exception {
                super.run(configuration, environment);
                setConfigurationFactoryFactory((klass, validator, objectMapper, propertyPrefix) ->
                        new POJOConfigurationFactory<>(configuration));
            }
        };
        app.initialize(bs);

        // Redirect stderr to our byte stream
        System.setErr(new PrintStream(stdErr));

        cli = new Cli(location, bs, originalOut, stdErr);
    }

    @AfterEach
    void teardown() {
        System.setErr(originalErr);
    }

    @Test
    void testSeedCommand() {
        final Optional<Throwable> success = cli.run("seed", "src/test/resources/test.application.yml");
        assertTrue(success.isEmpty(), "Should have succeeded");
        assertTrue(stdErr.toString().isEmpty(), "Should not have errors");
    }

    @Test
    void testConnectionError() {
        String errMsg = "Connection error";
        try (MockedStatic<DSL> mockedDSL = Mockito.mockStatic(DSL.class)) {
            mockedDSL.when(() -> DSL.using(any(Connection.class), any(Settings.class)))
                    .thenThrow(new RuntimeException(errMsg));

            final Optional<Throwable> failure = cli.run("seed", "src/test/resources/test.application.yml");
            assertFalse(failure.isEmpty(), "Should have not succeeded");
            assertTrue(stdErr.toString().contains(errMsg), "Should have error message");
        }
    }
}
