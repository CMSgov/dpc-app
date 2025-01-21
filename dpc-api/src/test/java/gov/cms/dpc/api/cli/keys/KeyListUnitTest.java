package gov.cms.dpc.api.cli.keys;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.cms.dpc.api.DPCAPIConfiguration;
import gov.cms.dpc.api.DPCAPIService;
import gov.cms.dpc.api.entities.PublicKeyEntity;
import gov.cms.dpc.api.models.CollectionResponse;
import gov.cms.dpc.testing.APIAuthHelpers;
import gov.cms.dpc.testing.NoExitSecurityManager;
import gov.cms.dpc.testing.exceptions.SystemExitException;
import io.dropwizard.core.cli.Cli;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.util.JarLocation;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.Parameter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class KeyListUnitTest {
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
        bootstrap.addCommand(new KeyList());

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
    public void testListKeys_happyPath() throws IOException {
        PublicKeyEntity publicKeyEntity = new PublicKeyEntity();
        publicKeyEntity.setId(UUID.randomUUID());
        publicKeyEntity.setLabel("test public key");
        publicKeyEntity.setCreatedAt(OffsetDateTime.now());
        CollectionResponse collectionResponse = new CollectionResponse(List.of(publicKeyEntity));

        ObjectMapper mapper = new ObjectMapper();
        String payload = mapper.writeValueAsString(collectionResponse);

        new MockServerClient(taskUri.getHost(), taskUri.getPort())
            .when(
                HttpRequest.request()
                    .withMethod("POST")
                    .withPath(taskUri.getPath() + "list-keys")
                    .withQueryStringParameters(List.of(Parameter.param("organization", "org_id")))
            )
            .respond(
                org.mockserver.model.HttpResponse.response()
                    .withStatusCode(HttpStatus.SC_OK)
                    .withBody(payload)
            );

        Optional<Throwable> errors = cli.run("list", "org_id");
        assertTrue(errors.isEmpty());

        String results = stdOut.toString();
        assertTrue(results.contains("│ test public key │"));
    }

    @Test
    public void testListKeys_badResponse() throws IOException {
        new MockServerClient(taskUri.getHost(), taskUri.getPort())
            .when(
                HttpRequest.request()
                    .withMethod("POST")
                    .withPath(taskUri.getPath() + "list-keys")
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
