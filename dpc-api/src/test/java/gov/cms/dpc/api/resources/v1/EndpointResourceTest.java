package gov.cms.dpc.api.resources.v1;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.ICreateTyped;
import ca.uhn.fhir.rest.gclient.IDeleteTyped;
import ca.uhn.fhir.rest.gclient.IReadExecutable;
import ca.uhn.fhir.rest.gclient.IUpdateExecutable;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import gov.cms.dpc.api.APITestHelpers;
import gov.cms.dpc.api.AbstractSecureApplicationTest;
import gov.cms.dpc.api.TestOrganizationContext;
import gov.cms.dpc.fhir.DPCResourceType;
import gov.cms.dpc.fhir.FHIRExtractors;
import gov.cms.dpc.testing.APIAuthHelpers;
import gov.cms.dpc.testing.OrganizationHelpers;
import gov.cms.dpc.testing.factories.OrganizationFactory;
import org.apache.http.HttpHeaders;
import org.eclipse.jetty.http.HttpStatus;
import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.junit.jupiter.api.Test;

import javax.ws.rs.HttpMethod;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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
            assertEquals(DPCResourceType.Endpoint.getPath(), resource.getResourceType().getPath());
            Endpoint endpoint = (Endpoint) resource;
            assertEquals(APITestHelpers.ORGANIZATION_ID, FHIRExtractors.getEntityUUID(endpoint.getManagingOrganization().getReference()).toString());
        }
    }

    @Test
    void testFetchEndpoint() throws GeneralSecurityException, IOException, URISyntaxException {
        final TestOrganizationContext orgAContext = registerAndSetupNewOrg();
        final TestOrganizationContext orgBContext = registerAndSetupNewOrg();
        final IGenericClient orgAClient = APIAuthHelpers.buildAuthenticatedClient(ctx, getBaseURL(), orgAContext.getClientToken(), UUID.fromString(orgAContext.getPublicKeyId()), orgAContext.getPrivateKey());
        final IGenericClient orgBClient = APIAuthHelpers.buildAuthenticatedClient(ctx, getBaseURL(), orgBContext.getClientToken(), UUID.fromString(orgBContext.getPublicKeyId()), orgBContext.getPrivateKey());

        //Setup Org A with one single endpoint
        MethodOutcome outcome = orgAClient.create().resource(OrganizationFactory.createValidFakeEndpoint(orgAContext.getOrgId())).execute();
        final Endpoint orgAEndpoint = (Endpoint) outcome.getResource();

        //Setup Org B with one single endpoint
        outcome = orgBClient.create().resource(OrganizationFactory.createValidFakeEndpoint(orgBContext.getOrgId())).execute();
        final Endpoint orgBEndpoint = (Endpoint) outcome.getResource();

        //Assert Org A can get their endpoint
        Endpoint readEndpoint = orgAClient.read().resource(Endpoint.class).withId(orgAEndpoint.getId()).execute();
        assertTrue(readEndpoint.equalsDeep(orgAEndpoint), "Organization should have been able to retrieve their own endpoint");

        //Assert Org B can get their endpoint
        readEndpoint = orgBClient.read().resource(Endpoint.class).withId(orgBEndpoint.getId()).execute();
        assertTrue(readEndpoint.equalsDeep(orgBEndpoint), "Organization should have been able to retrieve their own endpoint");

        //Assert Org B can NOT get org A's endpoint
        IReadExecutable<Endpoint> readExecutable = orgBClient.read().resource(Endpoint.class).withId(orgAEndpoint.getId());
        assertThrows(AuthenticationException.class, readExecutable::execute, "Expected auth error when accessing another org's endpoint");
    }

    @Test
    void testUpdateEndpoint() throws GeneralSecurityException, IOException, URISyntaxException {
        final TestOrganizationContext orgAContext = registerAndSetupNewOrg();
        final TestOrganizationContext orgBContext = registerAndSetupNewOrg();
        final IGenericClient orgAClient = APIAuthHelpers.buildAuthenticatedClient(ctx, getBaseURL(), orgAContext.getClientToken(), UUID.fromString(orgAContext.getPublicKeyId()), orgAContext.getPrivateKey());
        final IGenericClient orgBClient = APIAuthHelpers.buildAuthenticatedClient(ctx, getBaseURL(), orgBContext.getClientToken(), UUID.fromString(orgBContext.getPublicKeyId()), orgBContext.getPrivateKey());

        //Setup Org A with one single endpoint
        MethodOutcome outcome = orgAClient.create().resource(OrganizationFactory.createValidFakeEndpoint(orgAContext.getOrgId())).execute();
        final Endpoint orgAEndpoint = (Endpoint) outcome.getResource();

        //Setup Org B with one single endpoint
        outcome = orgBClient.create().resource(OrganizationFactory.createValidFakeEndpoint(orgBContext.getOrgId())).execute();
        final Endpoint orgBEndpoint = (Endpoint) outcome.getResource();

        //Assert Org A can update their own endpoint
        orgAEndpoint.setName("Name updated by Org A");
        orgAEndpoint.setPayloadType(List.of(createCodeableConcept()));
        MethodOutcome updateOutcome = orgAClient.update().resource(orgAEndpoint).withId(orgAEndpoint.getId()).execute();
        final Endpoint orgAUpdatedEndpoint = (Endpoint) updateOutcome.getResource();
        assertEquals("Name updated by Org A", orgAEndpoint.getName(), "Org should have been able to update their own endpoint's name");

        //Assert Org B can update their own endpoint
        orgBEndpoint.setName("Name updated by Org B");
        orgBEndpoint.setPayloadType(List.of(createCodeableConcept()));
        updateOutcome = orgBClient.update().resource(orgBEndpoint).withId(orgBEndpoint.getId()).execute();
        final Endpoint orgBUpdatedEndpoint = (Endpoint) updateOutcome.getResource();
        assertEquals("Name updated by Org B", orgBUpdatedEndpoint.getName(), "Org should have been able to update their own endpoint's name");

        //Assert Org B can NOT update org A's endpoint.
        orgAEndpoint.setName("Name updated by Org A");
        IUpdateExecutable iUpdateExecutable = orgBClient.update().resource(orgAEndpoint).withId(orgAEndpoint.getId());
        assertThrows(AuthenticationException.class, iUpdateExecutable::execute, "Expected auth error when updating another org's endpoint.");

        //Assert Org B can NOT update their endpoint to have org A's reference in managing organization
        orgBEndpoint.setPayloadType(List.of(createCodeableConcept()));
        orgBEndpoint.setManagingOrganization(new Reference(new IdType("Organization", orgAContext.getOrgId())));
        final IUpdateExecutable updateExecutable = orgBClient.update().resource(orgBEndpoint).withId(orgBEndpoint.getId());
        assertThrows(UnprocessableEntityException.class, updateExecutable::execute, "Expected 422 error when updating the endpoints org's endpoint.");
    }

    private CodeableConcept createCodeableConcept (){
        CodeableConcept payloadType = new CodeableConcept();
        payloadType.addCoding().setCode("nothing").setSystem("http://nothing.com");
        return payloadType;
    }


    @Test
    void testDeleteOrgsOnlyEndpoint() throws IOException, GeneralSecurityException, URISyntaxException {
        final TestOrganizationContext orgAContext = registerAndSetupNewOrg();
        final IGenericClient orgAClient = APIAuthHelpers.buildAuthenticatedClient(ctx, getBaseURL(), orgAContext.getClientToken(), UUID.fromString(orgAContext.getPublicKeyId()), orgAContext.getPrivateKey());

        //Setup Org A with 2 endpoints.
        MethodOutcome outcome = orgAClient.create().resource(OrganizationFactory.createValidFakeEndpoint(orgAContext.getOrgId())).execute();

        //Assert we have 2 resources.
        String[] endpointIds =  getAvailableResources(orgAClient, Endpoint.class).toArray(String[]::new);
        assertEquals(2, getAvailableResources(orgAClient, Endpoint.class).size(), "Only 2 Endpoints should exist");

        //Assert Org A CAN delete 1 of 2 endpoints.
        orgAClient.delete().resourceById("Endpoint",new IdType(endpointIds[0]).getIdPart()).execute();
        assertEquals(1, getAvailableResources(orgAClient, Endpoint.class).size(), "Only 1 of 2 Endpoints should exist");

        //Assert Org A CAN NOT delete their last endpoint.
        IDeleteTyped deleteExecutable = orgAClient.delete().resourceById("Endpoint",new IdType(endpointIds[1]).getIdPart());
        assertThrows(UnprocessableEntityException.class, deleteExecutable::execute, "Expected 422 when deleting the last endpoint");
    }

    @Test
    void testDeleteEndpoint() throws GeneralSecurityException, IOException, URISyntaxException {
        final TestOrganizationContext orgAContext = registerAndSetupNewOrg();
        final TestOrganizationContext orgBContext = registerAndSetupNewOrg();
        final IGenericClient orgAClient = APIAuthHelpers.buildAuthenticatedClient(ctx, getBaseURL(), orgAContext.getClientToken(), UUID.fromString(orgAContext.getPublicKeyId()), orgAContext.getPrivateKey());
        final IGenericClient orgBClient = APIAuthHelpers.buildAuthenticatedClient(ctx, getBaseURL(), orgBContext.getClientToken(), UUID.fromString(orgBContext.getPublicKeyId()), orgBContext.getPrivateKey());

        //Setup Org A with one single endpoint
        MethodOutcome outcome = orgAClient.create().resource(OrganizationFactory.createValidFakeEndpoint(orgAContext.getOrgId())).execute();
        final Endpoint orgAEndpoint = (Endpoint) outcome.getResource();

        //Setup Org B with one single endpoint
        outcome = orgBClient.create().resource(OrganizationFactory.createValidFakeEndpoint(orgBContext.getOrgId())).execute();
        final Endpoint orgBEndpoint = (Endpoint) outcome.getResource();

        //Assert Org B can NOT delete org A's endpoint.
        IDeleteTyped executableDelete = orgBClient.delete().resourceById("Endpoint",new IdType(orgAEndpoint.getId()).getIdPart());
        assertThrows(AuthenticationException.class, executableDelete::execute, "Expected auth error when deleting another org's endpoint.");

        //Assert Org B CAN delete their own endpoint
        Set<String> availableEndpoints = getAvailableResources(orgBClient, Endpoint.class);
        assertTrue(availableEndpoints.contains(orgBEndpoint.getId()), "Endpoint should be found in Org");
        orgBClient.delete().resourceById("Endpoint",new IdType(orgBEndpoint.getId()).getIdPart()).execute();
        availableEndpoints = getAvailableResources(orgBClient, Endpoint.class);
        assertFalse(availableEndpoints.contains(orgBEndpoint.getId()), "Endpoint should not be found in Org.");
    }

    private <T extends IBaseResource> Set<String> getAvailableResources(IGenericClient client, Class<T> tClass){
        Bundle result = client.search().forResource(tClass).returnBundle(Bundle.class).execute();
        return result.getEntry().parallelStream().map(Bundle.BundleEntryComponent::getResource).map(Resource::getId).collect(Collectors.toSet());
    }
}
