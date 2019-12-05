package gov.cms.dpc.api.resources.v1;

import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IOperationUntypedWithInput;
import ca.uhn.fhir.rest.gclient.IUpdateTyped;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import gov.cms.dpc.api.APITestHelpers;
import gov.cms.dpc.api.AbstractSecureApplicationTest;
import gov.cms.dpc.fhir.helpers.FHIRHelpers;
import gov.cms.dpc.testing.APIAuthHelpers;
import gov.cms.dpc.testing.factories.OrganizationFactory;
import gov.cms.dpc.testing.OrganizationHelpers;
import org.apache.commons.lang3.tuple.Pair;
import org.assertj.core.util.Lists;
import org.hl7.fhir.dstu3.model.*;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
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
        final String goldenMacaroon = APIAuthHelpers.createGoldenMacaroon();
        final IGenericClient client = APIAuthHelpers.buildAdminClient(ctx, getBaseURL(), goldenMacaroon, false);


        final String newOrgID = UUID.randomUUID().toString();
        final Organization organization = OrganizationHelpers.createOrganization(ctx, client, newOrgID, true);
        assertNotNull(organization);

        // Try again, should fail because it's a duplicate
        // Error handling is really bad right now, but it should get improved in DPC-540
        assertThrows(InvalidRequestException.class, () -> OrganizationHelpers.createOrganization(ctx, client, newOrgID, true));

        // Now, try to create one again, but using an actual org token
        assertThrows(AuthenticationException.class, () -> OrganizationHelpers.createOrganization(ctx, APIAuthHelpers.buildAuthenticatedClient(ctx, getBaseURL(), ORGANIZATION_TOKEN, PUBLIC_KEY_ID, PRIVATE_KEY), UUID.randomUUID().toString(), true));
    }

    void testOrganizationFetch() {

        final IGenericClient client = APIAuthHelpers.buildAuthenticatedClient(ctx, getBaseURL(), ORGANIZATION_TOKEN, PUBLIC_KEY_ID, PRIVATE_KEY);

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
        final String goldenMacaroon = APIAuthHelpers.createGoldenMacaroon();
        final IGenericClient client = APIAuthHelpers.buildAdminClient(ctx, getBaseURL(), goldenMacaroon, false);
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
        final String goldenMacaroon = APIAuthHelpers.createGoldenMacaroon();
        final IGenericClient client = APIAuthHelpers.buildAdminClient(ctx, getBaseURL(), goldenMacaroon, false);
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

    @Test
    void testUpdateOrganization() throws IOException, URISyntaxException, NoSuchAlgorithmException {
        final String orgID = UUID.randomUUID().toString();
        final IParser parser = ctx.newJsonParser();
        final IGenericClient attrClient = APITestHelpers.buildAttributionClient(ctx);
        final String macaroon = FHIRHelpers.registerOrganization(attrClient, parser, orgID, getAdminURL());
        final Pair<UUID, PrivateKey> uuidPrivateKeyPair = APIAuthHelpers.generateAndUploadKey("org-update-key", orgID, GOLDEN_MACAROON, getBaseURL());
        final IGenericClient client = APIAuthHelpers.buildAuthenticatedClient(ctx, getBaseURL(), macaroon, uuidPrivateKeyPair.getLeft(), uuidPrivateKeyPair.getRight());

        Organization organization = client
                .read()
                .resource(Organization.class)
                .withId(orgID)
                .encodedJson()
                .execute();

        assertNotNull(organization);

        organization.setName("New Org Name");
        organization.setContact(Lists.emptyList());

        MethodOutcome outcome = client
                .update()
                .resource(organization)
                .execute();

        Organization result = (Organization) outcome.getResource();
        assertEquals(orgID, result.getIdentifierFirstRep().getValue());
        assertEquals(organization.getName(), result.getName(), "Name should be updated");
        assertTrue(organization.getContact().isEmpty(), "Contact list should be updated");
        assertEquals(1, result.getEndpoint().size(), "Endpoint list should be unchanged");

        // Try to update when authenticated as different organization
        final String org2ID = UUID.randomUUID().toString();
        final String org2Macaroon = FHIRHelpers.registerOrganization(attrClient, parser, org2ID, getAdminURL());
        final Pair<UUID, PrivateKey> org2UUIDPrivateKeyPair = APIAuthHelpers.generateAndUploadKey("org2-update-key", org2ID, GOLDEN_MACAROON, getBaseURL());
        final IGenericClient org2Client = APIAuthHelpers.buildAuthenticatedClient(ctx, getBaseURL(), org2Macaroon, org2UUIDPrivateKeyPair.getLeft(), org2UUIDPrivateKeyPair.getRight());

        IUpdateTyped update = org2Client.update().resource(organization);
        assertThrows(AuthenticationException.class, update::execute);
    }
}
