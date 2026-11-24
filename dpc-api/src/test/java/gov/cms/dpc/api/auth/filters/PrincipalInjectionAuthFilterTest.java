package gov.cms.dpc.api.auth.filters;

import gov.cms.dpc.api.auth.DPCAuthCredentials;
import gov.cms.dpc.api.auth.DPCUnauthorizedHandler;
import gov.cms.dpc.api.jdbi.TokenDAO;
import gov.cms.dpc.macaroons.MacaroonBakery;
import io.dropwizard.auth.Authenticator;
import jakarta.ws.rs.core.PathSegment;
import jakarta.ws.rs.core.UriInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PrincipalInjectionAuthFilterTest {

    @ParameterizedTest
    @ValueSource(strings = {"v1", "v3"})
    void extractsVersionForStandardAuthentication(String version) {
        UriInfo uriInfo = mock(UriInfo.class);
        PathSegment segment = mock(PathSegment.class);
        when(segment.getPath()).thenReturn(version);
        when(uriInfo.getPathSegments()).thenReturn(List.of(segment));
        PrincipalInjectionAuthFilter filter = new PrincipalInjectionAuthFilter(mock(MacaroonBakery.class),
                mockAuthenticator(), mock(TokenDAO.class), mock(DPCUnauthorizedHandler.class));
        UUID organizationId = UUID.randomUUID();

        DPCAuthCredentials credentials = filter.buildCredentials("token", organizationId, uriInfo);

        assertEquals(organizationId.toString(), credentials.getOrganizationId());
        assertEquals(version, credentials.getApiVersion());
        assertNull(credentials.getPathAuthorizer());
    }

    @SuppressWarnings("unchecked")
    private static Authenticator<DPCAuthCredentials, Principal> mockAuthenticator() {
        return mock(Authenticator.class);
    }
}
