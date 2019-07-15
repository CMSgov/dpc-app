package gov.cms.dpc.api.resources;

import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import gov.cms.dpc.api.APITestHelpers;
import gov.cms.dpc.api.AbstractApplicationTest;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Practitioner;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PractitionerResourceTest extends AbstractApplicationTest {

    PractitionerResourceTest() {
    }

    @Test
    void ensurePractitionersExist() throws IOException {
        final IParser parser = ctx.newJsonParser();
        final String macaroon = APITestHelpers.setupOrganizationTest(APITestHelpers.buildAttributionClient(ctx), parser);
        final IGenericClient client = APITestHelpers.buildAuthenticatedClient(ctx, getBaseURL(), macaroon);
        APITestHelpers.setupPractitionerTest(client, parser);

        // Find everything attributed

        final Bundle practitioners = client
                .search()
                .forResource(Practitioner.class)
                .returnBundle(Bundle.class)
                .encodedJson()
                .execute();

        assertEquals(4, practitioners.getTotal(), "Should have all the providers");
    }
}
