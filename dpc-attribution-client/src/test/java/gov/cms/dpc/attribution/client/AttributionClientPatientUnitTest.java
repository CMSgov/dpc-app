package gov.cms.dpc.attribution.client;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.SummaryEnum;
import ca.uhn.fhir.rest.gclient.*;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import gov.cms.dpc.attribution.client.search.PatientSearch;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.ArgumentCaptor;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test Patient methods
 */
class AttributionClientPatientUnitTest extends AbstractAttributionClientResourceUnitTest {

    private static final FhirContext FHIR_CONTEXT = FhirContext.forR4();
    private static final UUID ORGANIZATION_ID = UUID.randomUUID();

    @Test
    void testPatientSearch() {
        String mbi = "3aa0C00aA00";
        PatientSearch search = new PatientSearch(null, mbi, ORGANIZATION_ID, null, null, null);
        IQuery<Bundle> query = mockSearch(Patient.class);
        Bundle bundle = new Bundle();
        bundle.addEntry().setResource(new Patient());
        when(query.execute()).thenReturn(bundle);

        assertSame(bundle, client.searchPatients(search));
        verifyOrganizationFilter();
        verify(query).where(argThat((ICriterion<?> criterion) ->
                matchesCriterion(criterion, "identifier", DPCIdentifierSystem.MBI.getSystem() + "|" + mbi)));
        verify(query, never()).count(anyInt());
        verify(query, never()).offset(anyInt());
        verify(query, never()).summaryMode(any());
    }

    @ParameterizedTest
    @NullAndEmptySource
    void testPatientSearchNoIdentifier(String mbi) {
        PatientSearch search = new PatientSearch(null, mbi, ORGANIZATION_ID, null, null, null);
        IQuery<Bundle> query = mockSearch(Patient.class);
        Bundle bundle = new Bundle();
        when(query.execute()).thenReturn(bundle);

        assertSame(bundle, client.searchPatients(search));
        verifyOrganizationFilter();
        verify(query, never()).where(any(ICriterion.class));
    }

    @Test
    void testPatientSearchWithMbiToken() {
        String token = DPCIdentifierSystem.MBI.getSystem() + "|3aa0C00aA00";
        PatientSearch search = new PatientSearch(null, token, ORGANIZATION_ID, null, null, null);
        IQuery<Bundle> query = mockSearch(Patient.class);
        client.searchPatients(search);

        verify(query).where(argThat((ICriterion<?> criterion) ->
                matchesCriterion(criterion, "identifier", token)));
    }

    @Test
    void testPatientSearchById() {
        UUID patientId = UUID.randomUUID();
        PatientSearch search = new PatientSearch(patientId, null, ORGANIZATION_ID, null, null, null);
        IQuery<Bundle> query = mockSearch(Patient.class);
        client.searchPatients(search);

        verifyOrganizationFilter();
        verify(query).where(argThat((ICriterion<?> criterion) ->
                matchesCriterion(criterion, "_id", patientId.toString())));
    }

    @Test
    void testPatientSearchPaging() {
        PatientSearch search = new PatientSearch(null, null, ORGANIZATION_ID, 10, 20, null);
        IQuery<Bundle> query = mockSearch(Patient.class);
        Bundle bundle = new Bundle();
        bundle.addLink().setRelation("next").setUrl("/r4/Patient?_count=10&_offset=30");
        when(query.execute()).thenReturn(bundle);

        assertSame(bundle, client.searchPatients(search));
        verify(query).count(10);
        verify(query).offset(20);
        assertEquals("/r4/Patient?_count=10&_offset=30", bundle.getLink("next").getUrl());
    }

