package gov.cms.dpc.api.cli.keys;

import gov.cms.dpc.api.DPCAPIConfiguration;
import gov.cms.dpc.api.DPCAPIService;
import gov.cms.dpc.testing.APIAuthHelpers;
import gov.cms.dpc.testing.NoExitSecurityManager;
import io.dropwizard.cli.Cli;
import io.dropwizard.setup.Bootstrap;
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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class KeyDeleteUnitTest {
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
        bootstrap.addCommand(new KeyDelete());

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
    public void testDeleteKeys_happyPath() {
        new MockServerClient(taskUri.getHost(), taskUri.getPort())
            .when(
                HttpRequest.request()
                    .withMethod("POST")
                    .withPath(taskUri.getPath() + "delete-key")
                    .withQueryStringParameters(List.of(
                        Parameter.param("organization", "org_id"),
                        Parameter.param("key", "key_id")
                    ))
            )
            .respond(
                org.mockserver.model.HttpResponse.response()
                    .withStatusCode(HttpStatus.SC_OK)
            );

        Optional<Throwable> errors = cli.run("delete", "-o", "org_id", "key_id");
        assertTrue(errors.isEmpty());

        String results = stdOut.toString();
        assertTrue(results.contains("Successfully deleted public key"));
    }

    @Test
    public void testDeleteKeys_badResponse() throws IOException {
        new MockServerClient(taskUri.getHost(), taskUri.getPort())
            .when(
                HttpRequest.request()
                    .withMethod("POST")
                    .withPath(taskUri.getPath() + "delete-key")
                    .withQueryStringParameters(List.of(
                        Parameter.param("organization", "org_id"),
                        Parameter.param("key", "key_id")
                    ))
            )
            .respond(
                org.mockserver.model.HttpResponse.response()
                    .withStatusCode(HttpStatus.SC_NOT_FOUND)
            );

        // This is kind of kludgey and isn't guaranteed to work for all versions of Java, but it allows us to test error
        // cases that call System.exit()
        SecurityManager originalSecurityManager = System.getSecurityManager();
        System.setSecurityManager(new NoExitSecurityManager());

        Optional<Throwable> errors = cli.run("delete", "-o", "org_id", "key_id");
        assertFalse(errors.isEmpty());

        Throwable throwable = errors.get();
        assertInstanceOf(RuntimeException.class, throwable);
        assertEquals("1", throwable.getMessage());

        System.setSecurityManager(originalSecurityManager);
        assertFalse(stdErr.toString().isEmpty());
    }
}
