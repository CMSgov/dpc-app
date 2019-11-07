package gov.cms.dpc.api.auth.jwt;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.cms.dpc.api.APITestHelpers;
import gov.cms.dpc.api.AbstractSecureApplicationTest;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Organization;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;

import static gov.cms.dpc.api.APITestHelpers.ORGANIZATION_ID;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class BackendServicesAuthTest extends AbstractSecureApplicationTest {

    private final ObjectMapper mapper;

    BackendServicesAuthTest() {
        this.mapper = new ObjectMapper();
    }

    @Test
    void testRoundTrip() throws IOException, URISyntaxException {
        // Verify we can pull the Organization resource
        final IGenericClient client = APITestHelpers.buildAuthenticatedClient(ctx, getBaseURL(), ORGANIZATION_TOKEN, KEY_ID, privateKey);

        final Organization orgBundle = client
                .read()
                .resource(Organization.class)
                .withId(new IdType("Organization", ORGANIZATION_ID))
                .encodedJson()
                .execute();

        assertNotNull(orgBundle, "Should have found the organization");
    }
}
