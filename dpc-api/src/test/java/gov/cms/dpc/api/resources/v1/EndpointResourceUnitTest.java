package gov.cms.dpc.api.resources.v1;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.*;
import gov.cms.dpc.api.APITestHelpers;
import gov.cms.dpc.api.auth.OrganizationPrincipal;
import org.apache.http.HttpStatus;
import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.dstu3.model.Endpoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


public class EndpointResourceUnitTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    IGenericClient attributionClient;

    EndpointResource resource;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        resource = new EndpointResource(attributionClient);
    }

    @Test
    public void testCreateEndpointWithNoManagingOrganization() {
        OrganizationPrincipal op = APITestHelpers.makeOrganizationPrincipal();

        Endpoint endpoint = new Endpoint();

        ICreateTyped createExec = mock(ICreateTyped.class);
        when(attributionClient.create().resource(endpoint).encodedJson()).thenReturn(createExec);

        MethodOutcome outcome = new MethodOutcome();
        outcome.setResource(endpoint);
        when(createExec.execute()).thenReturn(outcome);

        Response response = resource.createEndpoint(op, endpoint);
        Endpoint result = (Endpoint) response.getEntity();

        assertEquals(endpoint, result);
        assertEquals("Organization/" + op.getOrganization().getId(), endpoint.getManagingOrganization().getReference());
    }

    @Test
    public void testCreateEndpointWithManagingOrganizationMismatch() {
        OrganizationPrincipal organizationPrincipal = APITestHelpers.makeOrganizationPrincipal();

        Endpoint endpoint = new Endpoint();
        endpoint.setManagingOrganization(new Reference("Organization/" + UUID.randomUUID()));

        assertThrows(WebApplicationException.class, () -> resource.createEndpoint(organizationPrincipal, endpoint));
    }

    @Test
    public void testCreateEndpointWithMatchingManagingOrganizations() {
        OrganizationPrincipal op = APITestHelpers.makeOrganizationPrincipal();

        Endpoint endpoint = new Endpoint();
        endpoint.setManagingOrganization(new Reference("Organization/" + op.getOrganization().getId()));

        ICreateTyped createExec = mock(ICreateTyped.class);
        when(attributionClient.create().resource(endpoint).encodedJson()).thenReturn(createExec);

        MethodOutcome outcome = new MethodOutcome();
        outcome.setResource(endpoint);
        when(createExec.execute()).thenReturn(outcome);

        Response response = resource.createEndpoint(op, endpoint);
        Endpoint result = (Endpoint) response.getEntity();

        assertEquals(endpoint, result);
        assertEquals("Organization/" + op.getOrganization().getId(), endpoint.getManagingOrganization().getReference());
    }

    @Test
    public void testGetEndpoints() {
        OrganizationPrincipal organizationPrincipal = APITestHelpers.makeOrganizationPrincipal();

        Bundle bundle = mock(Bundle.class);
        IQuery<Bundle> iQueryBundle = mock(IQuery.class);

        when(attributionClient.search()
                .forResource(Endpoint.class)
                .where(any(ICriterion.class))
                .returnBundle(Bundle.class)
        ).thenReturn(iQueryBundle);
        when(iQueryBundle.encodedJson()).thenReturn(iQueryBundle);
        when(iQueryBundle.execute()).thenReturn(bundle);

        assertEquals(bundle, resource.getEndpoints(organizationPrincipal));
    }

    @Test
    public void testFetchEndpoint() {
        Endpoint endPoint = mock(Endpoint.class);
        IReadExecutable<Endpoint> iReadExecutable = mock(IReadExecutable.class);

        when(attributionClient.read()
                .resource(Endpoint.class)
                .withId(any(IdType.class))
        ).thenReturn(iReadExecutable);
        when(iReadExecutable.encodedJson()).thenReturn(iReadExecutable);
        when(iReadExecutable.execute()).thenReturn(endPoint);

        assertEquals(endPoint, resource.fetchEndpoint(UUID.randomUUID()));
    }

    @Test
    public void testUpdateEndPoint() {
        Endpoint endPoint = mock(Endpoint.class, Answers.RETURNS_DEEP_STUBS);
        when(endPoint.getManagingOrganization().getReference()).thenReturn("orgReference");

        MethodOutcome outcome = mock(MethodOutcome.class);
        when(outcome.getResource()).thenReturn(endPoint);

        EndpointResource spiedResource = spy(resource);
        UUID endPointUUID = UUID.randomUUID();
        doReturn(endPoint).when(spiedResource).fetchEndpoint(endPointUUID);

        IUpdateExecutable iUpdateExecutable = mock(IUpdateExecutable.class);
        when(attributionClient.update()
                .resource(endPoint)
                .withId(anyString())
        ).thenReturn(iUpdateExecutable);
        when(iUpdateExecutable.encodedJson()).thenReturn(iUpdateExecutable);
        when(iUpdateExecutable.execute()).thenReturn(outcome);

        assertEquals(endPoint, spiedResource.updateEndpoint(endPointUUID, endPoint));
    }

    @Test
    public void testUpdateEndPointWithWrongId() {
        Endpoint newEndPoint = mock(Endpoint.class, Answers.RETURNS_DEEP_STUBS);
        when(newEndPoint.getManagingOrganization().getReference()).thenReturn("newRef");
        Endpoint existingEndPoint = mock(Endpoint.class, Answers.RETURNS_DEEP_STUBS);
        when(existingEndPoint.getManagingOrganization().getReference()).thenReturn("existingRef");

        EndpointResource spiedResource = spy(resource);
        UUID endPointUUID = UUID.randomUUID();
        doReturn(existingEndPoint).when(spiedResource).fetchEndpoint(endPointUUID);

        WebApplicationException exception =  assertThrows(WebApplicationException.class,
                () -> spiedResource.updateEndpoint(endPointUUID, newEndPoint));
        assertEquals(exception.getResponse().getStatus(), HttpStatus.SC_UNPROCESSABLE_ENTITY);
    }

    @Test
    public void deleteEndPoint() {
        UUID endPointUUID = UUID.randomUUID();

        when(attributionClient.delete()
                .resourceById("Endpoint", endPointUUID.toString())
                .execute()
        ).thenReturn(null);

        // Delete always returns a 200
        assertEquals(HttpStatus.SC_OK, resource.deleteEndpoint(endPointUUID).getStatus());
    }
}
