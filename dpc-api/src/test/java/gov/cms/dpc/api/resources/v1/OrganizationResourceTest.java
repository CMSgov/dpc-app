package gov.cms.dpc.api.resources.v1;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IOperationUntypedWithInput;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import gov.cms.dpc.api.APITestHelpers;
import gov.cms.dpc.api.AbstractSecureApplicationTest;
import gov.cms.dpc.testing.factories.OrganizationFactory;
import gov.cms.dpc.testing.OrganizationHelpers;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Endpoint;
import org.hl7.fhir.dstu3.model.Organization;
import org.hl7.fhir.dstu3.model.Parameters;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.UUID;

import static gov.cms.dpc.api.APITestHelpers.ORGANIZATION_ID;
import static org.junit.jupiter.api.Assertions.*;

class OrganizationResourceTest extends AbstractSecureApplicationTest {

    OrganizationResourceTest() {
        // not used
    }

    @Test
    void testOrganizationRegistration() throws IOException {
        // Generate a golden macaroon
        final String goldenMacaroon = APITestHelpers.createGoldenMacaroon();
        final IGenericClient client = APITestHelpers.buildAuthenticatedClient(ctx, getBaseURL(), goldenMacaroon);


        final String newOrgID = UUID.randomUUID().toString();
        final Organization organization = OrganizationHelpers.createOrganization(ctx, client, newOrgID, true);
        assertNotNull(organization);

        // Try again, should fail because it's a duplicate
        // Error handling is really bad right now, but it should get improved in DPC-540
        assertThrows(InternalErrorException.class, () -> OrganizationHelpers.createOrganization(ctx, APITestHelpers.buildAuthenticatedClient(ctx, getBaseURL(), goldenMacaroon), newOrgID, true));

        // Now, try to create one again, but using an actual org token
        assertThrows(AuthenticationException.class, () -> OrganizationHelpers.createOrganization(ctx, APITestHelpers.buildAuthenticatedClient(ctx, getBaseURL(), ORGANIZATION_TOKEN), UUID.randomUUID().toString(), true));
    }

    @Test
    void testOrganizationFetch() {

        final IGenericClient client = APITestHelpers.buildAuthenticatedClient(ctx, getBaseURL(), ORGANIZATION_TOKEN);

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


    @Test
    void testMissingEndpoint() throws IOException {
        // Generate a golden macaroon
        final String goldenMacaroon = APITestHelpers.createGoldenMacaroon();
        final IGenericClient client = APITestHelpers.buildAuthenticatedClient(ctx, getBaseURL(), goldenMacaroon);
        final Organization organization = OrganizationFactory.generateFakeOrganization();

        final Bundle bundle = new Bundle();
        bundle.addEntry().setResource(organization);

        final Parameters parameters = new Parameters();
        parameters.addParameter().setName("resource").setResource(bundle);

        final IOperationUntypedWithInput<Organization> operation = client
                .operation()
                .onType(Organization.class)
                .named("submit")
                .withParameters(parameters)
                .returnResourceType(Organization.class)
                .encodedJson();

        assertThrows(InvalidRequestException.class, operation::execute, "Should be unprocessable");

    }

    @Test
    void testMissingOrganization() throws IOException {
        // Generate a golden macaroon
        final String goldenMacaroon = APITestHelpers.createGoldenMacaroon();
        final IGenericClient client = APITestHelpers.buildAuthenticatedClient(ctx, getBaseURL(), goldenMacaroon);
        final Endpoint endpoint = OrganizationFactory.createFakeEndpoint();

        final Bundle bundle = new Bundle();
        bundle.addEntry().setResource(endpoint);

        final Parameters parameters = new Parameters();
        parameters.addParameter().setName("resource").setResource(bundle);

        final IOperationUntypedWithInput<Organization> operation = client
                .operation()
                .onType(Organization.class)
                .named("submit")
                .withParameters(parameters)
                .returnResourceType(Organization.class)
                .encodedJson();

        assertThrows(InvalidRequestException.class, operation::execute, "Should be unprocessable");
    }
}
