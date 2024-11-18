package gov.cms.dpc.consent.cli;

import gov.cms.dpc.consent.DPCConsentConfiguration;
import gov.cms.dpc.consent.DPCConsentService;
import gov.cms.dpc.testing.DummyJarLocation;
import io.dropwizard.core.cli.Cli;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.util.JarLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;

@Disabled
@DisplayName("Seed commands")
class SeedCommandTest {

    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;
    private final InputStream originalIn = System.in;

    private final ByteArrayOutputStream stdOut = new ByteArrayOutputStream();
    private final ByteArrayOutputStream stdErr = new ByteArrayOutputStream();

    private Cli cli;

    SeedCommandTest() {
        // Not used
    }

    @BeforeEach
    void cliSetup() {
        final JarLocation location = new DummyJarLocation();

        final Bootstrap<DPCConsentConfiguration> bootstrap = new Bootstrap<>(new DPCConsentService());
        bootstrap.addCommand(new SeedCommand(bootstrap.getApplication()));

        // Redirect stdout and stderr to our byte streams
        System.setOut(new PrintStream(stdOut));
        System.setErr(new PrintStream(stdErr));

        cli = new Cli(location, bootstrap, stdOut, stdErr);
    }

    @AfterEach
    void teardown() {
        System.setOut(originalOut);
        System.setErr(originalErr);
        System.setIn(originalIn);
    }

    @Test
    @DisplayName("Run seed command 🥳")
    void testSeedCommand() throws Exception {

        final Optional<Throwable> success = cli.run("seed", "src/test/resources/test.application.yml");
        /* dies here with the following error
        Should not have errors ==> expected: <> but was: <io.dropwizard.configuration.ConfigurationParsingException: default configuration has an error:
          * Unrecognized field at: consentDatabase
                Did you mean?:
                - consentdb
                        - config
                        - metrics
                        - logging
                        - server

        This is because consentdb (annotation on Config database property) does not match getter name. However, making them match by changing
        the setter name results in the property values not being injected correctly.
        */
        assertAll(() -> assertTrue(success.isPresent(), "Should have succeeded"),
                () -> assertEquals("", stdErr.toString(), "Should not have errors"));

        // todo confirm 10 seeds are present (count)

        // todo check one randomly
    }
}
