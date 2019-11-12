package gov.cms.dpc.api.auth;

import com.github.nitram509.jmacaroons.Macaroon;
import gov.cms.dpc.api.auth.filters.AdminAuthFilter;
import gov.cms.dpc.macaroons.MacaroonBakery;
import gov.cms.dpc.macaroons.MacaroonCaveat;
import gov.cms.dpc.macaroons.MacaroonCondition;
import gov.cms.dpc.macaroons.store.MemoryRootKeyStore;
import gov.cms.dpc.macaroons.thirdparty.MemoryThirdPartyKeyStore;
import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.Authenticator;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

@SuppressWarnings("unchecked")
class AdminAuthFilterTests {

    private final MacaroonBakery bakery;
    private final AdminAuthFilter filter;
    private final Authenticator authenticator;

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
        final MultivaluedMap headers = mock(MultivaluedMap.class);
        Mockito.when(request.getHeaders()).thenReturn(headers);
        Mockito.when(headers.getFirst(HttpHeaders.AUTHORIZATION)).thenReturn(String.format("Bearer %s", macaroonValue));

        this.filter.filter(request);
        Mockito.verify(authenticator, times(1)).authenticate(Mockito.any());
    }

    @Test
    void ensureOrganizationTokenRejected() {
        // Create Golden Macaroon
        final Macaroon m1 = bakery.createMacaroon(Collections.singletonList(new MacaroonCaveat(new MacaroonCondition("organization_id", MacaroonCondition.Operator.EQ, "1234"))));
        final String macaroonValue = new String(this.bakery.serializeMacaroon(m1, true), StandardCharsets.UTF_8);

        // Mock the request context and pass it through
        final ContainerRequestContext request = mock(ContainerRequestContext.class);
        final MultivaluedMap headers = mock(MultivaluedMap.class);
        Mockito.when(request.getHeaders()).thenReturn(headers);
        Mockito.when(headers.getFirst(HttpHeaders.AUTHORIZATION)).thenReturn(String.format("Bearer %s", macaroonValue));

        final WebApplicationException ex = assertThrows(WebApplicationException.class, () -> this.filter.filter(request), "Should have thrown exception");
        assertEquals(HttpStatus.UNAUTHORIZED_401, ex.getResponse().getStatus(), "Should be unauthorized");
        Mockito.verifyNoInteractions(authenticator);
    }
}
