package gov.cms.dpc.attribution.resources;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IDeleteTyped;
import ca.uhn.fhir.rest.gclient.IReadExecutable;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import gov.cms.dpc.attribution.AbstractAttributionTest;
import gov.cms.dpc.attribution.AttributionTestHelpers;
import gov.cms.dpc.fhir.FHIRExtractors;
import gov.cms.dpc.testing.OrganizationHelpers;
import org.hl7.fhir.dstu3.model.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class EndpointResourceTest extends AbstractAttributionTest {

    final IGenericClient client = AttributionTestHelpers.createFHIRClient(ctx, getServerURL());

    @Test
    void testCreateEndpoint() {
        Organization organization = OrganizationHelpers.createOrganization(ctx, client, "test-create-endpoint", false);
        Endpoint endpoint = AttributionTestHelpers.createEndpoint(organization.getId());

        MethodOutcome outcome = client
                .create()
                .resource(endpoint)
                .encodedJson()
                .execute();

        assertTrue(outcome.getCreated());
    }

    @Test
    void testSearchEndpoints() {
        Organization organization = OrganizationHelpers.createOrganization(ctx, client, "test-search-endpoints", false);
        String endpointId = FHIRExtractors.getEntityUUID(organization.getEndpointFirstRep().getReference()).toString();

        final Bundle bundle = client
                .search()
                .forResource(Endpoint.class)
                .where(Endpoint.ORGANIZATION.hasId("Organization/" + AttributionTestHelpers.DEFAULT_ORG_ID))
                .returnBundle(Bundle.class)
                .execute();

        assertTrue(bundle.hasEntry());
        assertFalse(bundle.getEntry().isEmpty());
    }

    @Test
    void testFetchEndpoint() {
        Organization organization = OrganizationHelpers.createOrganization(ctx, client, "test-fetch-endpoint", false);
        String endpointId = FHIRExtractors.getEntityUUID(organization.getEndpointFirstRep().getReference()).toString();

        Endpoint result = client
                .read()
                .resource(Endpoint.class)
                .withId(endpointId)
                .execute();

        assertNotNull(result);
    }

    @Test
    void testUpdateEndpoint() {
        Organization organization = OrganizationHelpers.createOrganization(ctx, client, "test-update-endpoint", false);
        String endpointId = FHIRExtractors.getEntityUUID(organization.getEndpointFirstRep().getReference()).toString();

        Endpoint endpoint = client
                .read()
                .resource(Endpoint.class)
                .withId(endpointId)
                .execute();

        endpoint.setName("New Endpoint Name");

        MethodOutcome outcome = client
                .update()
                .resource(endpoint)
                .withId(endpoint.getId())
                .execute();

        Endpoint updatedEndpoint = (Endpoint) outcome.getResource();
        assertEquals(endpoint.getId(), updatedEndpoint.getId());
        assertEquals("New Endpoint Name", updatedEndpoint.getName());
        assertEquals(endpoint.getAddress(), updatedEndpoint.getAddress());
    }

    @Test
    void testDeleteEndpoint() {
        Organization organization = OrganizationHelpers.createOrganization(ctx, client, "test-delete-endpoint", false);
        String endpointId = FHIRExtractors.getEntityUUID(organization.getEndpointFirstRep().getReference()).toString();

        // Add another endpoint to organization
        client
                .create()
                .resource(AttributionTestHelpers.createEndpoint(organization.getId()))
                .execute();

        // Delete original endpoint
        client
                .delete()
                .resourceById("Endpoint", endpointId)
                .execute();

        IReadExecutable readExec = client
                .read()
                .resource(Endpoint.class)
                .withId(endpointId);

        assertThrows(ResourceNotFoundException.class, readExec::execute);
    }

    @Test
    void testDeleteOnlyEndpoint() {
        Organization organization = OrganizationHelpers.createOrganization(ctx, client, "test-delete-only-endpoint", false);
        String endpointId = FHIRExtractors.getEntityUUID(organization.getEndpointFirstRep().getReference()).toString();

        IDeleteTyped delete = client
                .delete()
                .resourceById("Endpoint", endpointId);

        assertThrows(InternalErrorException.class, delete::execute);
    }
}
