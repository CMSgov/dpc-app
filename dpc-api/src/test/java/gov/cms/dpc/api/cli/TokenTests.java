package gov.cms.dpc.api.cli;

import gov.cms.dpc.api.AbstractApplicationTest;
import gov.cms.dpc.api.DPCAPIConfiguration;
import gov.cms.dpc.api.DPCAPIService;
import gov.cms.dpc.api.cli.organizations.OrganizationRegistration;
import gov.cms.dpc.api.cli.tokens.TokenDelete;
import gov.cms.dpc.api.cli.tokens.TokenList;
import io.dropwizard.cli.Cli;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.util.JarLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TokenTests extends AbstractApplicationTest {

    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;
    private final InputStream originalIn = System.in;

    private final ByteArrayOutputStream stdOut = new ByteArrayOutputStream();
    private final ByteArrayOutputStream stdErr = new ByteArrayOutputStream();
    private Cli cli;

    TokenTests() {

    }

    @BeforeEach
    void cliSetup() {
        // Setup necessary mock
        final JarLocation location = mock(JarLocation.class);
        when(location.getVersion()).thenReturn(Optional.of("1.0.0"));

        // Add commands you want to test
        final Bootstrap<DPCAPIConfiguration> bootstrap = new Bootstrap<>(new DPCAPIService());
        bootstrap.addCommand(new OrganizationRegistration());
        bootstrap.addCommand(new TokenList());
        bootstrap.addCommand(new TokenDelete());

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
        System.setIn(originalIn);
    }

    @Test
    void testTokenDeletion() throws Exception {
        // Create the organization
        final boolean success = cli.run("register", "-f", "../src/main/resources/organization.tmpl.json");

        assertAll(() -> assertTrue(success, "Should have succeeded"),
                () -> assertEquals("", stdErr.toString(), "Should not have errors"),
                () -> assertTrue(stdOut.toString().contains("eyJ2IjoyLCJsIjoiaHR0"), "Should have token"));

        // Pull out the organization ID
        final Matcher matcher = Pattern.compile("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}").matcher(stdOut.toString());

        assertTrue(matcher.find(), "Should find Organization ID");

        // List the tokens
        final String organizationID = matcher.group(0);
        final List<UUID> matchedTokenIDs = getTokenIDs(organizationID);
        assertEquals(1, matchedTokenIDs.size(), "Should have a single token id");

        // Try to remove it
        stdOut.reset();
        stdErr.reset();
        final boolean s3 = cli.run("delete", "-o", organizationID, matchedTokenIDs.get(0).toString());

        assertTrue(s3, "Should have succeeded");

        final List<UUID> tokenIDs = getTokenIDs(organizationID);
        assertTrue(tokenIDs.isEmpty(), "Should not have any tokens");
    }

    List<UUID> getTokenIDs(String organizationID) throws Exception {
        stdOut.reset();
        stdErr.reset();

        final boolean s2 = cli.run("list", organizationID);

        // Find all of the token IDs
        final List<UUID> matchedTokenIDs = Pattern.compile("║\\s([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})")
                .matcher(stdOut.toString())
                .results()
                .map(MatchResult::group)
                .map(match -> match.replace("║ ", ""))
                .map(UUID::fromString)
                .collect(Collectors.toList());

        assertAll(() -> assertTrue(s2, "Should have succeeded"),
                () -> assertTrue(stdErr.toString().isEmpty(), "Should be empty"));

        return matchedTokenIDs;
    }

}
