package gov.cms.dpc.attribution.client;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.gclient.ICreateTyped;
import ca.uhn.fhir.rest.gclient.IDeleteTyped;
import ca.uhn.fhir.rest.gclient.IOperationUntypedWithInput;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.gclient.IReadExecutable;
import ca.uhn.fhir.rest.gclient.IUpdateExecutable;
import ca.uhn.fhir.rest.server.exceptions.ForbiddenOperationException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import gov.cms.dpc.attribution.client.search.RosterSearch;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Group;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiFunction;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests Group methods
 */
class AttributionClientGroupUnitTest extends AbstractAttributionClientResourceUnitTest {

    private static final UUID ORGANIZATION_ID = UUID.randomUUID();
    private static final UUID ROSTER_ID = UUID.randomUUID();

    @Test
    void testSearchRostersByOrganization() {
        RosterSearch search = new RosterSearch(null, ORGANIZATION_ID, null, null);
        IQuery<Bundle> query = mockRosterSearch(Map.of());
        Bundle bundle = new Bundle();
        when(query.execute()).thenReturn(bundle);

        assertSame(bundle, client.searchRosters(search));
        verify(query).execute();
        verifyOrganizationTag(Map.of());
    }

    @Test
    void testSearchRostersWithAllFilters() {
        String providerNpi = "attributed-to$http://hl7.org/fhir/sid/us-npi|1234567890";
        String patientReference = "Patient/" + UUID.randomUUID();
        Map<String, List<String>> filters = Map.of(
            "_id", List.of(ROSTER_ID.toString()),
            Group.SP_CHARACTERISTIC_VALUE, List.of(providerNpi),
            "member", List.of(patientReference));
        IQuery<Bundle> query = mockRosterSearch(filters);
        Bundle bundle = new Bundle();
        when(query.execute()).thenReturn(bundle);
        RosterSearch search = new RosterSearch(ROSTER_ID, ORGANIZATION_ID, providerNpi, patientReference);

        assertSame(bundle, client.searchRosters(search));
        verify(query).execute();
        verifyOrganizationTag(filters);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Patient/123", "123"})
    void testSearchRostersHandlesBothPatientReferenceFormats(String patientReference) {
        Map<String, List<String>> filters = Map.of("member", List.of(patientReference));
        IQuery<Bundle> query = mockRosterSearch(filters);
        RosterSearch search = new RosterSearch(null, ORGANIZATION_ID, null, patientReference);
        client.searchRosters(search);

        verify(query).execute();
        verifyOrganizationTag(filters);
    }

    @Test
    void testGetRoster() {
        Group group = new Group();
        group.setId(ROSTER_ID.toString());
        IReadExecutable<Group> readExec = mockRead(Group.class, ROSTER_ID);
        when(readExec.execute()).thenReturn(group);

        assertSame(group, client.getRoster(ROSTER_ID));
        verify(readExec).execute();
    }

    @Test
    void testGetRosterHandlesNotFound() {
        IReadExecutable<Group> readExec = mockRead(Group.class, ROSTER_ID);
        ResourceNotFoundException failure = new ResourceNotFoundException("Cannot find Roster resource");
        when(readExec.execute()).thenThrow(failure);

        assertSame(failure, assertThrows(ResourceNotFoundException.class, () -> client.getRoster(ROSTER_ID)));
    }

    @Test
    void testCreateRoster() {
        Group group = new Group();
        ICreateTyped createExec = mockCreate(group);
        MethodOutcome outcome = new MethodOutcome();
        outcome.setResource(group);
        when(createExec.execute()).thenReturn(outcome);

        assertSame(outcome, client.createRoster(group));
        verify(createExec).execute();
    }

    @Test
    void testCreateRosterHandlesExistingRoster() {
        Group group = new Group();
        ICreateTyped createExec = mockCreate(group);
        ForbiddenOperationException failure = new ForbiddenOperationException("Provider already has a roster");
        when(createExec.execute()).thenThrow(failure);

        assertSame(failure, assertThrows(ForbiddenOperationException.class, () -> client.createRoster(group)));
    }

    @Test
    void testReplaceRoster() {
        Group group = new Group();
        Group updated = new Group();
        updated.setId(ROSTER_ID.toString());
        MethodOutcome outcome = new MethodOutcome();
        outcome.setResource(updated);
        IUpdateExecutable updateExec = mockUpdate(ROSTER_ID, group);
        when(updateExec.execute()).thenReturn(outcome);

        assertSame(updated, client.replaceRoster(ROSTER_ID, group));
        verify(updateExec).execute();
    }

    @Test
    void testReplaceRosterNoResource() {
        Group group = new Group();
        IUpdateExecutable updateExec = mockUpdate(ROSTER_ID, group);
        when(updateExec.execute()).thenReturn(new MethodOutcome());

        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> client.replaceRoster(ROSTER_ID, group));
        assertEquals("Attribution service returned no Group after update", failure.getMessage());
    }

