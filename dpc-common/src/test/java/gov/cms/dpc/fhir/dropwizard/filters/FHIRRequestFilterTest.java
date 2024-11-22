package gov.cms.dpc.fhir.dropwizard.filters;

import gov.cms.dpc.fhir.FHIRMediaTypes;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotSupportedException;
import org.eclipse.jetty.server.Response;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.UriInfo;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.DisplayName;

@SuppressWarnings({"rawtypes", "unchecked"})
@DisplayName("FHIR request filtering")
public class FHIRRequestFilterTest {

    private static final FHIRRequestFilter filter = new FHIRRequestFilter();

    @Test
    @DisplayName("Accept FHIR request 🥳")
    void testSuccess() throws URISyntaxException {
        final MultivaluedMap headerMap = Mockito.mock(MultivaluedMap.class);
        Mockito.when(headerMap.get(HttpHeaders.CONTENT_TYPE)).thenReturn(Collections.singletonList(FHIRMediaTypes.FHIR_JSON));
        final ContainerRequestContext request = mockRequest();
        setAcceptHeader(request, FHIRMediaTypes.FHIR_JSON);
        Mockito.when(request.getHeaders()).thenReturn(headerMap);

        filter.filter(request);
    }

    @Test
    @DisplayName("Filter FHIR request with missing header 🤮")
    void testMissingAcceptsHeaderForExport() throws URISyntaxException {
        final MultivaluedMap headerMap = Mockito.mock(MultivaluedMap.class);
        Mockito.when(headerMap.get(HttpHeaders.CONTENT_TYPE)).thenReturn(Collections.singletonList(FHIRMediaTypes.FHIR_JSON));
        final ContainerRequestContext request = mockRequest();
        Mockito.when(request.getAcceptableMediaTypes()).thenReturn(null);
        Mockito.when(request.getHeaders()).thenReturn(headerMap);

        final BadRequestException exception = assertThrows(BadRequestException.class, () -> filter.filter(request));
        assertEquals(Response.SC_BAD_REQUEST, exception.getResponse().getStatus(), "Should have 400 error");
    }

    @Test
    @DisplayName("Filter FHIR request with invalid header 🤮")
    void testIncorrectAcceptsHeaderForExport() throws URISyntaxException {
        final MultivaluedMap headerMap = Mockito.mock(MultivaluedMap.class);
        Mockito.when(headerMap.get(HttpHeaders.CONTENT_TYPE)).thenReturn(Collections.singletonList(FHIRMediaTypes.FHIR_JSON));
        final ContainerRequestContext request = mockRequest();
        setAcceptHeader(request, MediaType.APPLICATION_JSON);
        Mockito.when(request.getHeaders()).thenReturn(headerMap);

        final NotSupportedException exception = assertThrows(NotSupportedException.class, () -> filter.filter(request));
        assertEquals(Response.SC_UNSUPPORTED_MEDIA_TYPE, exception.getResponse().getStatus(), "Should have 415 error");
    }

    @Test
    @DisplayName("Filter FHIR request with unsupported header 🤮")
    void testWildcardAcceptsHeaderForExport() throws URISyntaxException {
        final MultivaluedMap headerMap = Mockito.mock(MultivaluedMap.class);
        Mockito.when(headerMap.get(HttpHeaders.CONTENT_TYPE)).thenReturn(Collections.singletonList(FHIRMediaTypes.FHIR_JSON));
        final ContainerRequestContext request = mockRequest();
        setAcceptHeader(request, MediaType.WILDCARD);
        Mockito.when(request.getHeaders()).thenReturn(headerMap);

        final NotSupportedException exception = assertThrows(NotSupportedException.class, () -> filter.filter(request));
        assertEquals(Response.SC_UNSUPPORTED_MEDIA_TYPE, exception.getResponse().getStatus(), "Should have 415 error");
    }

