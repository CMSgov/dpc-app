package gov.cms.dpc.attribution.client;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import gov.cms.dpc.attribution.client.search.PatientSearch;
import gov.cms.dpc.attribution.client.search.RosterSearch;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Group;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Practitioner;

import java.util.Objects;
import java.util.UUID;

/**
 * Placeholder implementation of {@link AttributionClient}.
 */
public class AttributionClientImpl implements AttributionClient {

    private final IGenericClient client;

    public AttributionClientImpl(IGenericClient client) {
        this.client = Objects.requireNonNull(client);
    }

    @Override
    public Bundle searchOrganizations(String identifier) {
        throw new UnsupportedOperationException("Method not implemented yet.");
    }

    @Override
    public Organization getOrganization(UUID id) {
        throw new UnsupportedOperationException("Method not implemented yet.");
    }

    @Override
    public MethodOutcome submitOrganization(Bundle bundle) {
        throw new UnsupportedOperationException("Method not implemented yet.");
    }

    @Override
    public Organization updateOrganization(UUID id, Organization organization) {
        throw new UnsupportedOperationException("Method not implemented yet.");
    }

    @Override
    public void deleteOrganization(UUID id) {
        throw new UnsupportedOperationException("Method not implemented yet.");
    }

    @Override
    public Bundle searchPractitioners(UUID organizationId, UUID practitionerId) {
        throw new UnsupportedOperationException("Method not implemented yet.");
    }

    @Override
    public Bundle searchPractitioners(UUID organizationId, String providerNpi) {
        throw new UnsupportedOperationException("Method not implemented yet.");
    }

    @Override
    public Practitioner getPractitioner(UUID id) {
        throw new UnsupportedOperationException("Method not implemented yet.");
    }

    @Override
    public MethodOutcome createPractitioner(Practitioner practitioner) {
        throw new UnsupportedOperationException("Method not implemented yet.");
    }

    @Override
    public Bundle submitPractitioners(Bundle bundle) {
        throw new UnsupportedOperationException("Method not implemented yet.");
    }

    @Override
    public Practitioner updatePractitioner(UUID id, Practitioner practitioner) {
        throw new UnsupportedOperationException("Method not implemented yet.");
    }

    @Override
    public void deletePractitioner(UUID id) {
        throw new UnsupportedOperationException("Method not implemented yet.");
    }

    @Override
    public Bundle searchPatients(PatientSearch query) {
        throw new UnsupportedOperationException("Method not implemented yet.");
    }

    @Override
    public Patient getPatient(UUID id) {
        throw new UnsupportedOperationException("Method not implemented yet.");
    }

    @Override
    public MethodOutcome createPatient(Patient patient) {
        throw new UnsupportedOperationException("Method not implemented yet.");
    }

    @Override
    public Bundle submitPatients(Bundle bundle) {
        throw new UnsupportedOperationException("Method not implemented yet.");
    }

    @Override
    public Patient updatePatient(UUID id, Patient patient) {
        throw new UnsupportedOperationException("Method not implemented yet.");
    }

    @Override
    public void deletePatient(UUID id) {
        throw new UnsupportedOperationException("Method not implemented yet.");
    }

    @Override
    public Bundle searchRosters(RosterSearch query) {
        throw new UnsupportedOperationException("Method not implemented yet.");
    }

    @Override
    public Group getRoster(UUID id) {
        throw new UnsupportedOperationException("Method not implemented yet.");
    }

    @Override
    public MethodOutcome createRoster(Group group) {
        throw new UnsupportedOperationException("Method not implemented yet.");
    }

    @Override
    public Group replaceRoster(UUID id, Group group) {
        throw new UnsupportedOperationException("Method not implemented yet.");
    }

    @Override
    public void deleteRoster(UUID id) {
        throw new UnsupportedOperationException("Method not implemented yet.");
    }

    @Override
    public Group addRosterMembers(UUID id, Group group) {
        throw new UnsupportedOperationException("Method not implemented yet.");
    }

    @Override
    public Group removeRosterMembers(UUID id, Group group) {
        throw new UnsupportedOperationException("Method not implemented yet.");
    }

    @Override
    public Bundle getAttributedPatients(UUID id, boolean activeOnly) {
        throw new UnsupportedOperationException("Method not implemented yet.");
    }

    @Override
    public boolean checkHealth() {
        throw new UnsupportedOperationException("Method not implemented yet.");
    }

    @Override
    public String getVersion() {
        throw new UnsupportedOperationException("Method not implemented yet.");
    }
}
