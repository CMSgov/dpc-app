package gov.cms.dpc.api.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import static gov.cms.dpc.api.auth.MacaroonHelpers.TOKEN_URI_PARAM;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

@SuppressWarnings("InnerClassMayBeStatic")
class MacaroonHelpersTest {

    MacaroonHelpersTest() {
        // Not used
    }

    @SuppressWarnings("unchecked")
    @Nested
    @DisplayName(value = "Macaroon Extraction")
    class MacaroonTests {

        @Test
        void getMacaroonFromHeader() {
            final ContainerRequestContext request = mock(ContainerRequestContext.class);
            final MultivaluedMap headers = mock(MultivaluedMap.class);
            Mockito.when(request.getHeaders()).thenReturn(headers);
            final String macaroonValue = "this is a macaroon";
            Mockito.when(headers.getFirst(HttpHeaders.AUTHORIZATION)).thenReturn(String.format("Bearer %s", macaroonValue));

            final Response response = Response.ok().build();
            assertEquals(macaroonValue, MacaroonHelpers.extractMacaroonFromRequest(request, response), "Should have Macaroon from header");
        }

        @Test
        void getMacaroonFromQueryParam() {
            final ContainerRequestContext request = mock(ContainerRequestContext.class);
            final MultivaluedMap headers = mock(MultivaluedMap.class);
            Mockito.when(request.getHeaders()).thenReturn(headers);
            final String macaroonValue = "this is a macaroon";
            Mockito.when(headers.getFirst(TOKEN_URI_PARAM)).thenReturn(macaroonValue);
            Mockito.when(headers.getFirst(HttpHeaders.AUTHORIZATION)).thenReturn(null);

            final UriInfo uriMock = mock(UriInfo.class);
            Mockito.when(uriMock.getQueryParameters()).thenReturn(headers);
            Mockito.when(request.getUriInfo()).thenReturn(uriMock);

            final Response response = Response.ok().build();
            assertEquals(macaroonValue, MacaroonHelpers.extractMacaroonFromRequest(request, response), "Should have Macaroon from header");
        }

        @Test
        void getNoMacaroon() {
            final ContainerRequestContext request = mock(ContainerRequestContext.class);
            final MultivaluedMap headers = mock(MultivaluedMap.class);
            Mockito.when(request.getHeaders()).thenReturn(headers);
            Mockito.when(headers.getFirst(TOKEN_URI_PARAM)).thenReturn(null);
            Mockito.when(headers.getFirst(HttpHeaders.AUTHORIZATION)).thenReturn(null);

            final UriInfo uriMock = mock(UriInfo.class);
            Mockito.when(uriMock.getQueryParameters()).thenReturn(headers);
            Mockito.when(request.getUriInfo()).thenReturn(uriMock);

            final Response response = Response.ok().build();
            assertThrows(WebApplicationException.class, () -> MacaroonHelpers.extractMacaroonFromRequest(request, response), "Should not have Macaroon");
        }

        @Test
        void ensureHeaderPriority() {
            final ContainerRequestContext request = mock(ContainerRequestContext.class);
            final MultivaluedMap headers = mock(MultivaluedMap.class);
            Mockito.when(request.getHeaders()).thenReturn(headers);
            final String macaroonValue = "this is a macaroon";
            Mockito.when(headers.getFirst(TOKEN_URI_PARAM)).thenReturn("this is from the query param");
            Mockito.when(headers.getFirst(HttpHeaders.AUTHORIZATION)).thenReturn(String.format("Bearer %s", macaroonValue));

            final UriInfo uriMock = mock(UriInfo.class);
            Mockito.when(uriMock.getQueryParameters()).thenReturn(headers);
            Mockito.when(request.getUriInfo()).thenReturn(uriMock);

            final Response response = Response.ok().build();
            assertEquals(macaroonValue, MacaroonHelpers.extractMacaroonFromRequest(request, response), "Should have Macaroon from header");
        }

