package gov.cms.dpc.fhir.dropwizard.filters;

import gov.cms.dpc.fhir.FHIRMediaTypes;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.UriInfo;
import org.eclipse.jetty.server.Response;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings({"rawtypes", "unchecked"})
public class FHIRRequestFilterTest {

    private static final FHIRRequestFilter filter = new FHIRRequestFilter();

    @Test
    void testSuccess() throws URISyntaxException {
        final MultivaluedMap headerMap = Mockito.mock(MultivaluedMap.class);
        Mockito.when(headerMap.get(HttpHeaders.CONTENT_TYPE)).thenReturn(Collections.singletonList(FHIRMediaTypes.FHIR_JSON));
        final ContainerRequestContext request = mockRequest();
        setAcceptHeader(request, FHIRMediaTypes.FHIR_JSON);
        Mockito.when(request.getHeaders()).thenReturn(headerMap);

        assertDoesNotThrow(() -> filter.filter(request));
    }

    @Test
    void testMissingAcceptsHeaderForExport() throws URISyntaxException {
        final MultivaluedMap headerMap = Mockito.mock(MultivaluedMap.class);
        Mockito.when(headerMap.get(HttpHeaders.CONTENT_TYPE)).thenReturn(Collections.singletonList(FHIRMediaTypes.FHIR_JSON));
        final ContainerRequestContext request = mockRequest();
        Mockito.when(request.getAcceptableMediaTypes()).thenReturn(null);
        Mockito.when(request.getHeaders()).thenReturn(headerMap);

        final WebApplicationException exception = assertThrows(WebApplicationException.class, () -> filter.filter(request));
        assertEquals(Response.SC_BAD_REQUEST, exception.getResponse().getStatus(), "Should have 400 error");
    }

    @Test
    void testIncorrectAcceptsHeaderForExport() throws URISyntaxException {
        final MultivaluedMap headerMap = Mockito.mock(MultivaluedMap.class);
        Mockito.when(headerMap.get(HttpHeaders.CONTENT_TYPE)).thenReturn(Collections.singletonList(FHIRMediaTypes.FHIR_JSON));
        final ContainerRequestContext request = mockRequest();
        setAcceptHeader(request, MediaType.APPLICATION_JSON);
        Mockito.when(request.getHeaders()).thenReturn(headerMap);

        final WebApplicationException exception = assertThrows(WebApplicationException.class, () -> filter.filter(request));
        assertEquals(Response.SC_UNSUPPORTED_MEDIA_TYPE, exception.getResponse().getStatus(), "Should have 415 error");
    }

    @Test
    void testWildcardAcceptsHeaderForExport() throws URISyntaxException {
        final MultivaluedMap headerMap = Mockito.mock(MultivaluedMap.class);
        Mockito.when(headerMap.get(HttpHeaders.CONTENT_TYPE)).thenReturn(Collections.singletonList(FHIRMediaTypes.FHIR_JSON));
        final ContainerRequestContext request = mockRequest();
        setAcceptHeader(request, MediaType.WILDCARD);
        Mockito.when(request.getHeaders()).thenReturn(headerMap);

        final WebApplicationException exception = assertThrows(WebApplicationException.class, () -> filter.filter(request));
        assertEquals(Response.SC_UNSUPPORTED_MEDIA_TYPE, exception.getResponse().getStatus(), "Should have 415 error");
    }

    @Test
    void testNestedAcceptsHeader() throws URISyntaxException {
        final MultivaluedMap headerMap = Mockito.mock(MultivaluedMap.class);
        Mockito.when(headerMap.get(HttpHeaders.CONTENT_TYPE)).thenReturn(Collections.singletonList(FHIRMediaTypes.FHIR_JSON));
        final ContainerRequestContext request = mockRequest();
        Mockito.when(request.getHeaderString(HttpHeaders.ACCEPT)).thenReturn(FHIRMediaTypes.FHIR_JSON);
        Mockito.when(request.getAcceptableMediaTypes()).thenReturn(List.of(MediaType.APPLICATION_JSON_TYPE, MediaType.valueOf(FHIRMediaTypes.FHIR_JSON)));
        Mockito.when(request.getHeaders()).thenReturn(headerMap);

        assertDoesNotThrow(() -> filter.filter(request));
    }

