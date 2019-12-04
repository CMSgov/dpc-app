package gov.cms.dpc.api.resources.v1;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import gov.cms.dpc.api.APITestHelpers;
import gov.cms.dpc.api.AbstractSecureApplicationTest;
import gov.cms.dpc.fhir.FHIRExtractors;
import gov.cms.dpc.testing.APIAuthHelpers;
import org.hl7.fhir.dstu3.model.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class EndpointResourceTest extends AbstractSecureApplicationTest {

    final IGenericClient client = APIAuthHelpers.buildAuthenticatedClient(ctx, getBaseURL(), ORGANIZATION_TOKEN, PUBLIC_KEY_ID, PRIVATE_KEY);

    @Test
    void testCreateEndpoint() {
        Endpoint endpoint = APITestHelpers.makeEndpoint();

        MethodOutcome outcome = client.create().resource(endpoint).execute();
        assertTrue(outcome.getCreated());
        Endpoint createdEndpoint = (Endpoint) outcome.getResource();
        assertNotNull(createdEndpoint.getId());
        assertEquals(endpoint.getName(), createdEndpoint.getName());
        assertEquals(endpoint.getAddress(), createdEndpoint.getAddress());
        assertEquals(APITestHelpers.ORGANIZATION_ID, FHIRExtractors.getEntityUUID(createdEndpoint.getManagingOrganization().getReference()).toString());
    }

    @Test
    void testGetEndpoints() {
        Bundle result = client.search().forResource(Endpoint.class).returnBundle(Bundle.class).execute();
        assertTrue(result.getTotal() > 0);
        for (Bundle.BundleEntryComponent component : result.getEntry()) {
            Resource resource = component.getResource();
            assertEquals(ResourceType.Endpoint, resource.getResourceType());
            Endpoint endpoint = (Endpoint) resource;
            assertEquals(APITestHelpers.ORGANIZATION_ID, FHIRExtractors.getEntityUUID(endpoint.getManagingOrganization().getReference()).toString());
        }
    }

    @Test
    void testFetchEndpoint() {
        Endpoint endpoint = APITestHelpers.makeEndpoint();
        MethodOutcome outcome = client.create().resource(endpoint).execute();
        Endpoint createdEndpoint = (Endpoint) outcome.getResource();

        Endpoint readEndpoint = client.read().resource(Endpoint.class).withId(createdEndpoint.getId()).execute();
        assertEquals(createdEndpoint.getId(), readEndpoint.getId());
        assertEquals(createdEndpoint.getName(), readEndpoint.getName());
        assertEquals(createdEndpoint.getAddress(), readEndpoint.getAddress());
    }

    @Test
    void testUpdateEndpoint() {
        Endpoint endpoint = APITestHelpers.makeEndpoint();
        MethodOutcome createOutcome = client.create().resource(endpoint).execute();
        Endpoint createdEndpoint = (Endpoint) createOutcome.getResource();
        createdEndpoint.setName("Test Update Endpoint");

        MethodOutcome updateOutcome = client.update().resource(createdEndpoint).withId(createdEndpoint.getId()).execute();

        Endpoint updatedEndpoint = (Endpoint) updateOutcome.getResource();
        assertEquals(createdEndpoint, updatedEndpoint);
    }

    void testDeleteEndpoint() {}
}
