package gov.cms.dpc.api.resources.v1;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.ICreateTyped;
import gov.cms.dpc.api.auth.OrganizationPrincipal;
import org.hl7.fhir.dstu3.model.Endpoint;
import org.hl7.fhir.dstu3.model.OperationOutcome;
import org.hl7.fhir.dstu3.model.Organization;
import org.hl7.fhir.dstu3.model.Reference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class EndpointResourceUnitTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    IGenericClient attributionClient;

    EndpointResource resource;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        resource = new EndpointResource(attributionClient);
    }

    @Test
    public void testCreateEndpoint() {
        UUID orgId = UUID.randomUUID();
        Organization organization = new Organization();
        organization.setId(orgId.toString());
        OrganizationPrincipal organizationPrincipal = new OrganizationPrincipal(organization);

        Endpoint endpoint = new Endpoint();

        ICreateTyped createExec = Mockito.mock(ICreateTyped.class);
        Mockito.when(attributionClient.create().resource(endpoint).encodedJson()).thenReturn(createExec);

        MethodOutcome outcome = new MethodOutcome();
        outcome.setResource(endpoint);
        Mockito.when(createExec.execute()).thenReturn(outcome);

        Response response = resource.createEndpoint(organizationPrincipal, endpoint);
        Endpoint result = (Endpoint) response.getEntity();

        assertEquals(endpoint, result);
        assertEquals("Organization/" + orgId.toString(), endpoint.getManagingOrganization().getReference());
    }

    @Test
    public void testCreateEndpointWithManagingOrganizationMismatch() {
        UUID orgId = UUID.randomUUID();
        Organization organization = new Organization();
        organization.setId(orgId.toString());
        OrganizationPrincipal organizationPrincipal = new OrganizationPrincipal(organization);

        Endpoint endpoint = new Endpoint();
        endpoint.setManagingOrganization(new Reference("Organization/" + UUID.randomUUID()));

        assertThrows(WebApplicationException.class, () -> resource.createEndpoint(organizationPrincipal, endpoint));
    }
}