    @Test
    void testNullAcceptsHeader() throws URISyntaxException {
        final MultivaluedMap headerMap = Mockito.mock(MultivaluedMap.class);
        Mockito.when(headerMap.get(HttpHeaders.CONTENT_TYPE)).thenReturn(Collections.singletonList(FHIRMediaTypes.FHIR_JSON));
        final ContainerRequestContext request = mockRequest();
        Mockito.when(request.getHeaderString(HttpHeaders.ACCEPT)).thenReturn(null);
        Mockito.when(request.getAcceptableMediaTypes()).thenReturn(null);
        Mockito.when(request.getHeaders()).thenReturn(headerMap);

        final WebApplicationException exception = assertThrows(WebApplicationException.class, () -> filter.filter(request));
        assertEquals(Response.SC_BAD_REQUEST, exception.getResponse().getStatus(), "Should have 400 error");
    }

    @Test
    void testIncorrectContentHeader() throws URISyntaxException {
        final MultivaluedMap headerMap = Mockito.mock(MultivaluedMap.class);
        Mockito.when(headerMap.get(HttpHeaders.CONTENT_TYPE)).thenReturn(Collections.singletonList("application/fire+json"));
        final ContainerRequestContext request = mockRequest();
        setAcceptHeader(request, FHIRMediaTypes.FHIR_JSON);
        Mockito.when(request.getHeaders()).thenReturn(headerMap);

        final WebApplicationException exception = assertThrows(WebApplicationException.class, () -> filter.filter(request));
        assertEquals(Response.SC_UNSUPPORTED_MEDIA_TYPE, exception.getResponse().getStatus(), "Should have 415 error");
    }

    @Test
    void testNullContentHeader() throws URISyntaxException {
        final MultivaluedMap headerMap = Mockito.mock(MultivaluedMap.class);
        Mockito.when(headerMap.get(HttpHeaders.CONTENT_TYPE)).thenReturn(null);
        final ContainerRequestContext request = mockRequest();
        setAcceptHeader(request, FHIRMediaTypes.FHIR_JSON);
        Mockito.when(request.getHeaders()).thenReturn(headerMap);

        assertDoesNotThrow(() -> filter.filter(request));
    }

    @Test
    void testTestedContentHeader() throws URISyntaxException {
        final MultivaluedMap headerMap = Mockito.mock(MultivaluedMap.class);
        Mockito.when(headerMap.get(HttpHeaders.CONTENT_TYPE)).thenReturn(List.of("application/fire+json", FHIRMediaTypes.FHIR_JSON));
        final ContainerRequestContext request = mockRequest();
        setAcceptHeader(request, FHIRMediaTypes.FHIR_JSON);
        Mockito.when(request.getHeaders()).thenReturn(headerMap);

        assertDoesNotThrow(() -> filter.filter(request));

    }

    private ContainerRequestContext mockRequest() throws URISyntaxException {
        ContainerRequestContext request = Mockito.mock(ContainerRequestContext.class);
        UriInfo uriInfo = Mockito.mock(UriInfo.class);
        Mockito.when(uriInfo.getRequestUri()).thenReturn(new URI("http://localhost:3002/v1/Group/1234567890/$export?_type=Patient"));
        Mockito.when(request.getUriInfo()).thenReturn(uriInfo);
        return request;
    }

    private void setAcceptHeader(ContainerRequestContext request, String headerString) {
        Mockito.when(request.getHeaderString(HttpHeaders.ACCEPT)).thenReturn(headerString);
        Mockito.when(request.getAcceptableMediaTypes()).thenReturn(Collections.singletonList(MediaType.valueOf(headerString)));
    }
}
