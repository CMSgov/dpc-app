package gov.cms.dpc.api.resources.v1;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.ICreateTyped;
import ca.uhn.fhir.rest.gclient.IOperationUntypedWithInput;
import ca.uhn.fhir.rest.gclient.IReadExecutable;
import ca.uhn.fhir.rest.gclient.IUpdateExecutable;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ValidationOptions;
import ca.uhn.fhir.validation.ValidationResult;
import com.google.common.net.HttpHeaders;
import gov.cms.dpc.api.auth.OrganizationPrincipal;
import gov.cms.dpc.bluebutton.client.BlueButtonClient;
import gov.cms.dpc.common.utils.NPIUtil;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import gov.cms.dpc.queue.service.DataService;
import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.instance.model.api.IBaseOperationOutcome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.Mock;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.util.UUID;

import static gov.cms.dpc.api.APITestHelpers.createProvenance;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

public class PatientResourceUnitTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    IGenericClient attributionClient;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    FhirValidator fhirValidator;

    @Mock
    DataService dataService;

    @Mock
    BlueButtonClient bfdClient;

    PatientResource patientResource;

    @BeforeEach
    public void setUp() {
        openMocks(this);
        patientResource = new PatientResource(attributionClient, fhirValidator, dataService, bfdClient);
    }

    @Test
    public void testSubmitPatient() {
        UUID orgId = UUID.randomUUID();
        Organization organization = new Organization();
        organization.setId(orgId.toString());
        OrganizationPrincipal organizationPrincipal = new OrganizationPrincipal(organization);
        Patient patient = new Patient();

        ICreateTyped createExec = mock(ICreateTyped.class);
        when(attributionClient.create().resource(patient).encodedJson()).thenReturn(createExec);

        MethodOutcome outcome = new MethodOutcome();
        outcome.setResource(patient);
        when(createExec.execute()).thenReturn(outcome);

        Response response = patientResource.submitPatient(organizationPrincipal, patient);
        Patient result = (Patient) response.getEntity();

        assertEquals(patient, result);
        assertEquals("Organization/" + orgId, result.getManagingOrganization().getReference());
    }

    @Test
    public void testBulkSubmitPatients() {
        UUID orgId = UUID.randomUUID();
        Organization organization = new Organization();
        organization.setId(orgId.toString());
        OrganizationPrincipal organizationPrincipal = new OrganizationPrincipal(organization);
        Bundle bundle = new Bundle();
        bundle.addEntry().setResource(new Patient());
        Parameters params = new Parameters();
        params.addParameter().setResource(bundle);

        @SuppressWarnings("unchecked")
        IOperationUntypedWithInput<Bundle> patientBundle = mock(IOperationUntypedWithInput.class);
        when(attributionClient
                .operation()
                .onType(Patient.class)
                .named("submit")
                .withParameters(any()).returnResourceType(Bundle.class).encodedJson()
        ).thenReturn(patientBundle);
        when(patientBundle.execute()).thenReturn(bundle);

        Bundle actualResponse = patientResource.bulkSubmitPatients(organizationPrincipal, params);
        assertEquals(bundle, actualResponse);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGetPatient() {
        IReadExecutable<Patient> readExec = mock(IReadExecutable.class);

        UUID patientId = UUID.randomUUID();
        Patient patient = new Patient();
        patient.setId(patientId.toString());

        when(
                attributionClient.read().resource(Patient.class).withId(patientId.toString()).encodedJson()
        ).thenReturn(readExec);
        when(readExec.execute()).thenReturn(patient);

        Patient result = patientResource.getPatient(patientId);
        assertEquals(patient, result);
    }

    @Test
    public void testEverything() {
        UUID practitionerId = UUID.randomUUID();
        Practitioner practitioner = new Practitioner();
        practitioner.setId(practitionerId.toString());
        String pracNpi = NPIUtil.generateNPI();
        practitioner.addIdentifier().setSystem(DPCIdentifierSystem.NPPES.getSystem()).setValue(pracNpi);
        @SuppressWarnings("unchecked")
        IReadExecutable<Practitioner> pracExec = mock(IReadExecutable.class);
        when(
                attributionClient.read().resource(Practitioner.class).withId(practitionerId.toString()).encodedJson()
        ).thenReturn(pracExec);
        when(pracExec.execute()).thenReturn(practitioner);

        UUID patientId = UUID.randomUUID();
        Patient patient = new Patient();
        patient.setId(patientId.toString());
        String patientMbi = "3aa0C00aA00";
        patient.addIdentifier().setSystem(DPCIdentifierSystem.MBI.getSystem()).setValue(patientMbi);
        @SuppressWarnings("unchecked")
        IReadExecutable<Patient> patExec = mock(IReadExecutable.class);
        when(
                attributionClient.read().resource(Patient.class).withId(patientId.toString()).encodedJson()
        ).thenReturn(patExec);
        when(patExec.execute()).thenReturn(patient);

        UUID orgId = UUID.randomUUID();
        Organization organization = new Organization();
        organization.setId(orgId.toString());
        String orgNpi = NPIUtil.generateNPI();
        organization.addIdentifier().setSystem(DPCIdentifierSystem.NPPES.getSystem()).setValue(orgNpi);
        @SuppressWarnings("unchecked")
        IReadExecutable<Organization> orgExec = mock(IReadExecutable.class);
        when(attributionClient
                .read()
                .resource(Organization.class)
                .withId(orgId.toString())
                .encodedJson()
        ).thenReturn(orgExec);
        when(orgExec.execute()).thenReturn(organization);

        OrganizationPrincipal organizationPrincipal = new OrganizationPrincipal(organization);
        Bundle bundle = new Bundle();
        bundle.addEntry().setResource(patient);
        Provenance provenance = createProvenance(
                orgId.toString(), practitionerId.toString(), singletonList(patientId.toString())
        );

        String since = "2000-01-01T12:00+00:00";
        HttpServletRequest request = mock(HttpServletRequest.class);
        String requestUrl = "http://localhost:3000/v1/Patient/12345/everything";
        String requestIp = "200.0.200.200";
        when(request.getRequestURL()).thenReturn(new StringBuffer(requestUrl));
        when(request.getHeader(HttpHeaders.X_FORWARDED_FOR)).thenReturn(requestIp);

        when(dataService.retrieveData(
                eq(orgId), eq(orgNpi), eq(pracNpi), eq(singletonList(patientMbi)), any(), any(), eq(requestIp), eq(requestUrl)
        )).thenReturn(patient);
        when(
                bfdClient.requestPatientFromServer(anyString(), any(), any())
        ).thenReturn(bundle);

        Bundle actualResponse = patientResource.everything(organizationPrincipal, provenance, patientId, since, request);
        assertEquals(bundle, actualResponse);
    }

//    @Test
//    public void testDeletePatient() {
//        UUID patientId = UUID.randomUUID();
//
//        IDeleteTyped delResp = Mockito.mock(IDeleteTyped.class);
//        Mockito.when(attributionClient
//                .delete()
//                .resourceById(new IdType("Patient", patientId.toString()))
//                .encodedJson()
//        ).thenReturn(delResp);
//
//        Response actualResponse = patientResource.deletePatient(patientId);
//        assertEquals(200, actualResponse.getStatus());
//    }

    @Test
    public void testUpdatePatient() {
        UUID patientId = UUID.randomUUID();
        Patient patient = new Patient();
        patient.setId(patientId.toString());

        MethodOutcome outcome = mock(MethodOutcome.class);
        when(outcome.getResource()).thenReturn(patient);

        IUpdateExecutable updateExec = mock(IUpdateExecutable.class);
        when(attributionClient
                .update()
                .resource(patient)
                .withId(new IdType("Patient", patientId.toString()))
                .encodedJson()
        ).thenReturn(updateExec);
        when(updateExec.execute()).thenReturn(outcome);

        Patient actualResponse = patientResource.updatePatient(patientId, patient);
        assertEquals(patient, actualResponse);
    }

    @Test
    public void testUpdatePatientNoResource() {
        UUID patientId = UUID.randomUUID();
        Patient patient = new Patient();
        patient.setId(patientId.toString());

        IUpdateExecutable updateExec = mock(IUpdateExecutable.class);
        when(attributionClient
                .update()
                .resource(patient)
                .withId(new IdType("Patient", patientId.toString()))
                .encodedJson()
        ).thenReturn(updateExec);
        when(updateExec.execute()).thenReturn(mock(MethodOutcome.class));

        try {
            patientResource.updatePatient(patientId, patient);
            fail("This call is supposed to fail.");
        } catch (WebApplicationException exc) {
            String excMsg = "Unable to update Patient";
            assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), exc.getResponse().getStatus());
            assertEquals(excMsg, exc.getMessage());
        }
    }

    @Test
    public void testValidatePatient() {
        UUID orgId = UUID.randomUUID();
        Organization organization = new Organization();
        organization.setId(orgId.toString());
        OrganizationPrincipal organizationPrincipal = new OrganizationPrincipal(organization);
        Parameters params = new Parameters();
        params.addParameter().setResource(new Bundle());

        ValidationResult validationResult = mock(ValidationResult.class);
        Resource resource = params.getParameterFirstRep().getResource();
        ValidationOptions valOps = new ValidationOptions();
        String patient_profile_uri = "https://dpc.cms.gov/api/v1/StructureDefinition/dpc-profile-patient";
        valOps.addProfile(patient_profile_uri);
        when(fhirValidator.validateWithResult(resource, valOps)).thenReturn(validationResult);

        IBaseOperationOutcome actualResponse = patientResource.validatePatient(organizationPrincipal, params);

        assertNotNull(actualResponse);
    }
}
