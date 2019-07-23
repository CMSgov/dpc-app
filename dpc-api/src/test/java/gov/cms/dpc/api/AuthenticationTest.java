package gov.cms.dpc.api;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IReadExecutable;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import gov.cms.dpc.fhir.helpers.FHIRHelpers;
import org.hl7.fhir.dstu3.model.Organization;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static gov.cms.dpc.api.APITestHelpers.ATTRIBUTION_URL;
import static gov.cms.dpc.api.APITestHelpers.ORGANIZATION_ID;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthenticationTest extends AbstractSecureApplicationTest {
    private static final String BAD_ORG_ID = "065fbe84-3551-4ec3-98a3-0d1198c3cb55";

    private AuthenticationTest() {
        // Not used
    }

    @Test
    void testBasicAuthentication() throws IOException {
        // Manually setup the required org functions
        final String macaroon = FHIRHelpers.registerOrganization(APITestHelpers.buildAttributionClient(ctx), ctx.newJsonParser(), ORGANIZATION_ID, ATTRIBUTION_URL);
        FHIRHelpers.registerOrganization(APITestHelpers.buildAttributionClient(ctx), ctx.newJsonParser(), BAD_ORG_ID, ATTRIBUTION_URL);

        // Now, try to read the organization, which should succeed
        final IGenericClient client = APITestHelpers.buildAuthenticatedClient(ctx, getBaseURL(), macaroon);

        final Organization organization = client
                .read()
                .resource(Organization.class)
                .withId(ORGANIZATION_ID)
                .encodedJson()
                .execute();

        assertNotNull(organization, "Should have a single Organization");

        final IReadExecutable<Organization> orgRequest = client
                .read()
                .resource(Organization.class)
                .withId(BAD_ORG_ID)
                .encodedJson();

        assertThrows(AuthenticationException.class, orgRequest::execute, "Should be unauthorized");
    }


}
