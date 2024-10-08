package gov.cms.dpc.consent.cli;

import ch.qos.logback.classic.LoggerContext;
import gov.cms.dpc.consent.DPCConsentConfiguration;
import gov.cms.dpc.consent.DPCConsentService;
import gov.cms.dpc.testing.DummyJarLocation;
import gov.cms.dpc.testing.IntegrationTest;
import io.dropwizard.core.cli.Cli;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.testing.POJOConfigurationFactory;
import io.dropwizard.util.JarLocation;
import org.junit.jupiter.api.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@IntegrationTest
class ConsentCommandsTest {

    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;

    private final ByteArrayOutputStream stdOut = new ByteArrayOutputStream();
    private final ByteArrayOutputStream stdErr = new ByteArrayOutputStream();

    private static final DPCConsentService app = new DPCConsentService();
    private static final Bootstrap<DPCConsentConfiguration> bs = setupBootstrap(app);

    private static final String configPath = "src/test/resources/test.application.yml";

    private Cli cli;

    private static Bootstrap<DPCConsentConfiguration> setupBootstrap(DPCConsentService app) {
        // adapted from DropwizardTestSupport
        Bootstrap<DPCConsentConfiguration> bootstrap = new Bootstrap<>(app) {
            public void run(DPCConsentConfiguration configuration, Environment environment) throws Exception {
                super.run(configuration, environment);
                setConfigurationFactoryFactory((klass, validator, objectMapper, propertyPrefix) ->
                        new POJOConfigurationFactory<>(configuration));
            }
        };
        app.initialize(bootstrap);
        return bootstrap;
    }

    @BeforeAll
    void cliSetup() throws Exception {
        // Redirect stdout and stderr to our byte streams
        System.setOut(new PrintStream(stdOut));
        System.setErr(new PrintStream(stdErr));

        final JarLocation location = new DummyJarLocation();

        cli = new Cli(location, bs, stdOut, stdErr);
    }

    @BeforeEach
    void stopLogging() {
        ((LoggerContext)org.slf4j.LoggerFactory.getILoggerFactory()).stop();
    }

    @AfterAll
    void teardown() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    @AfterEach
    void resetIO() {
        stdOut.reset();
        stdErr.reset();
    }

    @Test
    final void pertinentHelpMessageDisplayed() throws Exception {
        final Optional<Throwable> t1 = cli.run("consent", "create", "-h");
        String errorMsg = String.format("Should have pertinent help message, got: %s", stdOut.toString());
        assertAll(() -> assertFalse(t1.isPresent(), "Should have succeeded"),
                () -> assertEquals("", stdErr.toString(), "Should not have errors"),
                () -> assertTrue(stdOut.toString().contains("Create a new consent record"), errorMsg));
    }

    @Test
    final void onlyAllowsInOrOut() throws Exception {
        final Optional<Throwable> t1 = cli.run("consent", "create", configPath, "-p", "t2-mbi", "-d", "2019-11-22", "-i", "-o", "--host", "http://localhost:3500/v1");
        assertAll(() -> assertTrue(t1.isPresent(), "Should have failed"),
                () -> assertEquals("", stdOut.toString(), "Should not have output"),
                () -> assertNotEquals("", stdErr.toString(), "Should have errors"),
                () -> assertTrue(stdErr.toString().contains("argument -o/--out: not allowed with argument -i/--in"), "Should have '-o not allowed with -i' help message"));
    }

    @Test
    final void detectsInvalidDate() throws Exception {
        final Optional<Throwable> t5 = cli.run("consent", "create", configPath, "-p", "tA-mbi", "-d", "Nov 22 2019", "-i", "--host", "http://localhost:3500/v1");
        assertAll(() -> assertTrue(t5.isPresent(), "Should have failed"),
                () -> assertEquals("", stdOut.toString(), "Should not have output"),
                () -> assertNotEquals("", stdErr.toString(), "Should have errors"),
                () -> assertTrue(stdErr.toString().contains("java.time.format.DateTimeParseException"), "Should have date parsing error"));
    }

    @Test
    final void createDefaultOptInRecord() throws Exception {
        final Optional<Throwable> t2 = cli.run("consent", "create", configPath, "-p", "t2-mbi", "-d", "2019-11-22", "-i", "--host", "http://localhost:3500/v1");
        assertAll(() -> assertFalse(t2.isPresent(), "Should have succeeded"),
                () -> assertEquals("", stdErr.toString(), "Should not have errors"));
    }

    @Test
    final void createDefaultOptOutRecord() throws Exception {
        final Optional<Throwable> t3 = cli.run("consent", "create", configPath, "-p", "t3-mbi", "-d", "2019-11-23", "-o", "--host", "http://localhost:3500/v1");
        assertAll(() -> assertFalse(t3.isPresent(), "Should have succeeded"),
                () -> assertEquals("", stdErr.toString(), "Should not have errors"));
    }
}
