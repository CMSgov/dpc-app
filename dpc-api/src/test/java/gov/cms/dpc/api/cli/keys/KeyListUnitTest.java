package gov.cms.dpc.api.cli.keys;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.cms.dpc.api.DPCAPIConfiguration;
import gov.cms.dpc.api.DPCAPIService;
import gov.cms.dpc.api.entities.PublicKeyEntity;
import gov.cms.dpc.api.models.CollectionResponse;
import io.dropwizard.cli.Cli;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.util.JarLocation;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.codehaus.groovy.tools.shell.util.NoExitSecurityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.MockedStatic;

import java.io.*;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class KeyListUnitTest {
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;
    private final InputStream originalIn = System.in;

    private final ByteArrayOutputStream stdOut = new ByteArrayOutputStream();
    private final ByteArrayOutputStream stdErr = new ByteArrayOutputStream();
    private Cli cli;

    @BeforeEach
    void setUp() throws Exception {
        // Setup necessary mock
        final JarLocation location = mock(JarLocation.class);
        when(location.getVersion()).thenReturn(Optional.of("1.0.0"));

        // Add commands you want to test
        final Bootstrap<DPCAPIConfiguration> bootstrap = new Bootstrap<>(new DPCAPIService());
        bootstrap.addCommand(new KeyList());

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
    public void testListKeys_happyPath() throws IOException {
        // Build a mocked http client that returns a successful key list response
        CloseableHttpClient mockClient = mock(CloseableHttpClient.class);
        CloseableHttpResponse mockResponse = mock(CloseableHttpResponse.class, Answers.RETURNS_DEEP_STUBS);

        when(mockClient.execute(any())).thenReturn(mockResponse);
        when(mockResponse.getStatusLine().getStatusCode()).thenReturn(HttpStatus.SC_OK);

        PublicKeyEntity publicKeyEntity = new PublicKeyEntity();
        publicKeyEntity.setId(UUID.randomUUID());
        publicKeyEntity.setLabel("test public key");
        publicKeyEntity.setCreatedAt(OffsetDateTime.now());
        CollectionResponse collectionResponse = new CollectionResponse(List.of(publicKeyEntity));

        ObjectMapper mapper = new ObjectMapper();
        InputStream inputStream = new ByteArrayInputStream(mapper.writeValueAsBytes(collectionResponse));

        when(mockResponse.getEntity().getContent()).thenReturn(inputStream);

        // Sub in our mocked http client and run the command
        try(MockedStatic<HttpClients> httpClients = mockStatic(HttpClients.class)) {
            httpClients.when(HttpClients::createDefault).thenReturn(mockClient);

            Optional<Throwable> errors = cli.run("list", "org_id");
            assertTrue(errors.isEmpty());
        }

        String results = stdOut.toString();
        assertTrue(results.contains("│ test public key │"));
        assertEquals("", stdErr.toString());
    }

    @Test
    public void testListKeys_badResponse() throws IOException {
        // Build a mocked http client that returns a successful key list response
        CloseableHttpClient mockClient = mock(CloseableHttpClient.class);
        CloseableHttpResponse mockResponse = mock(CloseableHttpResponse.class, Answers.RETURNS_DEEP_STUBS);

        when(mockClient.execute(any())).thenReturn(mockResponse);
        when(mockResponse.getStatusLine().getStatusCode()).thenReturn(HttpStatus.SC_BAD_REQUEST);

        // This is kind of kludgey and isn't guaranteed to work for all versions of Java, but it allows us to test error
        // cases that call System.exit()
        SecurityManager originalSecurityManager = System.getSecurityManager();
        System.setSecurityManager(new NoExitSecurityManager());

        // Sub in our mocked http client and run the command
        try(MockedStatic<HttpClients> httpClients = mockStatic(HttpClients.class)) {
            httpClients.when(HttpClients::createDefault).thenReturn(mockClient);

            Optional<Throwable> errors = cli.run("list", "org_id");
            assertFalse(errors.isEmpty());
        }
        System.setSecurityManager(originalSecurityManager);

        assertFalse(stdErr.toString().isEmpty());
    }
}