package gov.cms.dpc.api.auth;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.github.nitram509.jmacaroons.Macaroon;
import gov.cms.dpc.api.auth.annotations.Authorizer;
import gov.cms.dpc.api.auth.staticauth.StaticAuthFactory;
import gov.cms.dpc.api.auth.staticauth.StaticAuthenticator;
import gov.cms.dpc.api.jdbi.TokenDAO;
import gov.cms.dpc.attribution.client.AttributionClient;
import gov.cms.dpc.macaroons.MacaroonBakery;
import gov.cms.dpc.macaroons.MacaroonCaveat;
import gov.cms.dpc.macaroons.MacaroonCondition;
import gov.cms.dpc.macaroons.store.IDKeyPair;
import gov.cms.dpc.macaroons.store.IRootKeyStore;
import gov.cms.dpc.macaroons.store.MemoryRootKeyStore;
import gov.cms.dpc.macaroons.thirdparty.MemoryThirdPartyKeyStore;
import io.dropwizard.auth.Auth;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import io.dropwizard.testing.junit5.ResourceExtension;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.UUID;

import static gov.cms.dpc.api.auth.staticauth.StaticAuthFilter.DEFAULT_ORG_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// TODO: We can probably get rid of this class when we get rid of /v1 if we're no longer using versioned auth
@ExtendWith(DropwizardExtensionsSupport.class)
class VersionedPrincipalInjectionTest {

    // This is the
    private static final ResourceExtension RESOURCE = ResourceExtension.builder()
            .addResource(new V1Resource())
            .addResource(new V3Resource())
            .addProvider(new DPCAuthDynamicFeature(new StaticAuthFactory(new StaticAuthenticator())))
            .addProvider(new OrganizationAuthValueFactoryProvider.Binder())
            .build();

    private static String clientToken;
    private static final ResourceExtension AUTHENTICATED_RESOURCE = buildAuthenticatedResource();

    // Verify the static filter works across versions when auth is turned off
    @Test
    void injectsTheCorrectPrincipalForEachVersion() {
        assertEquals(DEFAULT_ORG_ID, RESOURCE.target("/v1/principal").request().get(String.class));
        assertEquals(DEFAULT_ORG_ID, RESOURCE.target("/v3/principal").request().get(String.class));
    }

    @Test
    void reusesTheSameOrganizationIdAcrossVersions() {
        String organizationId = UUID.randomUUID().toString();

        assertEquals(organizationId, RESOURCE.target("/v1/principal").request().header("Organization", organizationId).get(String.class));
        assertEquals(organizationId, RESOURCE.target("/v3/principal").request().header("Organization", organizationId).get(String.class));
    }

    @Test
    // Test that we can pull an org from a client token in both versions
    void acceptsTheSameAccessTokenForBothVersions() {
        assertEquals(DEFAULT_ORG_ID, AUTHENTICATED_RESOURCE.target("/v1/principal").request()
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + clientToken).get(String.class));
        assertEquals(DEFAULT_ORG_ID, AUTHENTICATED_RESOURCE.target("/v3/principal").request()
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + clientToken).get(String.class));
    }

    private static ResourceExtension buildAuthenticatedResource() {
        String tokenId = UUID.randomUUID().toString();
        String rootKey = new MemoryRootKeyStore(new SecureRandom()).generateKey();
        IRootKeyStore rootKeys = mock(IRootKeyStore.class);
        when(rootKeys.create()).thenReturn(new IDKeyPair(tokenId, rootKey));
        when(rootKeys.get(tokenId)).thenReturn(rootKey);
        MacaroonBakery bakery = new MacaroonBakery.MacaroonBakeryBuilder("http://test.local",
                rootKeys, new MemoryThirdPartyKeyStore()).build();
        Macaroon macaroon = bakery.createMacaroon(java.util.List.of(new MacaroonCaveat(
                new MacaroonCondition("organization_id", MacaroonCondition.Operator.EQ, DEFAULT_ORG_ID))));
        clientToken = new String(bakery.serializeMacaroon(macaroon, true), StandardCharsets.UTF_8);
        TokenDAO dao = mock(TokenDAO.class);
        when(dao.findOrgByToken(UUID.fromString(macaroon.identifier))).thenReturn(UUID.fromString(DEFAULT_ORG_ID));
        DPCAuthFactory factory = new DPCAuthFactory(bakery,
                new gov.cms.dpc.api.auth.macaroonauth.stu3.MacaroonsAuthenticator(mock(IGenericClient.class)),
                new gov.cms.dpc.api.auth.macaroonauth.r4.MacaroonsAuthenticator(mock(AttributionClient.class)),
                dao, new DPCUnauthorizedHandler());

        return ResourceExtension.builder()
                .addResource(new V1Resource())
                .addResource(new V3Resource())
                .addProvider(new DPCAuthDynamicFeature(factory))
                .addProvider(new OrganizationAuthValueFactoryProvider.Binder())
                .build();
    }


    // Set up a couple /principal endpoints that just return the principal's id
    @Path("/v1/principal")
    public static class V1Resource {
        @GET
        @Authorizer
        @jakarta.ws.rs.Produces(MediaType.TEXT_PLAIN)
        public String get(@Auth OrganizationPrincipal principal) {
            return principal.getID().toString();
        }
    }

    @Path("/v3/principal")
    public static class V3Resource {
        @GET
        @Authorizer
        @jakarta.ws.rs.Produces(MediaType.TEXT_PLAIN)
        public String get(@Auth gov.cms.dpc.api.auth.r4.OrganizationPrincipal principal) {
            return principal.getID().toString();
        }
    }
}
