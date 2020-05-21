package gov.cms.dpc.api.resources.v1;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.ICreateTyped;
import ca.uhn.fhir.rest.gclient.IDeleteTyped;
import ca.uhn.fhir.rest.gclient.IReadExecutable;
import ca.uhn.fhir.rest.gclient.IUpdateExecutable;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import gov.cms.dpc.api.APITestHelpers;
import gov.cms.dpc.api.AbstractSecureApplicationTest;
import gov.cms.dpc.fhir.FHIRExtractors;
import gov.cms.dpc.testing.APIAuthHelpers;
import gov.cms.dpc.testing.OrganizationHelpers;
import gov.cms.dpc.testing.factories.OrganizationFactory;
import org.apache.http.HttpHeaders;
import org.eclipse.jetty.http.HttpStatus;
import org.hl7.fhir.dstu3.model.*;
import org.junit.jupiter.api.Test;

import javax.ws.rs.HttpMethod;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class EndpointResourceTest extends AbstractSecureApplicationTest {

    final IGenericClient client = APIAuthHelpers.buildAuthenticatedClient(ctx, getBaseURL(), ORGANIZATION_TOKEN, PUBLIC_KEY_ID, PRIVATE_KEY);

    @Test
    void testCreateEndpoint() {
        Endpoint endpoint = OrganizationFactory.createValidFakeEndpoint();

        MethodOutcome outcome = client.create().resource(endpoint).execute();
        assertTrue(outcome.getCreated());
        Endpoint createdEndpoint = (Endpoint) outcome.getResource();
        assertNotNull(createdEndpoint.getId());
        assertEquals(endpoint.getName(), createdEndpoint.getName());
        assertEquals(endpoint.getAddress(), createdEndpoint.getAddress());
        assertEquals(APITestHelpers.ORGANIZATION_ID, FHIRExtractors.getEntityUUID(createdEndpoint.getManagingOrganization().getReference()).toString());
    }

    @Test
    void testCreateInvalidEndpoint() throws IOException, URISyntaxException {
        URL url = new URL(getBaseURL() + "/Endpoint");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(HttpMethod.POST);
        conn.setRequestProperty(HttpHeaders.CONTENT_TYPE, "application/fhir+json");

        APIAuthHelpers.AuthResponse auth = APIAuthHelpers.jwtAuthFlow(getBaseURL(), ORGANIZATION_TOKEN, PUBLIC_KEY_ID, PRIVATE_KEY);
        conn.setRequestProperty(HttpHeaders.AUTHORIZATION, "Bearer " + auth.accessToken);

        conn.setDoOutput(true);
        String reqBody = "{\"test\": \"test\"}";
        conn.getOutputStream().write(reqBody.getBytes());

        assertEquals(HttpStatus.BAD_REQUEST_400, conn.getResponseCode());

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()))) {
            StringBuilder respBuilder = new StringBuilder();
            String respLine = null;
            while ((respLine = reader.readLine()) != null) {
                respBuilder.append(respLine.trim());
            }
            String resp = respBuilder.toString();
            assertTrue(resp.contains("\"resourceType\":\"OperationOutcome\""));
            assertTrue(resp.contains("Invalid JSON content"));
        }

        conn.disconnect();
    }

    @Test
    void testCreateEndpointNullStatus() {
        Endpoint endpoint = OrganizationFactory.createValidFakeEndpoint();
        endpoint.setStatus(null);

        ICreateTyped create = client.create().resource(endpoint);
        assertThrows(UnprocessableEntityException.class, create::execute);
    }

    @Test
    void testCreateEndpointDifferentOrg() throws IOException {
        final String goldenMacaroon = APIAuthHelpers.createGoldenMacaroon();
        final IGenericClient adminClient = APIAuthHelpers.buildAdminClient(ctx, getBaseURL(), goldenMacaroon, false);
        final Organization organization = OrganizationHelpers.createOrganization(ctx, adminClient, "1234567893", true);

        Endpoint endpoint = OrganizationFactory.createValidFakeEndpoint();
        endpoint.setManagingOrganization(new Reference("Organization/"+ organization.getId()));
        ICreateTyped createExec = client.create().resource(endpoint);
        assertThrows(UnprocessableEntityException.class, createExec::execute);
    }

    @Test
    void testCreateEndpointWithoutAddress() {
        Endpoint endpoint = OrganizationFactory.createValidFakeEndpoint();
        endpoint.setAddress((String)null);
        ICreateTyped createExec = client.create().resource(endpoint);
        assertThrows(UnprocessableEntityException.class, createExec::execute);
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
        Endpoint endpoint = OrganizationFactory.createValidFakeEndpoint();
        MethodOutcome outcome = client.create().resource(endpoint).execute();
        Endpoint createdEndpoint = (Endpoint) outcome.getResource();

        Endpoint readEndpoint = client.read().resource(Endpoint.class).withId(createdEndpoint.getId()).execute();
        assertTrue(readEndpoint.equalsDeep(createdEndpoint));
    }

    @Test
    void testUpdateEndpoint() {
        Endpoint endpoint = OrganizationFactory.createValidFakeEndpoint();
        MethodOutcome createOutcome = client.create().resource(endpoint).execute();
        Endpoint createdEndpoint = (Endpoint) createOutcome.getResource();
        createdEndpoint.setName("Test Update Endpoint");
        // Payload type must be set because it is not in EndpointEntity and will not be returned from the create operation
        CodeableConcept payloadType = new CodeableConcept();
        payloadType.addCoding().setCode("nothing").setSystem("http://nothing.com");
        createdEndpoint.setPayloadType(List.of(payloadType));

        MethodOutcome updateOutcome = client.update().resource(createdEndpoint).withId(createdEndpoint.getId()).execute();

        Endpoint updatedEndpoint = (Endpoint) updateOutcome.getResource();
        // Same as above; not returned by update
        updatedEndpoint.setPayloadType(List.of(payloadType));
        assertTrue(updatedEndpoint.equalsDeep(createdEndpoint));
    }

    @Test
    void testUpdateEndpointDifferentOrg() throws IOException {
        final String goldenMacaroon = APIAuthHelpers.createGoldenMacaroon();
        final IGenericClient adminClient = APIAuthHelpers.buildAdminClient(ctx, getBaseURL(), goldenMacaroon, false);

        final Organization organization1 = OrganizationHelpers.createOrganization(ctx, adminClient, "2111111110", true);
        String endpointId = FHIRExtractors.getEntityUUID(organization1.getEndpointFirstRep().getReference()).toString();

        Endpoint endpoint = client
                .read()
                .resource(Endpoint.class)
                .withId(endpointId)
                .execute();

        final Organization organization2 = OrganizationHelpers.createOrganization(ctx, adminClient, "1121111110", true);
        endpoint.setManagingOrganization(new Reference(new IdType("Organization", organization2.getId())));

        IUpdateExecutable updateExec = client
                .update()
                .resource(endpoint)
                .withId(endpoint.getId());

        assertThrows(UnprocessableEntityException.class, updateExec::execute);
    }


    @Test
    void testDeleteEndpoint() {
        Endpoint endpoint = OrganizationFactory.createValidFakeEndpoint();
        MethodOutcome createOutcome = client.create().resource(endpoint).execute();
        Endpoint createdEndpoint = (Endpoint) createOutcome.getResource();
        String endpointId = FHIRExtractors.getEntityUUID(createdEndpoint.getId()).toString();

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
    void testDeleteOnlyEndpoint() throws IOException {
        final String goldenMacaroon = APIAuthHelpers.createGoldenMacaroon();
        final IGenericClient adminClient = APIAuthHelpers.buildAdminClient(ctx, getBaseURL(), goldenMacaroon, false);

        final String newOrgNPI = "1111211110";
        final Organization organization = OrganizationHelpers.createOrganization(ctx, adminClient, newOrgNPI, true);
        String endpointId = FHIRExtractors.getEntityUUID(organization.getEndpointFirstRep().getReference()).toString();

        IDeleteTyped delete = client
                .delete()
                .resourceById("Endpoint", endpointId);

        assertThrows(UnprocessableEntityException.class, delete::execute);
    }
}
