package gov.cms.dpc.api.cli.keys;

import gov.cms.dpc.api.DPCAPIConfiguration;
import gov.cms.dpc.api.DPCAPIService;
import gov.cms.dpc.testing.APIAuthHelpers;
import gov.cms.dpc.testing.NoExitSecurityManager;
import gov.cms.dpc.testing.exceptions.SystemExitException;
import io.dropwizard.cli.Cli;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.util.JarLocation;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockserver.client.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.Parameter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;

class KeyUploadUnitTest {
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
        bootstrap.addCommand(new KeyUpload());

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
    public void testDeleteKeys_happyPath() throws IOException {
        new MockServerClient(taskUri.getHost(), taskUri.getPort())
            .when(
                HttpRequest.request()
                    .withMethod("POST")
                    .withPath(taskUri.getPath() + "upload-key")
                    .withQueryStringParameters(List.of(
                        Parameter.param("organization", "org_id"),
                        Parameter.param("label", "label")
                    ))
            )
            .respond(
                org.mockserver.model.HttpResponse.response()
                    .withStatusCode(HttpStatus.SC_OK)
                    .withBody("org_id")
            );

        Optional<Throwable> errors = Optional.empty();
        try (MockedStatic<Files> files = Mockito.mockStatic(Files.class, Mockito.CALLS_REAL_METHODS)) {
            files.when(() -> Files.readString(eq(Paths.get("key_file")))).thenReturn("fake key data");
            files.when(() -> Files.readString(eq(Paths.get("sig_file")))).thenReturn("fake sig data");

            errors = cli.run("upload", "org_id", "-l", "label", "key_file", "sig_file");
        }
        assertTrue(errors.isEmpty());

        String results = stdOut.toString();
        assertTrue(results.contains("org_id"));
    }

    @Test
    public void testDeleteKeys_badResponse() throws IOException {
        new MockServerClient(taskUri.getHost(), taskUri.getPort())
                .when(
                        HttpRequest.request()
                                .withMethod("POST")
                                .withPath(taskUri.getPath() + "upload-key")
                                .withQueryStringParameters(List.of(
                                        Parameter.param("organization", "org_id"),
                                        Parameter.param("label", "label")
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

        Optional<Throwable> errors = Optional.empty();
        try (MockedStatic<Files> files = Mockito.mockStatic(Files.class, Mockito.CALLS_REAL_METHODS)) {
            files.when(() -> Files.readString(eq(Paths.get("key_file")))).thenReturn("fake key data");
            files.when(() -> Files.readString(eq(Paths.get("sig_file")))).thenReturn("fake sig data");

            errors = cli.run("upload", "org_id", "-l", "label", "key_file", "sig_file");
        }
        assertFalse(errors.isEmpty());

        Throwable throwable = errors.get();
        assertInstanceOf(SystemExitException.class, throwable);
        assertEquals("1", throwable.getMessage());

        System.setSecurityManager(originalSecurityManager);
        assertFalse(stdErr.toString().isEmpty());
    }
}