    @Test
    void testNegativeCount() {
        PatientSearch search = new PatientSearch(null, null, ORGANIZATION_ID, -1, null, null);

        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
            () -> client.searchPatients(search));
        assertEquals("Parameter _count must be >= 0", failure.getMessage());
        verifyNoInteractions(attributionClient);
    }

    @Test
    void testNegativeOffset() {
        PatientSearch search = new PatientSearch(null, null, ORGANIZATION_ID, null, -1, null);

        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
            () -> client.searchPatients(search));
        assertEquals("Parameter _offset must be >= 0", failure.getMessage());
        verifyNoInteractions(attributionClient);
    }

    @Test
    void testZeroCountCausesSummary() {
        PatientSearch search = new PatientSearch(null, null, ORGANIZATION_ID, 0, null, null);
        IQuery<Bundle> query = mockSearch(Patient.class);
        Bundle summary = new Bundle();
        summary.setTotal(600);
        when(query.execute()).thenReturn(summary);

        assertSame(summary, client.searchPatients(search));
        verify(query).count(0);
        verify(query).summaryMode(SummaryEnum.COUNT);
    }

    @Test
    void testExplicitSummary() {
        PatientSearch search = new PatientSearch(null, null, ORGANIZATION_ID, null, null, "count");
        IQuery<Bundle> query = mockSearch(Patient.class);
        client.searchPatients(search);

        verify(query).summaryMode(SummaryEnum.COUNT);
        verify(query, never()).count(anyInt());
    }

    @Test
    void testOrganizationRequired() {
        PatientSearch search = new PatientSearch(UUID.randomUUID(), null, null, null, null, null);

        assertThrows(NullPointerException.class, () -> client.searchPatients(search));
        verifyNoInteractions(attributionClient);
    }

    @Test
    void testGetPatient() {
        UUID patientId = UUID.randomUUID();
        Patient patient = new Patient();
        patient.setId(patientId.toString());
        IReadExecutable<Patient> readExec = mockRead(Patient.class, patientId);
        when(readExec.execute()).thenReturn(patient);

        assertSame(patient, client.getPatient(patientId));
        verify(readExec).execute();
    }

    @Test
    void testGetPatientHandlesNotFound() {
        UUID patientId = UUID.randomUUID();
        IReadExecutable<Patient> readExec = mockRead(Patient.class, patientId);
        ResourceNotFoundException failure = new ResourceNotFoundException("Patient is not registered");
        when(readExec.execute()).thenThrow(failure);

        assertSame(failure, assertThrows(ResourceNotFoundException.class,
                () -> client.getPatient(patientId)));
    }

    @Test
    void testCreatePatient() {
        Patient patient = new Patient();
        ICreateTyped createExec = mockCreate(patient);
        MethodOutcome outcome = new MethodOutcome();
        outcome.setResource(patient);
        when(createExec.execute()).thenReturn(outcome);

        assertSame(outcome, client.createPatient(patient));
        verify(createExec).execute();
    }

    @Test
    void testCreatePatientThrowsHapiException() {
        Patient patient = new Patient();
        ICreateTyped createExec = mockCreate(patient);
        UnprocessableEntityException failure = new UnprocessableEntityException("Invalid Patient resource");
        when(createExec.execute()).thenThrow(failure);

        assertSame(failure, assertThrows(UnprocessableEntityException.class,
                () -> client.createPatient(patient)));
    }

    @Test
    void testSubmitPatients() {
        Bundle bundle = new Bundle();
        bundle.addEntry().setResource(new Patient());
        Bundle result = new Bundle();
        result.setType(Bundle.BundleType.COLLECTION);
        IOperationUntypedWithInput<Bundle> submitExec = mockSubmit(Patient.class, Bundle.class);
        when(submitExec.execute()).thenReturn(result);

        assertSame(result, client.submitPatients(bundle));
        ArgumentCaptor<Parameters> parameters = ArgumentCaptor.forClass(Parameters.class);
        verify(attributionClient.operation().onType(Patient.class).named("submit"))
                .withParameters(parameters.capture());
        assertEquals(1, parameters.getValue().getParameter().size());
        assertSame(bundle, parameters.getValue().getParameterFirstRep().getResource());
        verify(submitExec).execute();
    }

    @Test
    void testSubmitPatientsThrowsHapiException() {
        Bundle bundle = new Bundle();
        IOperationUntypedWithInput<Bundle> submitExec = mockSubmit(Patient.class, Bundle.class);
        UnprocessableEntityException failure = new UnprocessableEntityException("No patients submitted");
        when(submitExec.execute()).thenThrow(failure);

        assertSame(failure, assertThrows(UnprocessableEntityException.class,
                () -> client.submitPatients(bundle)));
    }

    @Test
    void testUpdatePatient() {
        UUID patientId = UUID.randomUUID();
        Patient patient = new Patient();
        Patient updatedPatient = new Patient();
        updatedPatient.setId(patientId.toString());
        MethodOutcome outcome = new MethodOutcome();
        outcome.setResource(updatedPatient);
        IUpdateExecutable updateExec = mockUpdate(patientId, patient);
        when(updateExec.execute()).thenReturn(outcome);

        assertSame(updatedPatient, client.updatePatient(patientId, patient));
        verify(updateExec).execute();
    }

    @Test
    void testUpdatePatientNoResource() {
        UUID patientId = UUID.randomUUID();
        Patient patient = new Patient();
        IUpdateExecutable updateExec = mockUpdate(patientId, patient);
        when(updateExec.execute()).thenReturn(new MethodOutcome());

        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> client.updatePatient(patientId, patient));
        assertEquals("Attribution service returned no Patient after update", failure.getMessage());
    }

    @Test
    void testDeletePatient() {
        UUID patientId = UUID.randomUUID();
        IDeleteTyped deleteExec = mockDelete(Patient.class, patientId);

        assertDoesNotThrow(() -> client.deletePatient(patientId));
        verify(deleteExec).execute();
    }

    @Test
    void testDeletePatientHandlesNotFound() {
        UUID patientId = UUID.randomUUID();
        IDeleteTyped deleteExec = mockDelete(Patient.class, patientId);
        ResourceNotFoundException failure = new ResourceNotFoundException("Patient is not registered");
        when(deleteExec.execute()).thenThrow(failure);

        assertSame(failure, assertThrows(ResourceNotFoundException.class,
                () -> client.deletePatient(patientId)));
    }

    private void verifyOrganizationFilter() {
        verify(attributionClient.search().forResource(Patient.class).encodedJson()).where(
            argThat((ICriterion<?> criterion) ->
                matchesCriterion(criterion, "organization", ORGANIZATION_ID.toString())));
    }

    private static boolean matchesCriterion(ICriterion<?> criterion, String name, String value) {
        ICriterionInternal parameter = (ICriterionInternal) criterion;
        return name.equals(parameter.getParameterName()) && value.equals(parameter.getParameterValue(FHIR_CONTEXT));
    }
}