    @Test
    @DisplayName("Accept FHIR request with nested header value 🥳")
    void testNestedAcceptsHeader() throws URISyntaxException {
        final MultivaluedMap headerMap = Mockito.mock(MultivaluedMap.class);
        Mockito.when(headerMap.get(HttpHeaders.CONTENT_TYPE)).thenReturn(Collections.singletonList(FHIRMediaTypes.FHIR_JSON));
        final ContainerRequestContext request = mockRequest();
        Mockito.when(request.getHeaderString(HttpHeaders.ACCEPT)).thenReturn(FHIRMediaTypes.FHIR_JSON);
        Mockito.when(request.getAcceptableMediaTypes()).thenReturn(List.of(MediaType.APPLICATION_JSON_TYPE, MediaType.valueOf(FHIRMediaTypes.FHIR_JSON)));
        Mockito.when(request.getHeaders()).thenReturn(headerMap);

        filter.filter(request);
    }

    @Test
    @DisplayName("Filter FHIR request with null header value 🤮")
    void testNullAcceptsHeader() throws URISyntaxException {
        final MultivaluedMap headerMap = Mockito.mock(MultivaluedMap.class);
        Mockito.when(headerMap.get(HttpHeaders.CONTENT_TYPE)).thenReturn(Collections.singletonList(FHIRMediaTypes.FHIR_JSON));
        final ContainerRequestContext request = mockRequest();
        Mockito.when(request.getHeaderString(HttpHeaders.ACCEPT)).thenReturn(null);
        Mockito.when(request.getAcceptableMediaTypes()).thenReturn(null);
        Mockito.when(request.getHeaders()).thenReturn(headerMap);

        final BadRequestException exception = assertThrows(BadRequestException.class, () -> filter.filter(request));
        assertEquals(Response.SC_BAD_REQUEST, exception.getResponse().getStatus(), "Should have 400 error");
    }

    @Test
    @DisplayName("Filter FHIR request with incorrect header 🤮")
    void testIncorrectContentHeader() throws URISyntaxException {
        final MultivaluedMap headerMap = Mockito.mock(MultivaluedMap.class);
        Mockito.when(headerMap.get(HttpHeaders.CONTENT_TYPE)).thenReturn(Collections.singletonList("application/fire+json"));
        final ContainerRequestContext request = mockRequest();
        setAcceptHeader(request, FHIRMediaTypes.FHIR_JSON);
        Mockito.when(request.getHeaders()).thenReturn(headerMap);

        final NotSupportedException exception = assertThrows(NotSupportedException.class, () -> filter.filter(request));
        assertEquals(Response.SC_UNSUPPORTED_MEDIA_TYPE, exception.getResponse().getStatus(), "Should have 415 error");
    }

    @Test
    @DisplayName("Accept FHIR request with null header value 🥳")
    void testNullContentHeader() throws URISyntaxException {
        final MultivaluedMap headerMap = Mockito.mock(MultivaluedMap.class);
        Mockito.when(headerMap.get(HttpHeaders.CONTENT_TYPE)).thenReturn(null);
        final ContainerRequestContext request = mockRequest();
        setAcceptHeader(request, FHIRMediaTypes.FHIR_JSON);
        Mockito.when(request.getHeaders()).thenReturn(headerMap);

        filter.filter(request);
    }

    @Test
    @DisplayName("Accept FHIR request with multi-value header 🥳")
    void testTestedContentHeader() throws URISyntaxException {
        final MultivaluedMap headerMap = Mockito.mock(MultivaluedMap.class);
        Mockito.when(headerMap.get(HttpHeaders.CONTENT_TYPE)).thenReturn(List.of("application/fire+json", FHIRMediaTypes.FHIR_JSON));
        final ContainerRequestContext request = mockRequest();
        setAcceptHeader(request, FHIRMediaTypes.FHIR_JSON);
        Mockito.when(request.getHeaders()).thenReturn(headerMap);

        filter.filter(request);

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