    @Test
    void testDeleteRoster() {
        IDeleteTyped deleteExec = mockDelete(Group.class, ROSTER_ID);

        assertDoesNotThrow(() -> client.deleteRoster(ROSTER_ID));
        verify(deleteExec).execute();
    }

    @Test
    void testDeleteRosterHandlesNotFound() {
        IDeleteTyped deleteExec = mockDelete(Group.class, ROSTER_ID);
        ResourceNotFoundException failure = new ResourceNotFoundException("Cannot find Roster resource");
        when(deleteExec.execute()).thenThrow(failure);

        assertSame(failure, assertThrows(ResourceNotFoundException.class, () -> client.deleteRoster(ROSTER_ID)));
    }

    @Test
    void testAddRosterMembers() {
        assertMembershipOperation("add", client::addRosterMembers);
    }

    @Test
    void testRemoveRosterMembers() {
        assertMembershipOperation("remove", client::removeRosterMembers);
    }

    @Test
    void testAddRosterHandlesException() {
        Group group = new Group();
        IOperationUntypedWithInput<Group> operationExec =
                mockInstanceOperation(Group.class, ROSTER_ID, "add", Group.class);
        UnprocessableEntityException failure = new UnprocessableEntityException("Invalid members");
        when(operationExec.execute()).thenThrow(failure);

        assertSame(failure, assertThrows(UnprocessableEntityException.class,
                () -> client.addRosterMembers(ROSTER_ID, group)));
    }

