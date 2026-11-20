package gov.cms.dpc.api.auth.filters;

import gov.cms.dpc.api.auth.DPCAuthCredentials;
import gov.cms.dpc.api.auth.DPCUnauthorizedHandler;
import gov.cms.dpc.api.auth.annotations.PathAuthorizer;
import gov.cms.dpc.api.auth.macaroonauth.r4.MacaroonsAuthenticator;
import gov.cms.dpc.api.jdbi.TokenDAO;
import gov.cms.dpc.macaroons.MacaroonBakery;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.PathSegment;
import jakarta.ws.rs.core.UriInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PathAuthorizationFilterTest {

    // This just makes sure our new PathAuthorizationFilter can differentiate between v1 and v3 so it knows which
    // authenticator to call (stu3 or r4 based).
    // TODO: Remove this class when we remove v1 and only have a single authenticator
    @ParameterizedTest
    @ValueSource(strings = {"v1", "v3"})
    void extractsVersionFromMatchedPath(String version) {
        PathAuthorizer annotation = mock(PathAuthorizer.class);
        when(annotation.pathParam()).thenReturn("providerID");
        UriInfo uriInfo = mock(UriInfo.class);
        PathSegment segment = mock(PathSegment.class);
        when(segment.getPath()).thenReturn(version);
        when(uriInfo.getPathSegments()).thenReturn(List.of(segment));
        MultivaluedHashMap<String, String> pathParameters = new MultivaluedHashMap<>();
        pathParameters.add("providerID", "test-id");
        when(uriInfo.getPathParameters()).thenReturn(pathParameters);
        PathAuthorizationFilter filter = new PathAuthorizationFilter(mock(MacaroonBakery.class),
                mock(MacaroonsAuthenticator.class), mock(TokenDAO.class), annotation,
                mock(DPCUnauthorizedHandler.class));

        DPCAuthCredentials credentials = filter.buildCredentials("token", UUID.randomUUID(), uriInfo);

        assertEquals(version, credentials.getApiVersion());
        assertEquals("test-id", credentials.getPathValue());
        assertSame(annotation, credentials.getPathAuthorizer());
    }
}
