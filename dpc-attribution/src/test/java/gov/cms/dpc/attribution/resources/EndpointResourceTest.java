package gov.cms.dpc.attribution.resources;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IReadExecutable;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import gov.cms.dpc.attribution.AbstractAttributionTest;
import gov.cms.dpc.attribution.AttributionTestHelpers;
import gov.cms.dpc.fhir.FHIRExtractors;
import gov.cms.dpc.testing.IntegrationTest;
import gov.cms.dpc.testing.OrganizationHelpers;
import gov.cms.dpc.testing.factories.OrganizationFactory;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Endpoint;
import org.hl7.fhir.dstu3.model.Organization;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@IntegrationTest
public class EndpointResourceTest extends AbstractAttributionTest {

    final IGenericClient client = AttributionTestHelpers.createFHIRClient(ctx, getServerURL());

    @Test
    void testCreateEndpoint() {
        Organization organization = OrganizationHelpers.createOrganization(ctx, client, "1111111112", false);
        Endpoint endpoint = OrganizationFactory.createValidFakeEndpoint(organization.getId());

        MethodOutcome outcome = client
                .create()
                .resource(endpoint)
                .encodedJson()
                .execute();

        assertTrue(outcome.getCreated());
    }

    @Test
    void testSearchEndpoints() {
        Organization organization = OrganizationHelpers.createOrganization(ctx, client, "1111111211", false);
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
        Organization organization = OrganizationHelpers.createOrganization(ctx, client, "1111111310", false);
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
        Organization organization = OrganizationHelpers.createOrganization(ctx, client, "1211111111", false);
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
        assertTrue(updatedEndpoint.equalsDeep(endpoint));
    }

    @Test
    void testDeleteEndpoint() {
        Organization organization = OrganizationHelpers.createOrganization(ctx, client, "1112111111", false);
        String endpointId = FHIRExtractors.getEntityUUID(organization.getEndpointFirstRep().getReference()).toString();

        // Add another endpoint to organization
        client
                .create()
                .resource(OrganizationFactory.createValidFakeEndpoint(organization.getId()))
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
}
