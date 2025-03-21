package gov.cms.dpc.api.auth;

import com.github.nitram509.jmacaroons.Macaroon;
import gov.cms.dpc.api.auth.filters.AdminAuthFilter;
import gov.cms.dpc.macaroons.MacaroonBakery;
import gov.cms.dpc.macaroons.MacaroonCaveat;
import gov.cms.dpc.macaroons.MacaroonCondition;
import gov.cms.dpc.macaroons.exceptions.BakeryException;
import gov.cms.dpc.macaroons.store.MemoryRootKeyStore;
import gov.cms.dpc.macaroons.thirdparty.MemoryThirdPartyKeyStore;
import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.Authenticator;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MultivaluedMap;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@SuppressWarnings("unchecked")
class AdminAuthFilterTests {

    private final MacaroonBakery bakery;
    private final AdminAuthFilter filter;
    private final Authenticator<DPCAuthCredentials, OrganizationPrincipal> authenticator;

    AdminAuthFilterTests() {
        this.bakery = new MacaroonBakery.MacaroonBakeryBuilder("http://test.local", new MemoryRootKeyStore(new SecureRandom()), new MemoryThirdPartyKeyStore()).build();
        authenticator = Mockito.mock(Authenticator.class);
        this.filter = new AdminAuthFilter(bakery, authenticator);
    }

    @BeforeEach()
    void resetMocks() {
        Mockito.reset(authenticator);
    }

    @Test
    void ensureGoldenMacaroonPasses() throws AuthenticationException {
        // Create Golden Macaroon
        final Macaroon m1 = bakery.createMacaroon(Collections.emptyList());
        final String macaroonValue = new String(this.bakery.serializeMacaroon(m1, true), StandardCharsets.UTF_8);

        // Mock the request context and pass it through
        final ContainerRequestContext request = mock(ContainerRequestContext.class);
        final MultivaluedMap<String, String> headers = mock(MultivaluedMap.class);
        Mockito.when(request.getHeaders()).thenReturn(headers);
        Mockito.when(headers.getFirst(HttpHeaders.AUTHORIZATION)).thenReturn(String.format("Bearer %s", macaroonValue));

        this.filter.filter(request);
        Mockito.verify(authenticator, times(1)).authenticate(Mockito.any());
    }

    @Test
    void ensureInvalidMacaroonRejected() {
        ContainerRequestContext request = mock(ContainerRequestContext.class);
        MultivaluedMap<String, String> headers = mock(MultivaluedMap.class);
        Mockito.when(request.getHeaders()).thenReturn(headers);
        Mockito.when(headers.getFirst(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer ");
        String errMsg = "Cannot deserialize empty string";

        try (MockedStatic<MacaroonBakery> mockedStatic = mockStatic(MacaroonBakery.class)) {
            mockedStatic.when(() -> MacaroonBakery.deserializeMacaroon(anyString())).thenThrow(new BakeryException(errMsg));
            WebApplicationException exception = assertThrows(WebApplicationException.class, () -> this.filter.filter(request));
            assertEquals(HttpStatus.UNAUTHORIZED_401, exception.getResponse().getStatus());
        }
    }

    @Test
    void ensureOrganizationTokenRejected() {
        // Create Golden Macaroon
        final Macaroon m1 = bakery.createMacaroon(Collections.singletonList(new MacaroonCaveat(new MacaroonCondition("organization_id", MacaroonCondition.Operator.EQ, "1234"))));
        final String macaroonValue = new String(this.bakery.serializeMacaroon(m1, true), StandardCharsets.UTF_8);

        // Mock the request context and pass it through
        final ContainerRequestContext request = mock(ContainerRequestContext.class);
        final MultivaluedMap<String, String> headers = mock(MultivaluedMap.class);
        Mockito.when(request.getHeaders()).thenReturn(headers);
        Mockito.when(headers.getFirst(HttpHeaders.AUTHORIZATION)).thenReturn(String.format("Bearer %s", macaroonValue));

        final WebApplicationException ex = assertThrows(WebApplicationException.class, () -> this.filter.filter(request), "Should have thrown exception");
        assertEquals(HttpStatus.UNAUTHORIZED_401, ex.getResponse().getStatus(), "Should be unauthorized");
        Mockito.verifyNoInteractions(authenticator);
    }
}
