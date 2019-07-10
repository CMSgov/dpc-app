package gov.cms.dpc.api.auth;

import gov.cms.dpc.api.APITestHelpers;
import gov.cms.dpc.api.DPCAPIConfiguration;
import gov.cms.dpc.api.core.Capabilities;
import gov.cms.dpc.api.resources.v1.BaseResource;
import gov.cms.dpc.api.resources.v1.OrganizationResource;
import gov.cms.dpc.fhir.FHIRMediaTypes;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import io.dropwizard.testing.junit5.ResourceExtension;
import org.eclipse.jetty.http.HttpStatus;
import org.glassfish.jersey.test.grizzly.GrizzlyWebTestContainerFactory;
import org.hl7.fhir.dstu3.model.Organization;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@ExtendWith(DropwizardExtensionsSupport.class)
public class AuthenticationTest {
    private static final String BAD_ORG_ID = "0c527d2e-2e8a-4808-b11d-0fa06baf8252";
    private static final String TEST_MACAROON = "eyJ2IjoyLCJsIjoiaHR0cHM6Ly9kcGMuY21zLmdvdiIsImkiOiI3YzRhMzk1NS03ZWRjLTRjOWUtOGRjYS0wZjdjMjcwNzIwNzQiLCJjIjpbeyJpNjQiOiJaSEJqWDIxaFkyRnliMjl1WDNabGNuTnBiMjRnUFNBeCJ9LHsiaTY0IjoiWlhod2FYSmxjeUE5SURJd01qQXRNRGN0TVRCVU1UUTZNVGM2TXpNdU9EYzJOalF6V2cifSx7Imk2NCI6ImIzSm5ZVzVwZW1GMGFXOXVYMmxrSUQwZ01HTTFNamRrTW1VdE1tVTRZUzAwT0RBNExXSXhNV1F0TUdaaE1EWmlZV1k0TWpVMCJ9XSwiczY0Ijoic0ZvSlFGNGk5VHZuSnRHVEhUb1ZFblJwc3hzZmdJZjhDdWtpYy0xWE14ZyJ9";

    private static final ArgumentCaptor<String> requestPath = ArgumentCaptor.forClass(String.class);
    private static final ResourceExtension RESOURCE = buildAuthResource();

    private AuthenticationTest() {
        // Not used
    }

    @Test
    void testNoToken() {
        final Response response = RESOURCE.target("/Organization/" + APITestHelpers.ORGANIZATION_ID)
                .request(FHIRMediaTypes.FHIR_JSON)
                .get();

        assertEquals(HttpStatus.UNAUTHORIZED_401, response.getStatus(), "Should be unauthorized");
    }

    @Test
    void testCorrectToken() {
        final Response response = RESOURCE.target("/Organization/" + APITestHelpers.ORGANIZATION_ID)
                .request(FHIRMediaTypes.FHIR_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + TEST_MACAROON)
                .get();

        assertEquals(HttpStatus.OK_200, response.getStatus(), "Should be authorized");
    }

    @Test
    void testIncorrectToken() {
        final Response response = RESOURCE.target("/Organization/" + BAD_ORG_ID)
                .request(FHIRMediaTypes.FHIR_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + TEST_MACAROON)
                .get();

        assertEquals(HttpStatus.UNAUTHORIZED_401, response.getStatus(), "Should not authorized for other organization");
    }

    @Test
    void testMalformedHeader() {
        final Response response = RESOURCE.target("/Organization/" + BAD_ORG_ID)
                .request(FHIRMediaTypes.FHIR_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer" + TEST_MACAROON)
                .get();

        assertEquals(HttpStatus.UNAUTHORIZED_401, response.getStatus(), "Should not authorized for other organization");
    }

    @Test
    void testPublicAPI() {
        final Response response = RESOURCE.target("/v1/metadata")
                .request(FHIRMediaTypes.FHIR_JSON)
                .get();

        assertEquals(HttpStatus.OK_200, response.getStatus(), "Should be authorized");
    }

    private static ResourceExtension buildAuthResource() {

        final DPCAPIConfiguration config = new DPCAPIConfiguration();


        // Setup mocks
        final WebTarget webTarget = mockWebTarget();

        final MacaroonsDynamicFeature dynamicFeature = new MacaroonsDynamicFeature(new MacaroonsAuthFilter(webTarget), config);
        return ResourceExtension.builder()
                .setTestContainerFactory(new GrizzlyWebTestContainerFactory())
                .addProvider(dynamicFeature)
                .addResource(mockOrganizationResource())
                .addResource(mockBaseResource())
                .build();
    }

    private static WebTarget mockWebTarget() {
        final WebTarget webTarget = mock(WebTarget.class);
        final Invocation invocation = mock(Invocation.class);
        final Invocation.Builder builder = mock(Invocation.Builder.class);

        // Simple stubbing to just return the underlying web target
        // We need these in order to avoid getting an NPE in the AuthFilter
        // When we make the request, capture
        Mockito.when(webTarget.path(requestPath.capture())).thenReturn(webTarget);
        Mockito.when(webTarget.queryParam(Mockito.anyString(), Mockito.any())).thenReturn(webTarget);
        Mockito.when(webTarget.request(Mockito.anyString())).thenReturn(builder);
        Mockito.when(builder.buildGet()).thenReturn(invocation);

        // Now, the actual Mock response
        Mockito.when(invocation.invoke()).thenAnswer(answer -> {
            // The Organization ID should be captured, but we need to extract it from the path
            final String orgID = requestPath.getValue().replace("Organization/", "").replace("/token/verify", "");
            if (orgID.equals(APITestHelpers.ORGANIZATION_ID)) {
                return Response.status(Response.Status.OK).build();
            } else {
                return Response.status(Response.Status.UNAUTHORIZED).build();
            }
        });

        return webTarget;
    }

    private static OrganizationResource mockOrganizationResource() {
        final OrganizationResource organizationResource = mock(OrganizationResource.class);

        doReturn(new Organization()).when(organizationResource).getOrganization(Mockito.any(UUID.class));

        return organizationResource;
    }

    private static BaseResource mockBaseResource() {
        final BaseResource base = mock(BaseResource.class);

        doReturn(Capabilities.buildCapabilities()).when(base).metadata();

        return base;
    }
}
