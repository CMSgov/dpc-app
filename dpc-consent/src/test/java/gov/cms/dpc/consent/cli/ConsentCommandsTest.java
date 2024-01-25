package gov.cms.dpc.consent.cli;

import gov.cms.dpc.consent.DPCConsentConfiguration;
import gov.cms.dpc.consent.DPCConsentService;
import gov.cms.dpc.testing.IntegrationTest;
import io.dropwizard.cli.Cli;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.testing.POJOConfigurationFactory;
import io.dropwizard.util.JarLocation;
import org.junit.jupiter.api.*;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@IntegrationTest
class ConsentCommandsTest {

    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;
    private final InputStream originalIn = System.in;

    private ByteArrayOutputStream stdOut = new ByteArrayOutputStream();
    private ByteArrayOutputStream stdErr = new ByteArrayOutputStream();

    private static final DPCConsentService app = new DPCConsentService();
    private static final Bootstrap<DPCConsentConfiguration> bs = setupBootstrap();

    private Cli cli;

    private static Bootstrap<DPCConsentConfiguration> setupBootstrap() {
        // adapted from DropwizardTestSupport
        Bootstrap<DPCConsentConfiguration> bootstrap = new Bootstrap<>(ConsentCommandsTest.app) {
            public void run(DPCConsentConfiguration configuration, Environment environment) throws Exception {
                super.run(configuration, environment);
                setConfigurationFactoryFactory((klass, validator, objectMapper, propertyPrefix) ->
                        new POJOConfigurationFactory<>(configuration));
            }
        };
        ConsentCommandsTest.app.initialize(bootstrap);
        return bootstrap;
    }

    @BeforeEach
    void cliSetup() {
        final JarLocation location = mock(JarLocation.class);
        when(location.getVersion()).thenReturn(Optional.of("1.0.0"));
        stdOut = new ByteArrayOutputStream();
        stdErr = new ByteArrayOutputStream();
        // Redirect stdout and stderr to our byte streams
        System.setOut(new PrintStream(stdOut));
        System.setErr(new PrintStream(stdErr));

        cli = new Cli(location, bs, stdOut, stdErr);
    }

    @AfterEach
    void teardown() {
        System.setOut(originalOut);
        System.setErr(originalErr);
        System.setIn(originalIn);
    }

    @Test
    final void pertinentHelpMessageDisplayed() {
        final Optional<Throwable> t1 = cli.run("consent", "create", "-h");
        String errorMsg = String.format("Should have pertinent help message, got: %s", stdOut.toString());
        assertAll(() -> assertTrue(t1.isPresent(), "Should have succeeded"),
                () -> assertEquals("", stdErr.toString(), "Should not have errors"),
                () -> assertTrue(stdOut.toString().contains("Create a new consent record"), errorMsg));
    }

    @Test
    final void onlyAllowsInOrOut() {
        final Optional<Throwable> t1 = cli.run("consent", "create", "-p", "t2-mbi", "-d", "2019-11-22", "-i", "-o", "--host", "http://localhost:3500/v1");
        assertAll(() -> assertFalse(t1.isPresent(), "Should have failed"),
                () -> assertEquals("", stdOut.toString(), "Should not have output"),
                () -> assertNotEquals("", stdErr.toString(), "Should have errors"),
                () -> assertTrue(stdErr.toString().contains("argument -o/--out: not allowed with argument -i/--in"), "Should have '-o not allowed with -i' help message"));
    }

    @Test
    final void detectsInvalidDate() {
        final Optional<Throwable> t5 = cli.run("consent", "create", "-p", "tA-mbi", "-d", "Nov 22 2019", "-i", "--host", "http://localhost:3500/v1");
        assertAll(() -> assertFalse(t5.isPresent(), "Should have failed"),
                () -> assertEquals("", stdOut.toString(), "Should not have output"),
                () -> assertNotEquals("", stdErr.toString(), "Should have errors"),
                () -> assertTrue(stdErr.toString().contains("java.time.format.DateTimeParseException"), "Should have date parsing error"));
    }

    @Test
    final void createDefaultOptInRecord() {
        final Optional<Throwable> t2 = cli.run("consent", "create", "-p", "t2-mbi", "-d", "2019-11-22", "-i", "--host", "http://localhost:3500/v1");
        assertAll(() -> assertTrue(t2.isPresent(), "Should have succeeded"),
                () -> assertEquals("", stdErr.toString(), "Should not have errors"));
    }

    @Test
    final void createDefaultOptOutRecord() {
        final Optional<Throwable> t3 = cli.run("consent", "create", "-p", "t3-mbi", "-d", "2019-11-23", "-o", "--host", "http://localhost:3500/v1");
        assertAll(() -> assertTrue(t3.isPresent(), "Should have succeeded"),
                () -> assertEquals("", stdErr.toString(), "Should not have errors"));
    }
}
