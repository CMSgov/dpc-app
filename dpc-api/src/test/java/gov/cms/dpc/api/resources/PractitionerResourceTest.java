package gov.cms.dpc.api.resources;

import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.client.api.IClientInterceptor;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.IHttpRequest;
import ca.uhn.fhir.rest.client.api.IHttpResponse;
import gov.cms.dpc.api.APITestHelpers;
import gov.cms.dpc.api.AbstractSecureApplicationTest;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Practitioner;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static gov.cms.dpc.api.APITestHelpers.ORGANIZATION_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PractitionerResourceTest extends AbstractSecureApplicationTest {

    private static final String OTHER_ORG_ID = "065fbe84-3551-4ec3-98a3-0d1198c3cb55";

    PractitionerResourceTest() {
    }

    @Test
    void ensurePractitionersExist() throws IOException {
        final IParser parser = ctx.newJsonParser();
        final IGenericClient attrClient = APITestHelpers.buildAttributionClient(ctx);
        final String macaroon = APITestHelpers.registerOrganization(attrClient, parser, ORGANIZATION_ID);
        final IGenericClient client = APITestHelpers.buildAuthenticatedClient(ctx, getBaseURL(), macaroon);
        APITestHelpers.setupPractitionerTest(client, parser);

        // Register the Macaroons Interceptor
        final APITestHelpers.MacaroonsInterceptor interceptor = new APITestHelpers.MacaroonsInterceptor(macaroon);
        client.registerInterceptor(interceptor);

        // Find everything attributed

        final Bundle practitioners = client
                .search()
                .forResource(Practitioner.class)
                .returnBundle(Bundle.class)
                .encodedJson()
                .execute();

        assertEquals(4, practitioners.getTotal(), "Should have all the providers");

        final Bundle specificSearch = client
                .search()
                .forResource(Practitioner.class)
                .where(Practitioner.IDENTIFIER.exactly().code("8075963174210588464"))
                .returnBundle(Bundle.class)
                .encodedJson()
                .execute();

        assertEquals(1, specificSearch.getTotal(), "Should have a specific provider");

        // Create a new org and make sure it has no providers
        final String m2 = APITestHelpers.registerOrganization(attrClient, parser, OTHER_ORG_ID);

        client.unregisterInterceptor(interceptor);
        interceptor.setMacaroon(m2);
        client.registerInterceptor(interceptor);

        final Bundle otherPractitioners = client
                .search()
                .forResource(Practitioner.class)
                .returnBundle(Bundle.class)
                .encodedJson()
                .execute();

        assertEquals(0, otherPractitioners.getTotal(), "Should not have any practitioners");

        // Try to look for one of the other practitioners
        final Bundle otherSpecificSearch = client
                .search()
                .forResource(Practitioner.class)
                .where(Practitioner.IDENTIFIER.exactly().code("8075963174210588464"))
                .returnBundle(Bundle.class)
                .encodedJson()
                .execute();

        assertEquals(0, otherSpecificSearch.getTotal(), "Should have a specific provider");
    }

    @Test
    void testForInvalidOrganization() {
        // TODO: Should throw 404 when requesting an org that doesn't exist
    }

    public static class OrgInterceptor implements IClientInterceptor {

        private final String organizationID;

        OrgInterceptor(String organizationID) {
            this.organizationID = organizationID;
        }

        @Override
        public void interceptRequest(IHttpRequest theRequest) {
            theRequest.addHeader("Organization", this.organizationID);
        }

        @Override
        public void interceptResponse(IHttpResponse theResponse) throws IOException {
            // not used
        }
    }


}
