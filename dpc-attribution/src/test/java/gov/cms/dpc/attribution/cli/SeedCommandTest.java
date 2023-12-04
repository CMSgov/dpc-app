package gov.cms.dpc.attribution.cli;

import gov.cms.dpc.attribution.DPCAttributionConfiguration;
import gov.cms.dpc.attribution.DPCAttributionService;
import io.dropwizard.Application;
import io.dropwizard.cli.Cli;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.testing.DropwizardTestSupport;
import io.dropwizard.util.JarLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SeedCommandTest {

    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;

    private final ByteArrayOutputStream stdOut = new ByteArrayOutputStream();
    private final ByteArrayOutputStream stdErr = new ByteArrayOutputStream();
    private Cli cli;

    private final DropwizardTestSupport<DPCAttributionConfiguration> APPLICATION =
            new DropwizardTestSupport<>(DPCAttributionService.class, "ci.application.conf");

    SeedCommandTest() {
        // Not used
    }

    @BeforeEach
    void cliSetup() throws Exception {
        final JarLocation location = mock(JarLocation.class);
        when(location.getVersion()).thenReturn(Optional.of("1.0.0"));

        APPLICATION.before();
        final Application<DPCAttributionConfiguration> application = APPLICATION.getApplication();
        final Bootstrap<DPCAttributionConfiguration> bootstrap = new Bootstrap<>(application);
        bootstrap.addCommand(new SeedCommand(application));

        // Redirect stdout and stderr to our byte streams
        System.setOut(new PrintStream(stdOut));
        System.setErr(new PrintStream(stdErr));

        cli = new Cli(location, bootstrap, stdOut, stdErr);
    }

    @AfterEach
    void teardown() {
        APPLICATION.after();
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    @Test
    void testSeedCommand() {
        final Optional<Throwable> success = cli.run("seed", "ci.application.conf");
        assertAll(() -> assertTrue(success.isEmpty(), "Should have succeeded"),
                () -> assertEquals("", stdErr.toString(), "Should not have errors"),
                () -> assertTrue(stdOut.toString().contains("Seeding attribution at time ")),
                () -> assertTrue(stdOut.toString().contains("Finished loading seeds")));
    }
}
