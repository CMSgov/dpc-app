package gov.cms.dpc.api.resources.v1;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.ICreateTyped;
import ca.uhn.fhir.rest.gclient.IReadExecutable;
import ca.uhn.fhir.validation.FhirValidator;
import gov.cms.dpc.api.auth.OrganizationPrincipal;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import org.hl7.fhir.dstu3.model.Organization;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Practitioner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import javax.ws.rs.core.Response;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PractitionerResourceUnitTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    IGenericClient attributionClient;

    @Mock
    FhirValidator fhirValidator;

    PractitionerResource resource;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        resource = new PractitionerResource(attributionClient, fhirValidator);
    }

    @Test
    public void testPractitionerSearch() {
        //testPractitionerSearch
    }

    @Test
    public void testGetProvider() {
        //testGetProvider
    }

    @Test
    public void testSubmitProvider() {
        UUID orgId = UUID.randomUUID();
        Organization organization = new Organization();
        organization.setId(orgId.toString());
        OrganizationPrincipal organizationPrincipal = new OrganizationPrincipal(organization);
        Practitioner practitioner = new Practitioner();

        ICreateTyped createExec = Mockito.mock(ICreateTyped.class);
        Mockito.when(attributionClient.create().resource(practitioner).encodedJson()).thenReturn(createExec);

        MethodOutcome outcome = new MethodOutcome();
        outcome.setResource(practitioner);
        Mockito.when(createExec.execute()).thenReturn(outcome);

        Response response = resource.submitProvider(organizationPrincipal, practitioner);
        Practitioner result = (Practitioner) response.getEntity();

        assertEquals(practitioner, result);
        assertEquals("Organization ID", practitioner.getMeta().getTag(DPCIdentifierSystem.DPC.getSystem(), orgId.toString()).getDisplay());
    }

    @Test
    public void testBulkSubmitProviders() {
        //testBulkSubmitProviders
    }

    @Test
    public void testDeleteProvider() {
        //testDeleteProvider
    }

    @Test
    public void testUpdateProvider() {
        //testUpdateProvider
    }

    @Test
    public void testValidateProvider() {
        //testValidateProvider
    }
}