    @Test
    void testRemoveRosterHandlesException() {
        Group group = new Group();
        IOperationUntypedWithInput<Group> operationExec =
                mockInstanceOperation(Group.class, ROSTER_ID, "remove", Group.class);
        ResourceNotFoundException failure = new ResourceNotFoundException("Cannot find Roster resource");
        when(operationExec.execute()).thenThrow(failure);

        assertSame(failure, assertThrows(ResourceNotFoundException.class,
                () -> client.removeRosterMembers(ROSTER_ID, group)));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testGetAttributedPatients(boolean activeOnly) {
        IOperationUntypedWithInput<Bundle> operationExec =
                mockInstanceOperation(Group.class, ROSTER_ID, "patients", Bundle.class);
        Bundle bundle = new Bundle();
        Patient patient = new Patient();
        patient.addIdentifier().setSystem(DPCIdentifierSystem.MBI.getSystem()).setValue("3aa0C00aA00");
        bundle.addEntry().setResource(patient);
        when(operationExec.execute()).thenReturn(bundle);

        assertSame(bundle, client.getAttributedPatients(ROSTER_ID, activeOnly));
        Parameters parameters = captureParameters("patients");
        assertEquals(1, parameters.getParameter().size());
        assertEquals("active", parameters.getParameterFirstRep().getName());
        assertEquals(activeOnly, ((BooleanType) parameters.getParameterFirstRep().getValue()).booleanValue());
        verify(operationExec).useHttpGet();
        verify(operationExec).encodedJson();
        verify(operationExec).execute();
    }

    @Test
    void testPatientsOperationHandlesException() {
        IOperationUntypedWithInput<Bundle> operationExec =
                mockInstanceOperation(Group.class, ROSTER_ID, "patients", Bundle.class);
        ResourceNotFoundException failure = new ResourceNotFoundException("Cannot find Roster resource");
        when(operationExec.execute()).thenThrow(failure);

        assertSame(failure, assertThrows(ResourceNotFoundException.class,
                () -> client.getAttributedPatients(ROSTER_ID, true)));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testGetConsentingAttributedPatientsFiltersEntries(boolean activeOnly) {
        Patient permitted = createPatient("3aa0C00aA00");   // Consenting patient
        Patient optedOut = createPatient("3aa0C00aA01");    // Opted out patient
        Bundle attributed = new Bundle();
        attributed.setType(Bundle.BundleType.SEARCHSET);
        Bundle.BundleEntryComponent permittedEntry = attributed.addEntry()
                .setResource(permitted).setFullUrl("Patient/permitted");
        attributed.addEntry().setResource(optedOut);
        attributed.addEntry().setResource(new Organization());  // Add org to the bundle
        attributed.addEntry();                                  // Add an empty entry to the bundle
        attributed.setTotal(4);
        mockAttributedPatients(attributed);
        when(consentService.filterConsent(List.of(permitted, optedOut))).thenReturn(List.of(permitted));

        Bundle result = client.getConsentingAttributedPatients(ROSTER_ID, activeOnly);

        assertSame(attributed, result);
        assertEquals(List.of(permittedEntry), result.getEntry());
        assertSame(permittedEntry, result.getEntryFirstRep());
        assertSame(permitted, result.getEntryFirstRep().getResource());
        assertEquals("Patient/permitted", result.getEntryFirstRep().getFullUrl());
        assertEquals(1, result.getTotal());
        assertEquals(Bundle.BundleType.SEARCHSET, result.getType());
        verify(consentService).filterConsent(List.of(permitted, optedOut));
        Parameters parameters = captureParameters("patients");
        assertEquals(activeOnly, ((BooleanType) parameters.getParameterFirstRep().getValue()).booleanValue());
    }

    @Test
    void testGetConsentingAttributedPatientsSavesOrderAndDuplicates() {
        Patient first = createPatient("3aa0C00aA00");
        Patient second = createPatient("3aa0C00aA01");
        Bundle attributed = new Bundle();
        Bundle.BundleEntryComponent firstEntry = attributed.addEntry().setResource(first);
        Bundle.BundleEntryComponent secondEntry = attributed.addEntry().setResource(second);
        Bundle.BundleEntryComponent duplicateEntry = attributed.addEntry().setResource(first);
        mockAttributedPatients(attributed);
        when(consentService.filterConsent(List.of(first, second, first))).thenReturn(List.of(second, first));

        Bundle result = client.getConsentingAttributedPatients(ROSTER_ID, true);

        assertEquals(List.of(firstEntry, secondEntry, duplicateEntry), result.getEntry());
        assertFalse(result.hasTotal());
    }

    @Test
    void testGetConsentingAttributedPatientsWithNoConsent() {
        Patient patient = createPatient("3aa0C00aA00");
        Bundle attributed = new Bundle();
        attributed.addEntry().setResource(patient);
        attributed.setTotal(1);
        mockAttributedPatients(attributed);
        when(consentService.filterConsent(List.of(patient))).thenReturn(List.of());

        Bundle result = client.getConsentingAttributedPatients(ROSTER_ID, true);

        assertTrue(result.getEntry().isEmpty());
        assertEquals(0, result.getTotal());
    }

    @Test
    void testGetConsentingAttributedPatientsWithEmptyGroup() {
        Bundle attributed = new Bundle();
        mockAttributedPatients(attributed);
        when(consentService.filterConsent(List.of())).thenReturn(List.of());

        Bundle result = client.getConsentingAttributedPatients(ROSTER_ID, false);

        assertSame(attributed, result);
        assertTrue(result.getEntry().isEmpty());
        assertFalse(result.hasTotal());
        verify(consentService).filterConsent(List.of());
    }

    @Test
    void testGetConsentingAttributedPatientsHandlesExceptionOnConsent() {
        Patient patient = createPatient("3aa0C00aA00");
        Bundle attributed = new Bundle();
        Bundle.BundleEntryComponent entry = attributed.addEntry().setResource(patient);
        attributed.setTotal(1);
        mockAttributedPatients(attributed);
        IllegalStateException failure = new IllegalStateException("Consent lookup failed");
        when(consentService.filterConsent(List.of(patient))).thenThrow(failure);

        assertSame(failure, assertThrows(IllegalStateException.class,
                () -> client.getConsentingAttributedPatients(ROSTER_ID, true)));

        // Check that bundle hadn't been changed
        assertEquals(List.of(entry), attributed.getEntry());
        assertEquals(1, attributed.getTotal());
    }

    @Test
    void testGetConsentingAttributedPatientsHandlesExceptionOnAttribution() {
        IOperationUntypedWithInput<Bundle> operationExec =
                mockInstanceOperation(Group.class, ROSTER_ID, "patients", Bundle.class);
        ResourceNotFoundException failure = new ResourceNotFoundException("Cannot find Roster resource");
        when(operationExec.execute()).thenThrow(failure);

        assertSame(failure, assertThrows(ResourceNotFoundException.class,
                () -> client.getConsentingAttributedPatients(ROSTER_ID, true)));
        verifyNoInteractions(consentService);
    }

    private IOperationUntypedWithInput<Bundle> mockAttributedPatients(Bundle attributed) {
        IOperationUntypedWithInput<Bundle> operationExec =
                mockInstanceOperation(Group.class, ROSTER_ID, "patients", Bundle.class);
        when(operationExec.execute()).thenReturn(attributed);
        return operationExec;
    }

    private Patient createPatient(String mbi) {
        Patient patient = new Patient();
        patient.addIdentifier().setSystem(DPCIdentifierSystem.MBI.getSystem()).setValue(mbi);
        return patient;
    }

    private IQuery<Bundle> mockRosterSearch(Map<String, List<String>> filters) {
        return mockSearch(Group.class, filters, DPCIdentifierSystem.DPC.getSystem(), ORGANIZATION_ID.toString());
    }

    private void verifyOrganizationTag(Map<String, List<String>> filters) {
        verify(attributionClient.search().forResource(Group.class).encodedJson().returnBundle(Bundle.class).whereMap(filters))
                .withTag(DPCIdentifierSystem.DPC.getSystem(), ORGANIZATION_ID.toString());
    }

    private void assertMembershipOperation(String operationName, BiFunction<UUID, Group, Group> action) {
        Group update = new Group();
        update.addMember().getEntity().setReference("Patient/" + UUID.randomUUID());
        Group result = new Group();
        IOperationUntypedWithInput<Group> operationExec =
                mockInstanceOperation(Group.class, ROSTER_ID, operationName, Group.class);
        when(operationExec.execute()).thenReturn(result);

        assertSame(result, action.apply(ROSTER_ID, update));
        Parameters parameters = captureParameters(operationName);
        assertEquals(1, parameters.getParameter().size());
        assertSame(update, parameters.getParameterFirstRep().getResource());
        verify(operationExec, never()).useHttpGet();
        verify(operationExec).encodedJson();
        verify(operationExec).execute();
    }

    private Parameters captureParameters(String operationName) {
        ArgumentCaptor<Parameters> parameters = ArgumentCaptor.forClass(Parameters.class);
        verify(attributionClient.operation().onInstance(new IdType("Group", ROSTER_ID.toString()))
                .named(operationName)).withParameters(parameters.capture());
        return parameters.getValue();
    }
}
