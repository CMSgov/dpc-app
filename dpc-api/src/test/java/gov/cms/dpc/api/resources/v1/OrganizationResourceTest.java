package gov.cms.dpc.api.resources.v1;

import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import gov.cms.dpc.api.APITestHelpers;
import gov.cms.dpc.api.AbstractSecureApplicationTest;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Endpoint;
import org.hl7.fhir.dstu3.model.Organization;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static gov.cms.dpc.api.APITestHelpers.ORGANIZATION_ID;
import static org.junit.jupiter.api.Assertions.*;

class OrganizationResourceTest extends AbstractSecureApplicationTest {

    OrganizationResourceTest() {
        // not used
    }

    @Test
    void testOrganizationFetch() throws IOException {

        // This will come out once DPC-428 is merged
        final IParser parser = ctx.newJsonParser();
        final IGenericClient attrClient = APITestHelpers.buildAttributionClient(ctx);
        final String macaroon = APITestHelpers.registerOrganization(attrClient, parser, ORGANIZATION_ID);
        final IGenericClient client = APITestHelpers.buildAuthenticatedClient(ctx, getBaseURL(), macaroon);

        final Organization organization = client
                .read()
                .resource(Organization.class)
                .withId(ORGANIZATION_ID)
                .encodedJson()
                .execute();

        assertNotNull(organization, "Should have organization");

        // Try to get all public endpoints
        final Bundle endPointBundle = client
                .search()
                .forResource(Endpoint.class)
                .encodedJson()
                .returnBundle(Bundle.class)
                .execute();
        assertEquals(1, endPointBundle.getTotal(), "Should have one endpoint");

        // Try to fetch it
        final Endpoint endpoint = (Endpoint) endPointBundle.getEntryFirstRep().getResource();
        final Endpoint fetchedEndpoint = client
                .read()
                .resource(Endpoint.class)
                .withId(endpoint.getId())
                .encodedJson()
                .execute();

        assertTrue(endpoint.equalsDeep(fetchedEndpoint), "Should have matching records");
    }
}
