package gov.cms.dpc.api.resources;

import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import gov.cms.dpc.api.APITestHelpers;
import gov.cms.dpc.api.AbstractSecureApplicationTest;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Practitioner;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static gov.cms.dpc.api.APITestHelpers.ORGANIZATION_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

        // Fetch the provider directly
        final Practitioner foundProvider = (Practitioner) specificSearch.getEntryFirstRep().getResource();

        final Practitioner queriedProvider = client
                .read()
                .resource(Practitioner.class)
                .withId(foundProvider.getIdElement())
                .encodedJson()
                .execute();

        assertTrue(foundProvider.equalsDeep(queriedProvider), "Search and GET should be identical");

        // Create a new org and make sure it has no providers
        final String m2 = APITestHelpers.registerOrganization(attrClient, parser, OTHER_ORG_ID);

        // Update the Macaroons interceptor to use the new Organization token
        ((APITestHelpers.MacaroonsInterceptor) client.getInterceptors().get(0)).setMacaroon(m2);

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
                .where(Practitioner.IDENTIFIER.exactly().identifier(foundProvider.getIdentifierFirstRep().getValue()))
                .returnBundle(Bundle.class)
                .encodedJson()
                .execute();

        assertEquals(0, otherSpecificSearch.getTotal(), "Should have a specific provider");

        // Try to search for our fund provider
    }
}
