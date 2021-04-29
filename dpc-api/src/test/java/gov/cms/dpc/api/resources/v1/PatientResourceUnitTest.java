package gov.cms.dpc.api.resources.v1;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.ICreateTyped;
import ca.uhn.fhir.rest.gclient.IReadExecutable;
import ca.uhn.fhir.validation.FhirValidator;
import gov.cms.dpc.api.auth.OrganizationPrincipal;
import org.hl7.fhir.dstu3.model.Organization;
import org.hl7.fhir.dstu3.model.Patient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import javax.ws.rs.core.Response;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PatientResourceUnitTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    IGenericClient attributionClient;

    @Mock
    FhirValidator fhirValidator;

    PatientResource resource;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        resource = new PatientResource(attributionClient, fhirValidator, null, null);
    }

    @Test
    public void testSubmitPatient() {
        UUID orgId = UUID.randomUUID();
        Organization organization = new Organization();
        organization.setId(orgId.toString());
        OrganizationPrincipal organizationPrincipal = new OrganizationPrincipal(organization);
        Patient patient = new Patient();

        ICreateTyped createExec = Mockito.mock(ICreateTyped.class);
        Mockito.when(attributionClient.create().resource(patient).encodedJson()).thenReturn(createExec);

        MethodOutcome outcome = new MethodOutcome();
        outcome.setResource(patient);
        Mockito.when(createExec.execute()).thenReturn(outcome);

        Response response = resource.submitPatient(organizationPrincipal, patient);
        Patient result = (Patient) response.getEntity();

        assertEquals(patient, result);
        assertEquals("Organization/" + orgId, result.getManagingOrganization().getReference());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGetPatient() {
        IReadExecutable<Patient> readExec = Mockito.mock(IReadExecutable.class);

        UUID patientId = UUID.randomUUID();
        Patient patient = new Patient();
        patient.setId(patientId.toString());

        Mockito.when(attributionClient.read().resource(Patient.class).withId(patientId.toString()).encodedJson())
                .thenReturn(readExec);

        Mockito.when(readExec.execute()).thenReturn(patient);

        Patient result = resource.getPatient(patientId);

        assertEquals(patient, result);
    }
}
