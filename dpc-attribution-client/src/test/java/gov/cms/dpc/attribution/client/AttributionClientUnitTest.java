package gov.cms.dpc.attribution.client;

import ca.uhn.fhir.rest.client.exceptions.FhirClientConnectionException;
import ca.uhn.fhir.rest.gclient.IEntityResult;
import ca.uhn.fhir.rest.gclient.IClientHttpExecutable;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AttributionClientUnitTest extends AbstractAttributionClientResourceUnitTest {

    @ParameterizedTest
    // Check that we can trim whitespace, too
    @ValueSource(strings = {"true", "  true\n"})
    void testCheckHealth(String body) throws IOException {
        InputStream input = mockTextResponse("_healthy", body);

        assertTrue(client.checkHealth());
        verify(attributionClient.rawHttpRequest().get("_healthy")).accept("text/plain");
        verify(input).close();
    }

    @Test
    void testCheckHealthFalse() throws IOException {
        InputStream input = mockTextResponse("_healthy", "false");

        assertFalse(client.checkHealth());
        verify(input).close();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "not healthy", "<html>Error</html>"})
    void testCheckHealthHandlesBadResponse(String body) throws IOException {
        InputStream input = mockTextResponse("_healthy", body);

        IllegalStateException failure = assertThrows(IllegalStateException.class, client::checkHealth);
        assertEquals("Attribution service returned an invalid health response", failure.getMessage());
        verify(input).close();
    }

    @Test
    void testCheckHealthHandlesException() {
        InternalErrorException failure = new InternalErrorException("Service unavailable");
        when(mockRawRequest("_healthy").execute()).thenThrow(failure);

        assertSame(failure, assertThrows(InternalErrorException.class, client::checkHealth));
    }

    @Test
    void testGetVersion() throws IOException {
        String version = "fake_version";
        InputStream input = mockTextResponse("version", version);

        assertEquals(version, client.getVersion());
        verify(attributionClient.rawHttpRequest().get("version")).accept("text/plain");
        verify(input).close();
    }

    @Test
    void testGetVersionHandlesException() {
        FhirClientConnectionException failure = new FhirClientConnectionException("Connection failed");
        when(mockRawRequest("version").execute()).thenThrow(failure);

        assertSame(failure, assertThrows(FhirClientConnectionException.class, client::getVersion));
    }

    @Test
    void testReadFailureClosesResponseStream() throws IOException {
        InputStream input = mock(InputStream.class);
        IOException failure = new IOException("Read failed");
        when(input.readAllBytes()).thenThrow(failure);
        mockResponseStream("version", input);

        UncheckedIOException exception = assertThrows(UncheckedIOException.class, client::getVersion);
        assertSame(failure, exception.getCause());
        verify(input).close();
    }

    private InputStream mockTextResponse(String path, String text) {
        InputStream input = spy(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)));
        mockResponseStream(path, input);
        return input;
    }

    private void mockResponseStream(String path, InputStream input) {
        IEntityResult response = mock(IEntityResult.class);
        when(mockRawRequest(path).execute()).thenReturn(response);
        when(response.getInputStream()).thenReturn(input);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private IClientHttpExecutable<?, IEntityResult> mockRawRequest(String path) {
        IClientHttpExecutable request = mock(IClientHttpExecutable.class, RETURNS_SELF);
        when(attributionClient.rawHttpRequest().get(path)).thenReturn(request);
        return request;
    }
}
