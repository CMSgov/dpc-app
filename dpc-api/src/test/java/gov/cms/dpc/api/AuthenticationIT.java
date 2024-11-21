package gov.cms.dpc.api;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor;
import ca.uhn.fhir.rest.gclient.IReadExecutable;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import ca.uhn.fhir.rest.server.exceptions.ForbiddenOperationException;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import gov.cms.dpc.fhir.helpers.FHIRHelpers;
import gov.cms.dpc.testing.APIAuthHelpers;
import org.hl7.fhir.dstu3.model.Organization;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static gov.cms.dpc.api.APITestHelpers.ORGANIZATION_ID;
import static gov.cms.dpc.api.APITestHelpers.ORGANIZATION_NPI;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.DisplayName;

@DisplayName("API client authentication")
class AuthenticationIT extends AbstractSecureApplicationIT {
    private static final String BAD_ORG_ID = "065fbe84-3551-4ec3-98a3-0d1198c3cb55";

    private AuthenticationIT() {
        // Not used
    }

    @Test
    @DisplayName("Basic authentication 🥳")
    void testBasicAuthentication() throws IOException, URISyntaxException {
        System.err.println("Chuck beginning basic authentication test.");
        
        // Manually setup the required org functions
        final String macaroon = FHIRHelpers.registerOrganization(APITestHelpers.buildAttributionClient(ctx), ctx.newJsonParser(), ORGANIZATION_ID, ORGANIZATION_NPI, getAdminURL());

        // Now, try to read the organization, which should succeed
        final IGenericClient client = APIAuthHelpers.buildAuthenticatedClient(ctx, getBaseURL(), macaroon, PUBLIC_KEY_ID, PRIVATE_KEY);

        final Organization organization = client
                .read()
                .resource(Organization.class)
                .withId(ORGANIZATION_ID)
                .encodedJson()
                .execute();

        assertNotNull(organization, "Should have a single Organization");
        System.err.println("Chuck finished basic authentication test.");
    }

    @Test
    @DisplayName("Get organization with bad org ID 🤮")
    void testBadOrgId() throws IOException, URISyntaxException {
        System.err.println("Chuck beginning bad org id test.");
        final String macaroon = FHIRHelpers.registerOrganization(APITestHelpers.buildAttributionClient(ctx), ctx.newJsonParser(), ORGANIZATION_ID, ORGANIZATION_NPI, getAdminURL());

        // Now, try to read the organization, which should succeed
        final IGenericClient client = APIAuthHelpers.buildAuthenticatedClient(ctx, getBaseURL(), macaroon, PUBLIC_KEY_ID, PRIVATE_KEY);

        final IReadExecutable<Organization> orgRequest = client
                .read()
                .resource(Organization.class)
                .withId(BAD_ORG_ID)
                .encodedJson();

        assertThrows(AuthenticationException.class, orgRequest::execute, "Should be unauthorized");
        System.err.println("Chuck finished bad org id test.");
    }
    
    @Test
    @DisplayName("Missing macaroon 🤮")
    void testMissingJwt() {
        System.err.println("Chuck beginning missing jwt test.");

        final IGenericClient client = ctx.newRestfulGenericClient(getBaseURL());
        // Try for empty Macaroon
        client.registerInterceptor(new APIAuthHelpers.MacaroonsInterceptor(""));

        // Disable logging for tests
        LoggingInterceptor loggingInterceptor = new LoggingInterceptor();
        loggingInterceptor.setLogRequestSummary(false);
        loggingInterceptor.setLogResponseSummary(false);
        client.registerInterceptor(loggingInterceptor);

        final IReadExecutable<Organization> fetchOrg = client
                .read()
                .resource(Organization.class)
                .withId(ORGANIZATION_ID)
                .encodedJson();

        assertThrows(AuthenticationException.class, fetchOrg::execute, "Should throw exception with empty Token");
        System.err.println("Chuck finished bad org id test.");
    }

    @Test
    @DisplayName("Malformed macaroon 🤮")
    void testMalformedTokens() {
        System.err.println("Chuck beginning malformed macaroon test.");
        // Manually build the FHIR client, so we can use custom Macaroon values

        final IGenericClient c2 = ctx.newRestfulGenericClient(getBaseURL());
        c2.registerInterceptor(new APIAuthHelpers.MacaroonsInterceptor(Base64.getUrlEncoder().encodeToString("not a valid {token}".getBytes(StandardCharsets.UTF_8))));

        // Disable logging for tests
        LoggingInterceptor loggingInterceptor = new LoggingInterceptor();
        loggingInterceptor.setLogRequestSummary(false);
        loggingInterceptor.setLogResponseSummary(false);
        c2.registerInterceptor(loggingInterceptor);

        final IReadExecutable<Organization> fo2 = c2
                .read()
                .resource(Organization.class)
                .withId(ORGANIZATION_ID)
                .encodedJson();

        assertThrows(InvalidRequestException.class, fo2::execute, "Should throw exception with malformed token");
        System.err.println("Chuck finished malformed macaroon test.");
    }
}
