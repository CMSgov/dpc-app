package gov.cms.dpc.api.cli;

import gov.cms.dpc.api.AbstractApplicationTest;
import gov.cms.dpc.api.DPCAPIConfiguration;
import gov.cms.dpc.api.DPCAPIService;
import gov.cms.dpc.api.cli.organizations.OrganizationDelete;
import gov.cms.dpc.api.cli.organizations.OrganizationList;
import gov.cms.dpc.api.cli.organizations.OrganizationRegistration;
import io.dropwizard.core.cli.Cli;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.util.JarLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OrganizationTests extends AbstractApplicationTest {
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;

    private final ByteArrayOutputStream stdOut = new ByteArrayOutputStream();
    private final ByteArrayOutputStream stdErr = new ByteArrayOutputStream();
    private Cli cli;

    OrganizationTests() {
        // not used
    }

    @BeforeEach
    void cliSetup() {
        // Setup necessary mock
        final JarLocation location = mock(JarLocation.class);
        when(location.getVersion()).thenReturn(Optional.of("1.0.0"));

        // Add commands you want to test
        final Bootstrap<DPCAPIConfiguration> bootstrap = new Bootstrap<>(new DPCAPIService());
        bootstrap.addCommand(new OrganizationRegistration());
        bootstrap.addCommand(new OrganizationList());
        bootstrap.addCommand(new OrganizationDelete());

        // Redirect stdout and stderr to our byte streams
        System.setOut(new PrintStream(stdOut));
        System.setErr(new PrintStream(stdErr));

        // Build what'll run the command and interpret arguments
        cli = new Cli(location, bootstrap, stdOut, stdErr);
    }

    @AfterEach
    void teardown() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    @Test
    void testOrganizationCommands() {
        // Create the organization
        final Optional<Throwable> register = cli.run("register", "-f", "../src/main/resources/organization.tmpl.json", "--no-token", "--host", "http://localhost:3500/v1");
        assertAll(() -> assertTrue(register.isEmpty(), "Should have succeeded"),
                () -> assertEquals("", stdErr.toString(), "Should not have errors"),
                () -> assertTrue(stdOut.toString().contains("Registered organization:")));

        // Pull out the organization ID
        final Matcher matcher = Pattern.compile("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}").matcher(stdOut.toString());
        assertTrue(matcher.find(), "Should find organization ID");
        final String orgId = matcher.group(0);

        stdOut.reset();
        stdErr.reset();

        // List organizations
        final Optional<Throwable> listOrgs = cli.run("list", "--host", "http://localhost:3500/v1");
        assertAll(() -> assertTrue(listOrgs.isEmpty(), "Should have succeeded"),
                () -> assertEquals("", stdErr.toString(), "Should not have errors"),
                () -> assertTrue(stdOut.toString().contains(orgId)));

        stdOut.reset();
        stdErr.reset();

        // Delete the organization
        final String orgReference = "Organization/" + orgId;
        final Optional<Throwable> delete = cli.run("delete", orgReference, "--host", "http://localhost:3500/v1");
        assertAll(() -> assertTrue(delete.isEmpty(), "Should have succeeded"),
                () -> assertEquals("", stdErr.toString(), "Should not have errors"),
                () -> assertTrue(stdOut.toString().contains("Successfully deleted Organization")));

        stdOut.reset();
        stdErr.reset();

        // Confirm the organization has been deleted
        final Optional<Throwable> listEmpty = cli.run("list", "--host", "http://localhost:3500/v1");
        assertAll(() -> assertTrue(listEmpty.isEmpty(), "Should have succeeded"),
                () -> assertEquals("", stdErr.toString(), "Should not have errors"),
                () -> assertFalse(stdOut.toString().contains(orgId), "Should not list organization"));
    }
}