        @Test
        void testBlankMacaroonHeader() {
            final ContainerRequestContext request = mock(ContainerRequestContext.class);
            final MultivaluedMap headers = mock(MultivaluedMap.class);
            Mockito.when(request.getHeaders()).thenReturn(headers);
            final String macaroonValue = "";
            Mockito.when(headers.getFirst(TOKEN_URI_PARAM)).thenReturn("this is from the query param");
            Mockito.when(headers.getFirst(HttpHeaders.AUTHORIZATION)).thenReturn(String.format("Bearer %s", macaroonValue));

            final UriInfo uriMock = mock(UriInfo.class);
            Mockito.when(uriMock.getQueryParameters()).thenReturn(headers);
            Mockito.when(request.getUriInfo()).thenReturn(uriMock);

            final Response response = Response.ok().build();
            assertEquals(macaroonValue, MacaroonHelpers.extractMacaroonFromRequest(request, response), "Should have Macaroon from header");
        }

        @Test
        void testBlankMacaroonQueryParam() {
            final ContainerRequestContext request = mock(ContainerRequestContext.class);
            final MultivaluedMap headers = mock(MultivaluedMap.class);
            Mockito.when(request.getHeaders()).thenReturn(headers);
            final String macaroonValue = "";
            Mockito.when(headers.getFirst(TOKEN_URI_PARAM)).thenReturn(macaroonValue);
            Mockito.when(headers.getFirst(HttpHeaders.AUTHORIZATION)).thenReturn(null);

            final UriInfo uriMock = mock(UriInfo.class);
            Mockito.when(uriMock.getQueryParameters()).thenReturn(headers);
            Mockito.when(request.getUriInfo()).thenReturn(uriMock);

            final Response response = Response.ok().build();
            assertEquals(macaroonValue, MacaroonHelpers.extractMacaroonFromRequest(request, response), "Should have Macaroon from query param");
        }

        @Test
        void testHeaderNoSpace() {
            final ContainerRequestContext request = mock(ContainerRequestContext.class);
            final MultivaluedMap headers = mock(MultivaluedMap.class);
            Mockito.when(request.getHeaders()).thenReturn(headers);
            final String macaroonValue = "this is a macaroon";
            Mockito.when(headers.getFirst(TOKEN_URI_PARAM)).thenReturn(null);
            Mockito.when(headers.getFirst(HttpHeaders.AUTHORIZATION)).thenReturn(String.format("Bearer%s", macaroonValue));

            final UriInfo uriMock = mock(UriInfo.class);
            Mockito.when(uriMock.getQueryParameters()).thenReturn(headers);
            Mockito.when(request.getUriInfo()).thenReturn(uriMock);

            final Response response = Response.ok().build();
            assertThrows(WebApplicationException.class, () -> MacaroonHelpers.extractMacaroonFromRequest(request, response), "Should not have Macaroon");
        }

        @Test
        void testHeaderNoBearer() {
            final ContainerRequestContext request = mock(ContainerRequestContext.class);
            final MultivaluedMap headers = mock(MultivaluedMap.class);
            Mockito.when(request.getHeaders()).thenReturn(headers);
            final String macaroonValue = "this is a macaroon";
            Mockito.when(headers.getFirst(TOKEN_URI_PARAM)).thenReturn(null);
            Mockito.when(headers.getFirst(HttpHeaders.AUTHORIZATION)).thenReturn(String.format("%s", macaroonValue));

            final UriInfo uriMock = mock(UriInfo.class);
            Mockito.when(uriMock.getQueryParameters()).thenReturn(headers);
            Mockito.when(request.getUriInfo()).thenReturn(uriMock);

            final Response response = Response.ok().build();
            assertThrows(WebApplicationException.class, () -> MacaroonHelpers.extractMacaroonFromRequest(request, response), "Should not have Macaroon");
        }
    }
}
