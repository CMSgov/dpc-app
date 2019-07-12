package gov.cms.dpc.api.resources;

import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import gov.cms.dpc.api.APITestHelpers;
import gov.cms.dpc.api.AbstractApplicationTest;
import org.junit.jupiter.api.Test;

import java.io.IOException;

class PractitionerResourceTest extends AbstractApplicationTest {

    PractitionerResourceTest() {
    }

    @Test
    void ensurePractitionersExist() throws IOException {
        final IParser parser = ctx.newJsonParser();
        final String macaroon = APITestHelpers.setupOrganizationTest(APITestHelpers.buildAttributionClient(ctx), parser);
        final IGenericClient client = APITestHelpers.buildAuthenticatedClient(ctx, getBaseURL(), macaroon);
        APITestHelpers.setupPractitionerTest(client, parser);


    }
}
