package gov.cms.dpc.api.resources;

import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IReadExecutable;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import gov.cms.dpc.api.APITestHelpers;
import gov.cms.dpc.api.AbstractSecureApplicationTest;
import gov.cms.dpc.fhir.helpers.FHIRHelpers;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Practitioner;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static gov.cms.dpc.api.APITestHelpers.ATTRIBUTION_URL;
import static org.junit.jupiter.api.Assertions.*;

class PractitionerResourceTest extends AbstractSecureApplicationTest {

    PractitionerResourceTest() {
        // Not used
    }

    @Test
    void ensurePractitionersExist() throws IOException {
        final IParser parser = ctx.newJsonParser();
        final IGenericClient attrClient = APITestHelpers.buildAttributionClient(ctx);
        final IGenericClient client = APITestHelpers.buildAuthenticatedClient(ctx, getBaseURL(), ORGANIZATION_TOKEN);
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

        final IReadExecutable<Practitioner> clientQuery = client
                .read()
                .resource(Practitioner.class)
                .withId(foundProvider.getIdElement())
                .encodedJson();

        final Practitioner queriedProvider = clientQuery
                .execute();

        assertTrue(foundProvider.equalsDeep(queriedProvider), "Search and GET should be identical");

        // Try to delete the practitioner

        client
                .delete()
                .resourceById(queriedProvider.getIdElement())
                .encodedJson()
                .execute();


        // Try again, should be not found
        assertThrows(AuthenticationException.class, clientQuery::execute, "Should not have practitioner");

        // Create a new org and make sure it has no providers
        final String m2 = FHIRHelpers.registerOrganization(attrClient, parser, OTHER_ORG_ID, getAdminURL());

        // Update the Macaroons interceptor to use the new Organization token
        ((APITestHelpers.MacaroonsInterceptor) client.getInterceptorService().getAllRegisteredInterceptors().get(0)).setMacaroon(m2);

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
