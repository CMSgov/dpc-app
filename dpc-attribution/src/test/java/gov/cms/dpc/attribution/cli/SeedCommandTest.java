package gov.cms.dpc.attribution.cli;

import gov.cms.dpc.attribution.AbstractAttributionTest;
import gov.cms.dpc.attribution.DPCAttributionConfiguration;
import gov.cms.dpc.attribution.DPCAttributionService;
import gov.cms.dpc.testing.IntegrationTest;
import io.dropwizard.core.cli.Cli;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.testing.POJOConfigurationFactory;
import io.dropwizard.util.JarLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// TODO: dropwizard - debug
@Disabled
@IntegrationTest
public class SeedCommandTest extends AbstractAttributionTest {

    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;
    private final ByteArrayOutputStream stdErr = new ByteArrayOutputStream();

    private static final DPCAttributionService app = new DPCAttributionService();
    private static final Bootstrap<DPCAttributionConfiguration> bs = setupBootstrap();

    private Cli cli;

    private static Bootstrap<DPCAttributionConfiguration> setupBootstrap() {
        // adapted from DropwizardTestSupport
        Bootstrap<DPCAttributionConfiguration> bootstrap = new Bootstrap<>(app) {
            @Override
            public void run(DPCAttributionConfiguration configuration, Environment environment) throws Exception {
                super.run(configuration, environment);
                setConfigurationFactoryFactory((klass, validator, objectMapper, propertyPrefix) ->
                        new POJOConfigurationFactory<>(configuration));
            }
        };
        app.initialize(bootstrap);
        return bootstrap;
    }

    @BeforeEach
    void cliSetup() {
        final JarLocation location = mock(JarLocation.class);
        when(location.getVersion()).thenReturn(Optional.of("1.0.0"));

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
        //assertTrue(success.isEmpty(), "Should have succeeded");
        assertEquals("", stdErr.toString(), "Should not have errors");
    }
}
