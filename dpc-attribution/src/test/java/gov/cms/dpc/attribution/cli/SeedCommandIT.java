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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;

@IntegrationTest
@DisplayName("Seed command submission")
public class SeedCommandIT {

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
@DisplayName("Run seed command 🥳")

    void testSeedCommand() {
        final Optional<Throwable> success = cli.run("seed", "src/test/resources/test.application.yml");
        assertTrue(success.isEmpty(), "Should have succeeded");
        assertEquals("", stdErr.toString(), "Should not have errors");
    }
}
