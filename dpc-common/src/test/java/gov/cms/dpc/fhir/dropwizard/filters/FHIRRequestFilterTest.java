package gov.cms.dpc.fhir.dropwizard.filters;

import gov.cms.dpc.fhir.FHIRMediaTypes;
import org.eclipse.jetty.server.Response;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SuppressWarnings({"rawtypes", "unchecked"})
public class FHIRRequestFilterTest {

    private static final FHIRRequestFilter filter = new FHIRRequestFilter();

    @Test
    void testSuccess() {
        final MultivaluedMap headerMap = Mockito.mock(MultivaluedMap.class);
        Mockito.when(headerMap.get(HttpHeaders.CONTENT_TYPE)).thenReturn(Collections.singletonList(FHIRMediaTypes.FHIR_JSON));
        final ContainerRequestContext request = Mockito.mock(ContainerRequestContext.class);
        Mockito.when(request.getAcceptableMediaTypes()).thenReturn(Collections.singletonList(MediaType.valueOf(FHIRMediaTypes.FHIR_JSON)));
        Mockito.when(request.getHeaders()).thenReturn(headerMap);

        filter.filter(request);
    }

    @Test
    void testMissingAcceptsHeader() {
        final ContainerRequestContext request = Mockito.mock(ContainerRequestContext.class);
        Mockito.when(request.getAcceptableMediaTypes()).thenReturn(Collections.singletonList(MediaType.APPLICATION_JSON_TYPE));

        final WebApplicationException exception = assertThrows(WebApplicationException.class, () -> filter.filter(request));
        assertEquals(Response.SC_UNSUPPORTED_MEDIA_TYPE, exception.getResponse().getStatus(), "Should have 415 error");
    }

    @Test
    void testNestedAcceptsHeader() {
        final MultivaluedMap headerMap = Mockito.mock(MultivaluedMap.class);
        Mockito.when(headerMap.get(HttpHeaders.CONTENT_TYPE)).thenReturn(Collections.singletonList(FHIRMediaTypes.FHIR_JSON));
        final ContainerRequestContext request = Mockito.mock(ContainerRequestContext.class);
        Mockito.when(request.getAcceptableMediaTypes()).thenReturn(List.of(MediaType.APPLICATION_JSON_TYPE, MediaType.valueOf(FHIRMediaTypes.FHIR_JSON)));
        Mockito.when(request.getHeaders()).thenReturn(headerMap);

        filter.filter(request);
    }

    @Test
    void testNullAcceptsHeader() {
        final ContainerRequestContext request = Mockito.mock(ContainerRequestContext.class);
        Mockito.when(request.getAcceptableMediaTypes()).thenReturn(null);

        final WebApplicationException exception = assertThrows(WebApplicationException.class, () -> filter.filter(request));
        assertEquals(Response.SC_UNSUPPORTED_MEDIA_TYPE, exception.getResponse().getStatus(), "Should have 415 error");
    }

    @Test
    void testIncorrectContentHeader() {
        final MultivaluedMap headerMap = Mockito.mock(MultivaluedMap.class);
        Mockito.when(headerMap.get(HttpHeaders.CONTENT_TYPE)).thenReturn(Collections.singletonList("application/fire+json"));
        final ContainerRequestContext request = Mockito.mock(ContainerRequestContext.class);
        Mockito.when(request.getAcceptableMediaTypes()).thenReturn(Collections.singletonList(MediaType.valueOf(FHIRMediaTypes.FHIR_JSON)));
        Mockito.when(request.getHeaders()).thenReturn(headerMap);

        final WebApplicationException exception = assertThrows(WebApplicationException.class, () -> filter.filter(request));
        assertEquals(Response.SC_UNSUPPORTED_MEDIA_TYPE, exception.getResponse().getStatus(), "Should have 415 error");
    }

    @Test

    void testNullContentHeader() {
        final MultivaluedMap headerMap = Mockito.mock(MultivaluedMap.class);
        Mockito.when(headerMap.get(HttpHeaders.CONTENT_TYPE)).thenReturn(null);
        final ContainerRequestContext request = Mockito.mock(ContainerRequestContext.class);
        Mockito.when(request.getAcceptableMediaTypes()).thenReturn(Collections.singletonList(MediaType.valueOf(FHIRMediaTypes.FHIR_JSON)));
        Mockito.when(request.getHeaders()).thenReturn(headerMap);

        filter.filter(request);
    }

    @Test
    void testTestedContentHeader() {
        final MultivaluedMap headerMap = Mockito.mock(MultivaluedMap.class);
        Mockito.when(headerMap.get(HttpHeaders.CONTENT_TYPE)).thenReturn(List.of("application/fire+json", FHIRMediaTypes.FHIR_JSON));
        final ContainerRequestContext request = Mockito.mock(ContainerRequestContext.class);
        Mockito.when(request.getAcceptableMediaTypes()).thenReturn(Collections.singletonList(MediaType.valueOf(FHIRMediaTypes.FHIR_JSON)));
        Mockito.when(request.getHeaders()).thenReturn(headerMap);

        filter.filter(request);

    }
}
