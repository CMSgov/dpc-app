package gov.cms.dpc.api.resources.r4;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ResultSeverityEnum;
import ca.uhn.fhir.validation.SingleValidationMessage;
import ca.uhn.fhir.validation.ValidationOptions;
import ca.uhn.fhir.validation.ValidationResult;
import gov.cms.dpc.api.auth.OrganizationPrincipal;
import gov.cms.dpc.api.r4.APITestHelpers;
import gov.cms.dpc.attribution.client.AttributionClient;
import gov.cms.dpc.attribution.client.search.PatientSearch;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import gov.cms.dpc.fhir.validations.profiles.r4.PatientProfile;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.Reference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class PatientResourceUnitTest {

    private static final String BASE_URL = "http://localhost:3002/api";

    private AttributionClient attributionClient;
    private FhirValidator fhirValidator;
    private PatientResource patientResource;

    @BeforeEach
    void setUp() {
        attributionClient = mock(AttributionClient.class);
        fhirValidator = mock(FhirValidator.class);
        patientResource = new PatientResource(attributionClient, fhirValidator, BASE_URL);
    }

    @Test
    void testPatientSearch() {
        OrganizationPrincipal principal = APITestHelpers.makeOrganizationPrincipal();
        String mbi = "3aa0C00aA00";
        Bundle bundle = new Bundle();
        Patient patient = new Patient();
        patient.addIdentifier().setSystem(DPCIdentifierSystem.MBI.getSystem()).setValue(mbi);
        bundle.addEntry().setResource(patient);
        when(attributionClient.searchPatients(new PatientSearch(null, mbi, principal.getID(), null, null, null)))
                .thenReturn(bundle);

        assertSame(bundle, patientResource.patientSearch(principal, mbi, null, null));
        verify(attributionClient).searchPatients(new PatientSearch(null, mbi, principal.getID(), null, null, null));
    }

    @Test
    void testPatientSearchWithQualifiedIdentifier() {
        OrganizationPrincipal principal = APITestHelpers.makeOrganizationPrincipal();
        String mbi = DPCIdentifierSystem.MBI.getSystem() + "|3aa0C00aA00";
        Bundle bundle = new Bundle();
        when(attributionClient.searchPatients(any(PatientSearch.class))).thenReturn(bundle);

        assertSame(bundle, patientResource.patientSearch(principal, mbi, null, null));
        verify(attributionClient).searchPatients(new PatientSearch(null, mbi, principal.getID(), null, null, null));
    }

    @Test
    void testPatientSearchNoIdentifier() {
        OrganizationPrincipal principal = APITestHelpers.makeOrganizationPrincipal();
        Bundle bundle = new Bundle();
        bundle.addEntry().setResource(new Patient());
        when(attributionClient.searchPatients(any(PatientSearch.class))).thenReturn(bundle);

        assertSame(bundle, patientResource.patientSearch(principal, null, null, null));
        verify(attributionClient).searchPatients(new PatientSearch(null, null, principal.getID(), null, null, null));
    }

    @Test
    void testPatientSearchWithEmptyIdentifier() {
        OrganizationPrincipal principal = APITestHelpers.makeOrganizationPrincipal();
        Bundle bundle = new Bundle();
        when(attributionClient.searchPatients(any(PatientSearch.class))).thenReturn(bundle);

        assertSame(bundle, patientResource.patientSearch(principal, "", null, null));
        verify(attributionClient).searchPatients(new PatientSearch(null, "", principal.getID(), null, null, null));
    }

    @Test
    void testCountInvalid() {
        WebApplicationException exception = assertThrows(WebApplicationException.class,
                () -> patientResource.patientSearch(APITestHelpers.makeOrganizationPrincipal(), null, -4, null));

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), exception.getResponse().getStatus());
        verifyNoInteractions(attributionClient);
    }

    @Test
    void testPageInvalid() {
        WebApplicationException exception = assertThrows(WebApplicationException.class,
                () -> patientResource.patientSearch(APITestHelpers.makeOrganizationPrincipal(), null, null, -5));

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), exception.getResponse().getStatus());
        verifyNoInteractions(attributionClient);
    }

    @Test
    void testCountEqualsZero() {
        OrganizationPrincipal principal = APITestHelpers.makeOrganizationPrincipal();
        int patientCount = 600;
        Bundle summary = new Bundle();
        summary.setTotal(patientCount);
        summary.setType(Bundle.BundleType.SEARCHSET);
        when(attributionClient.searchPatients(any(PatientSearch.class))).thenReturn(summary);

        Bundle response = patientResource.patientSearch(principal, null, 0, 1);

        assertSame(summary, response);
        assertEquals(patientCount, response.getTotal());
        assertTrue(response.getEntry().isEmpty());
        verify(attributionClient).searchPatients(new PatientSearch(null, null, principal.getID(), 0, 1, null));
    }

    @Test
    void testPagingLinksHaveAbsoluteUrls() {
        OrganizationPrincipal principal = APITestHelpers.makeOrganizationPrincipal();
        Bundle bundle = createPatientBundle(100, principal.getID());
        bundle.addLink().setRelation(IBaseBundle.LINK_SELF).setUrl("/r4/Patient?_count=10&_offset=10");
        bundle.addLink().setRelation(IBaseBundle.LINK_PREV).setUrl("/r4/Patient?_count=10&_offset=0");
        bundle.addLink().setRelation(IBaseBundle.LINK_NEXT).setUrl("/r4/Patient?_count=10&_offset=20");
        bundle.addLink().setRelation("first").setUrl("/r4/Patient?_count=10&_offset=0");
        bundle.addLink().setRelation("related").setUrl("/r4/Patient/other");
        when(attributionClient.searchPatients(any(PatientSearch.class))).thenReturn(bundle);

        Bundle response = patientResource.patientSearch(principal, null, 10, 10);

        assertSame(bundle, response);
        assertEquals(BASE_URL + "/v3/Patient?_count=10&_offset=10", response.getLink(IBaseBundle.LINK_SELF).getUrl());
        assertEquals(BASE_URL + "/v3/Patient?_count=10&_offset=0", response.getLink(IBaseBundle.LINK_PREV).getUrl());
        assertEquals(BASE_URL + "/v3/Patient?_count=10&_offset=20", response.getLink(IBaseBundle.LINK_NEXT).getUrl());
        assertEquals(BASE_URL + "/v3/Patient?_count=10&_offset=0", response.getLink("first").getUrl());
        assertEquals("/r4/Patient/other", response.getLink("related").getUrl());
        verify(attributionClient).searchPatients(new PatientSearch(null, null, principal.getID(), 10, 10, null));
    }

    @Test
    void testPatientSearchLargerRoster() {
        OrganizationPrincipal principal = APITestHelpers.makeOrganizationPrincipal();
        Bundle bundle = createPatientBundle(400, principal.getID());
        when(attributionClient.searchPatients(any(PatientSearch.class))).thenReturn(bundle);

        Bundle response = patientResource.patientSearch(principal, null, null, null);

        assertSame(bundle, response);
        assertEquals(400, response.getEntry().size());
    }

    @Test
    void testSubmitPatient() {
        OrganizationPrincipal principal = APITestHelpers.makeOrganizationPrincipal();
        Patient patient = new Patient();
        patient.setManagingOrganization(new Reference("Organization/" + UUID.randomUUID()));
        MethodOutcome outcome = new MethodOutcome();
        outcome.setResource(patient);
        when(attributionClient.createPatient(patient)).thenReturn(outcome);

        Response response = patientResource.submitPatient(principal, patient);

        assertSame(patient, response.getEntity());
        assertEquals("Organization/" + principal.getID(), patient.getManagingOrganization().getReference());
        verify(attributionClient).createPatient(patient);
    }

    @Test
    void testSubmitPatientCreatedLocation() {
        OrganizationPrincipal principal = APITestHelpers.makeOrganizationPrincipal();
        UUID patientID = UUID.randomUUID();
        Patient patient = new Patient();
        patient.setId(patientID.toString());
        MethodOutcome outcome = new MethodOutcome();
        outcome.setResource(patient);
        outcome.setCreated(true);
        when(attributionClient.createPatient(patient)).thenReturn(outcome);

        Response response = patientResource.submitPatient(principal, patient);

        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        assertEquals(URI.create("v3/Patient/" + patientID), response.getLocation());
    }

    @Test
    void testSubmitPatientNoResource() {
        Patient patient = new Patient();
        when(attributionClient.createPatient(patient)).thenReturn(new MethodOutcome());

        WebApplicationException exception = assertThrows(WebApplicationException.class,
                () -> patientResource.submitPatient(APITestHelpers.makeOrganizationPrincipal(), patient));

        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), exception.getResponse().getStatus());
    }

    @Test
    void testBulkSubmitPatients() {
        OrganizationPrincipal principal = APITestHelpers.makeOrganizationPrincipal();
        Patient patient = new Patient();
        patient.setManagingOrganization(new Reference("Organization/" + UUID.randomUUID()));
        Bundle bundle = new Bundle();
        bundle.addEntry().setResource(patient);
        Parameters params = parametersWithBundle(bundle);
        ValidationResult validation = mock(ValidationResult.class);
        when(validation.isSuccessful()).thenReturn(true);
        when(fhirValidator.validateWithResult(eq(patient), any(ValidationOptions.class))).thenReturn(validation);
        when(attributionClient.submitPatients(bundle)).thenReturn(bundle);

        assertSame(bundle, patientResource.bulkSubmitPatients(principal, params));
        assertEquals("Organization/" + principal.getID(), patient.getManagingOrganization().getReference());
        verify(fhirValidator).validateWithResult(eq(patient),
                argThat((ValidationOptions options) -> options.getProfiles().contains(PatientProfile.PROFILE_URI)));
        verify(attributionClient).submitPatients(bundle);
    }

    @Test
    void testBulkSubmitValidatesEveryPatient() {
        OrganizationPrincipal principal = APITestHelpers.makeOrganizationPrincipal();
        Patient first = new Patient();
        Patient second = new Patient();
        Bundle bundle = new Bundle();
        bundle.addEntry().setResource(first);
        bundle.addEntry().setResource(second);
        ValidationResult validation = mock(ValidationResult.class);
        when(validation.isSuccessful()).thenReturn(true);
        when(fhirValidator.validateWithResult(any(Patient.class), any(ValidationOptions.class))).thenReturn(validation);
        when(attributionClient.submitPatients(bundle)).thenReturn(bundle);

        assertSame(bundle, patientResource.bulkSubmitPatients(principal, parametersWithBundle(bundle)));
        assertEquals("Organization/" + principal.getID(), first.getManagingOrganization().getReference());
        assertEquals("Organization/" + principal.getID(), second.getManagingOrganization().getReference());
        verify(fhirValidator).validateWithResult(eq(first), any(ValidationOptions.class));
        verify(fhirValidator).validateWithResult(eq(second), any(ValidationOptions.class));
        verify(attributionClient).submitPatients(bundle);
    }

    @Test
    void testBulkSubmitRejectsInvalidPatient() {
        OrganizationPrincipal principal = APITestHelpers.makeOrganizationPrincipal();
        Patient patient = new Patient();
        Bundle bundle = new Bundle();
        bundle.addEntry().setResource(patient);
        SingleValidationMessage message = new SingleValidationMessage();
        message.setSeverity(ResultSeverityEnum.ERROR);
        message.setMessage("Invalid Patient");
        ValidationResult validation = mock(ValidationResult.class);
        when(validation.getMessages()).thenReturn(List.of(message));
        when(fhirValidator.validateWithResult(eq(patient), any(ValidationOptions.class))).thenReturn(validation);

        WebApplicationException exception = assertThrows(WebApplicationException.class,
                () -> patientResource.bulkSubmitPatients(principal, parametersWithBundle(bundle)));

        assertEquals(422, exception.getResponse().getStatus());
        assertEquals("Invalid Patient", exception.getMessage());
        verifyNoInteractions(attributionClient);
    }

    @Test
    void testBulkSubmitAcceptsInformationalMessages() {
        OrganizationPrincipal principal = APITestHelpers.makeOrganizationPrincipal();
        Patient patient = new Patient();
        Bundle bundle = new Bundle();
        bundle.addEntry().setResource(patient);
        SingleValidationMessage message = new SingleValidationMessage();
        message.setSeverity(ResultSeverityEnum.INFORMATION);
        ValidationResult validation = mock(ValidationResult.class);
        when(validation.getMessages()).thenReturn(List.of(message));
        when(fhirValidator.validateWithResult(eq(patient), any(ValidationOptions.class))).thenReturn(validation);
        when(attributionClient.submitPatients(bundle)).thenReturn(bundle);

        assertSame(bundle, patientResource.bulkSubmitPatients(principal, parametersWithBundle(bundle)));
        verify(attributionClient).submitPatients(bundle);
    }

    @Test
    void testBulkSubmitIgnoresNonPatientEntries() {
        OrganizationPrincipal principal = APITestHelpers.makeOrganizationPrincipal();
        Bundle bundle = new Bundle();
        bundle.addEntry().setResource(new Practitioner());
        when(attributionClient.submitPatients(bundle)).thenReturn(bundle);

        assertSame(bundle, patientResource.bulkSubmitPatients(principal, parametersWithBundle(bundle)));
        verifyNoInteractions(fhirValidator);
    }

    @Test
    void testBulkSubmitRejectsMissingBundle() {
        Parameters params = new Parameters();

        WebApplicationException exception = assertThrows(WebApplicationException.class,
                () -> patientResource.bulkSubmitPatients(APITestHelpers.makeOrganizationPrincipal(), params));

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), exception.getResponse().getStatus());
        verifyNoInteractions(attributionClient, fhirValidator);
    }

    @Test
    void testBulkSubmitRejectsNonBundleResource() {
        Parameters params = new Parameters();
        params.addParameter().setResource(new Patient());

        WebApplicationException exception = assertThrows(WebApplicationException.class,
                () -> patientResource.bulkSubmitPatients(APITestHelpers.makeOrganizationPrincipal(), params));

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), exception.getResponse().getStatus());
        verifyNoInteractions(attributionClient, fhirValidator);
    }

    @Test
    void testBulkSubmitRejectsNullParameters() {
        WebApplicationException exception = assertThrows(WebApplicationException.class,
                () -> patientResource.bulkSubmitPatients(APITestHelpers.makeOrganizationPrincipal(), null));

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), exception.getResponse().getStatus());
        verifyNoInteractions(attributionClient, fhirValidator);
    }

    @Test
    void testGetPatient() {
        UUID patientID = UUID.randomUUID();
        Patient patient = new Patient();
        patient.setId(patientID.toString());
        when(attributionClient.getPatient(patientID)).thenReturn(patient);

        assertSame(patient, patientResource.getPatient(patientID));
        verify(attributionClient).getPatient(patientID);
    }

    @Test
    void testDeletePatient() {
        UUID patientID = UUID.randomUUID();

        Response response = patientResource.deletePatient(patientID);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        verify(attributionClient).deletePatient(patientID);
    }

    @Test
    void testUpdatePatient() {
        UUID patientID = UUID.randomUUID();
        Patient patient = new Patient();
        patient.setId(patientID.toString());
        when(attributionClient.updatePatient(patientID, patient)).thenReturn(patient);

        assertSame(patient, patientResource.updatePatient(patientID, patient));
        verify(attributionClient).updatePatient(patientID, patient);
    }

    @Test
    void testUpdatePatientNoResource() {
        UUID patientID = UUID.randomUUID();
        Patient patient = new Patient();

        WebApplicationException exception = assertThrows(WebApplicationException.class,
                () -> patientResource.updatePatient(patientID, patient));

        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), exception.getResponse().getStatus());
        assertEquals("Unable to update Patient", exception.getMessage());
        verify(attributionClient).updatePatient(patientID, patient);
    }

    @Test
    void testValidatePatient() {
        OrganizationPrincipal principal = APITestHelpers.makeOrganizationPrincipal();
        Bundle bundle = new Bundle();
        Parameters params = parametersWithBundle(bundle);
        ValidationResult validation = mock(ValidationResult.class);
        OperationOutcome outcome = new OperationOutcome();
        when(fhirValidator.validateWithResult(eq(bundle), any(ValidationOptions.class))).thenReturn(validation);
        when(validation.toOperationOutcome()).thenReturn(outcome);

        assertSame(outcome, patientResource.validatePatient(principal, params));
        verify(fhirValidator).validateWithResult(eq(bundle),
                argThat((ValidationOptions options) -> options.getProfiles().contains(PatientProfile.PROFILE_URI)));
        verifyNoInteractions(attributionClient);
    }

    @Test
    void testValidatePatientWithoutResource() {
        WebApplicationException exception = assertThrows(WebApplicationException.class,
                () -> patientResource.validatePatient(APITestHelpers.makeOrganizationPrincipal(), new Parameters()));

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), exception.getResponse().getStatus());
        verifyNoInteractions(fhirValidator, attributionClient);
    }

    private static Parameters parametersWithBundle(Bundle bundle) {
        Parameters parameters = new Parameters();
        parameters.addParameter().setResource(bundle);
        return parameters;
    }

    private static Bundle createPatientBundle(int count, UUID organizationID) {
        Bundle bundle = new Bundle();
        for (int i = 0; i < count; i++) {
            Patient patient = new Patient();
            patient.setId("Patient/patient-" + i);
            patient.setManagingOrganization(new Reference("Organization/" + organizationID));
            bundle.addEntry().setResource(patient);
        }
        return bundle;
    }
}
