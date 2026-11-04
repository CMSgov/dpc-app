package gov.cms.dpc.attribution.client;

import ca.uhn.fhir.context.FhirContext;
import gov.cms.dpc.attribution.client.search.PatientSearch;
import gov.cms.dpc.attribution.client.search.RosterSearch;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Group;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AttributionClientImplUnitTest {

    private static final FhirContext FHIR_CONTEXT = FhirContext.forR4();
    private static final UUID ORGANIZATION_ID = UUID.randomUUID();
    private static final UUID RESOURCE_ID = UUID.randomUUID();

    private final AttributionClient client = new AttributionClientImpl(
            FHIR_CONTEXT.newRestfulGenericClient("http://localhost:3500/r4/"));

    @Test
    void searchPatientsThrowsUnsupportedOperationException() {
        assertNotImplemented(() -> client.searchPatients(new PatientSearch(RESOURCE_ID, null, ORGANIZATION_ID, null, null, null)));
    }

    @Test
    void getPatientThrowsUnsupportedOperationException() {
        assertNotImplemented(() -> client.getPatient(RESOURCE_ID));
    }

    @Test
    void createPatientThrowsUnsupportedOperationException() {
        assertNotImplemented(() -> client.createPatient(new Patient()));
    }

    @Test
    void submitPatientsThrowsUnsupportedOperationException() {
        assertNotImplemented(() -> client.submitPatients(new Bundle()));
    }

    @Test
    void updatePatientThrowsUnsupportedOperationException() {
        assertNotImplemented(() -> client.updatePatient(RESOURCE_ID, new Patient()));
    }

    @Test
    void deletePatientThrowsUnsupportedOperationException() {
        assertNotImplemented(() -> client.deletePatient(RESOURCE_ID));
    }

    @Test
    void searchRostersThrowsUnsupportedOperationException() {
        assertNotImplemented(() -> client.searchRosters(new RosterSearch(RESOURCE_ID, ORGANIZATION_ID, null, null)));
    }

    @Test
    void getRosterThrowsUnsupportedOperationException() {
        assertNotImplemented(() -> client.getRoster(RESOURCE_ID));
    }

    @Test
    void createRosterThrowsUnsupportedOperationException() {
        assertNotImplemented(() -> client.createRoster(new Group()));
    }

    @Test
    void replaceRosterThrowsUnsupportedOperationException() {
        assertNotImplemented(() -> client.replaceRoster(RESOURCE_ID, new Group()));
    }

    @Test
    void deleteRosterThrowsUnsupportedOperationException() {
        assertNotImplemented(() -> client.deleteRoster(RESOURCE_ID));
    }

    @Test
    void addRosterMembersThrowsUnsupportedOperationException() {
        assertNotImplemented(() -> client.addRosterMembers(RESOURCE_ID, new Group()));
    }

    @Test
    void removeRosterMembersThrowsUnsupportedOperationException() {
        assertNotImplemented(() -> client.removeRosterMembers(RESOURCE_ID, new Group()));
    }

    @Test
    void getAttributedPatientsThrowsUnsupportedOperationException() {
        assertNotImplemented(() -> client.getAttributedPatients(RESOURCE_ID, true));
    }

    @Test
    void checkHealthThrowsUnsupportedOperationException() {
        assertNotImplemented(client::checkHealth);
    }

    @Test
    void getVersionThrowsUnsupportedOperationException() {
        assertNotImplemented(client::getVersion);
    }

    private static void assertNotImplemented(Executable invocation) {
        UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class, invocation);
        assertEquals("Method not implemented yet.", exception.getMessage());
    }
}
