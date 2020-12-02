package gov.cms.dpc.api.auth;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.gclient.IUntypedQuery;
import gov.cms.dpc.api.APITestHelpers;
import gov.cms.dpc.api.auth.macaroonauth.MacaroonsAuthenticator;
import gov.cms.dpc.api.core.Capabilities;
import gov.cms.dpc.api.jdbi.TokenDAO;
import gov.cms.dpc.api.resources.v1.BaseResource;
import gov.cms.dpc.api.resources.v1.OrganizationResource;
import gov.cms.dpc.fhir.FHIRMediaTypes;
import gov.cms.dpc.testing.BufferedLoggerHandler;
import gov.cms.dpc.macaroons.MacaroonBakery;
import gov.cms.dpc.macaroons.store.MemoryRootKeyStore;
import gov.cms.dpc.macaroons.thirdparty.MemoryThirdPartyKeyStore;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import io.dropwizard.testing.junit5.ResourceExtension;
import org.eclipse.jetty.http.HttpStatus;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Organization;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.exceptions.base.MockitoException;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@ExtendWith(DropwizardExtensionsSupport.class)
@ExtendWith(BufferedLoggerHandler.class)
@SuppressWarnings({"unchecked", "rawtypes"})
class AuthHandlerTest {
    private static final String BAD_ORG_ID = "0c527d2e-2e8a-4808-b11d-0fa06baf8252";
    private static final String TEST_MACAROON = "eyJ2IjoyLCJsIjoiaHR0cHM6Ly9kcGMuY21zLmdvdiIsImkiOiI3YzRhMzk1NS03ZWRjLTRjOWUtOGRjYS0wZjdjMjcwNzIwNzQiLCJjIjpbeyJpNjQiOiJaSEJqWDIxaFkyRnliMjl1WDNabGNuTnBiMjRnUFNBeCJ9LHsiaTY0IjoiWlhod2FYSmxjeUE5SURJd01qQXRNRGN0TVRCVU1UUTZNVGM2TXpNdU9EYzJOalF6V2cifSx7Imk2NCI6ImIzSm5ZVzVwZW1GMGFXOXVYMmxrSUQwZ01HTTFNamRrTW1VdE1tVTRZUzAwT0RBNExXSXhNV1F0TUdaaE1EWmlZV1k0TWpVMCJ9XSwiczY0Ijoic0ZvSlFGNGk5VHZuSnRHVEhUb1ZFblJwc3hzZmdJZjhDdWtpYy0xWE14ZyJ9";

    private static final ArgumentCaptor<Map<String, List<String>>> requestPath = ArgumentCaptor.forClass(Map.class);
    private static final ResourceExtension RESOURCE = buildAuthResource();
    private static IGenericClient client;
    private static IUntypedQuery untypedQuery;
    private static IQuery query;

    private AuthHandlerTest() {
        // Not used
    }

    @Test
    void testNoToken() {
        final Response response = RESOURCE.target("/v1/Organization/" + APITestHelpers.ORGANIZATION_ID)
                .request(FHIRMediaTypes.FHIR_JSON)
                .get();

        assertEquals(HttpStatus.UNAUTHORIZED_401, response.getStatus(), "Should be unauthorized");
    }

    @Test
    void testMalformedHeader() {
        final Response response = RESOURCE.target("/v1/Organization/" + BAD_ORG_ID)
                .request(FHIRMediaTypes.FHIR_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer" + TEST_MACAROON)
                .get();

        assertEquals(HttpStatus.UNAUTHORIZED_401, response.getStatus(), "Should not authorized for other organization");
    }

//    @Test
//    void testPublicAPI() {
//        final Response response = RESOURCE.target("/v1/metadata")
//                .request(FHIRMediaTypes.FHIR_JSON)
//                .get();
//
//        assertEquals(HttpStatus.OK_200, response.getStatus(), "Should be authorized");
//    }

    private static ResourceExtension buildAuthResource() {
        // Setup mocks
        final IGenericClient client = mockGenericClient();
        final MacaroonBakery bakery = buildBakery();
        final TokenDAO sessionFactory = mock(TokenDAO.class);
        final DPCUnauthorizedHandler dpc401handler = mock(DPCUnauthorizedHandler.class);
        Mockito.when(sessionFactory.fetchTokens(Mockito.any())).thenAnswer(answer -> "46ac7ad6-7487-4dd0-baa0-6e2c8cae76a0");


        final DPCAuthFactory factory = new DPCAuthFactory(bakery, new MacaroonsAuthenticator(client), sessionFactory, dpc401handler);
        final DPCAuthDynamicFeature dynamicFeature = new DPCAuthDynamicFeature(factory);

        final FhirContext ctx = FhirContext.forDstu3();

        return APITestHelpers.buildResourceExtension(ctx, List.of(mockOrganizationResource(), mockBaseResource()), List.of(dynamicFeature), false);
    }

    private static OrganizationResource mockOrganizationResource() {
        final OrganizationResource organizationResource = mock(OrganizationResource.class);

        doReturn(new Organization()).when(organizationResource).getOrganization(Mockito.any(UUID.class));

        return organizationResource;
    }

    private static BaseResource mockBaseResource() {
        final BaseResource base = mock(BaseResource.class);

        doReturn(Capabilities.getCapabilities("https://sandbox.dpc.cms.gov/api/v1")).when(base).metadata();

        return base;
    }

    private static MacaroonBakery buildBakery() {
        return new MacaroonBakery.MacaroonBakeryBuilder("http://test.local",
                new MemoryRootKeyStore(new SecureRandom()),
                new MemoryThirdPartyKeyStore()).build();
    }

    private static IGenericClient mockGenericClient() {
        client = mock(IGenericClient.class);
        untypedQuery = mock(IUntypedQuery.class);
        query = mock(IQuery.class);

        Mockito.when(client.search()).thenReturn(untypedQuery);
        Mockito.when(untypedQuery.forResource(Organization.class)).thenReturn(query);
        Mockito.when(untypedQuery.forResource(Mockito.anyString())).thenReturn(query);
        Mockito.when(query.withTag(Mockito.anyString(), Mockito.anyString())).thenReturn(query);
        Mockito.when(query.whereMap(requestPath.capture())).thenReturn(query);
        Mockito.when(query.returnBundle(Bundle.class)).thenReturn(query);
        Mockito.when(query.encodedJson()).thenReturn(query);
        Mockito.when(query.execute()).thenAnswer(answer -> {
            final Organization org = new Organization();
            org.setId(new IdType("Organization", APITestHelpers.ORGANIZATION_ID));
            final Bundle bundle = new Bundle();
            bundle.addEntry().setResource(org);
            bundle.setTotal(1);

            try {
                final Map<String, List<String>> value = requestPath.getValue();
                final String identifier = value.get("identifier").get(0);

                if (identifier.equals(BAD_ORG_ID)) {
                    return new Bundle();
                }

            } catch (MockitoException e) {
                // Ignore it
            }
            return bundle;
        });

        return client;
    }
}
