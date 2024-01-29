package gov.cms.dpc.api.cli.tokens;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.Parameter;

import com.fasterxml.jackson.databind.ObjectMapper;

import gov.cms.dpc.api.DPCAPIConfiguration;
import gov.cms.dpc.api.DPCAPIService;
import gov.cms.dpc.api.entities.TokenEntity;
import gov.cms.dpc.api.entities.TokenEntity.TokenType;
import gov.cms.dpc.api.models.CollectionResponse;
import gov.cms.dpc.testing.APIAuthHelpers;
import gov.cms.dpc.testing.NoExitSecurityManager;
import gov.cms.dpc.testing.exceptions.SystemExitException;
import io.dropwizard.core.cli.Cli;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.util.JarLocation;

public class TokenListUnitTest {
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;

    private final ByteArrayOutputStream stdOut = new ByteArrayOutputStream();
    private final ByteArrayOutputStream stdErr = new ByteArrayOutputStream();
    private Cli cli;
    private ClientAndServer mockServer;
    private URI taskUri;

    @BeforeEach
    void setUp() throws URISyntaxException {
        // Redirect stdout and stderr to our byte streams
        System.setOut(new PrintStream(stdOut));
        System.setErr(new PrintStream(stdErr));

        // Add commands you want to test
        final Bootstrap<DPCAPIConfiguration> bootstrap = new Bootstrap<>(new DPCAPIService());
        bootstrap.addCommand(new TokenList());

        cli = new Cli(mock(JarLocation.class), bootstrap, stdOut, stdErr);

        taskUri = new URI(APIAuthHelpers.TASK_URL);
        mockServer = ClientAndServer.startClientAndServer(taskUri.getPort());
    }

    @AfterEach
    void teardown() {
        mockServer.stop();

        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    @Test
    public void testListTokens_happyPath() throws IOException {
        UUID org_id = UUID.randomUUID();
        TokenEntity tokenEntity = new TokenEntity("tokenID", org_id, TokenType.OAUTH);
        tokenEntity.setLabel("test list tokens");
        tokenEntity.setCreatedAt(OffsetDateTime.now());
        tokenEntity.setExpiresAt(OffsetDateTime.now());
        tokenEntity.setToken("token");
        CollectionResponse collectionResponse = new CollectionResponse(List.of(tokenEntity));

        ObjectMapper mapper = new ObjectMapper();
        String payload = mapper.writeValueAsString(collectionResponse);

        new MockServerClient(taskUri.getHost(), taskUri.getPort())
            .when(
                HttpRequest.request()
                    .withMethod("POST")
                    .withPath(taskUri.getPath() + "list-tokens")
                    .withQueryStringParameters(List.of(Parameter.param("organization", org_id.toString())))
            )
            .respond(
                org.mockserver.model.HttpResponse.response()
                    .withStatusCode(HttpStatus.SC_OK)
                    .withBody(payload)
            );

        Optional<Throwable> errors = cli.run("list", org_id.toString());
        assertTrue(errors.isEmpty());

        String results = stdOut.toString();
        assertTrue(results.contains("│ test list tokens │"));
    }

    @Test
    public void testListTokens_badResponse() throws IOException {
        new MockServerClient(taskUri.getHost(), taskUri.getPort())
            .when(
                HttpRequest.request()
                    .withMethod("POST")
                    .withPath(taskUri.getPath() + "list-tokens")
                    .withQueryStringParameters(List.of(Parameter.param("organization", "org_id")))
            )
            .respond(
                org.mockserver.model.HttpResponse.response()
                    .withStatusCode(HttpStatus.SC_BAD_REQUEST)
            );

        // This is kind of kludgey and isn't guaranteed to work for all versions of Java, but it allows us to test error
        // cases that call System.exit()
        SecurityManager originalSecurityManager = System.getSecurityManager();
        System.setSecurityManager(new NoExitSecurityManager());

        Optional<Throwable> errors = cli.run("list", "org_id");
        assertFalse(errors.isEmpty());

        Throwable throwable = errors.get();
        assertInstanceOf(SystemExitException.class, throwable);
        assertEquals("1", throwable.getMessage());

        System.setSecurityManager(originalSecurityManager);
        assertFalse(stdErr.toString().isEmpty());
    }
}
