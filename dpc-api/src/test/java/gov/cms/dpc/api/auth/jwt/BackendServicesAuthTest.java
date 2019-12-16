package gov.cms.dpc.api.auth.jwt;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.cms.dpc.api.APITestHelpers;
import gov.cms.dpc.api.AbstractSecureApplicationTest;
import gov.cms.dpc.testing.APIAuthHelpers;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Organization;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;

import static gov.cms.dpc.api.APITestHelpers.ORGANIZATION_ID;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class BackendServicesAuthTest extends AbstractSecureApplicationTest {

    private final ObjectMapper mapper;

    BackendServicesAuthTest() {
        this.mapper = new ObjectMapper();
    }

    @Test
    void testRoundTrip() throws IOException, URISyntaxException {

        // Ensure the token is actually valid



        // Verify we can pull the Organization resource
        final IGenericClient client = APIAuthHelpers.buildAuthenticatedClient(ctx, getBaseURL(), ORGANIZATION_TOKEN, PUBLIC_KEY_ID, PRIVATE_KEY);

        final Organization orgBundle = client
                .read()
                .resource(Organization.class)
                .withId(new IdType("Organization", ORGANIZATION_ID))
                .encodedJson()
                .execute();

        assertNotNull(orgBundle, "Should have found the organization");
    }
}
