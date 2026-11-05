package gov.cms.dpc.attribution.client;

import ca.uhn.fhir.rest.api.MethodOutcome;
import gov.cms.dpc.attribution.client.search.PatientSearch;
import gov.cms.dpc.attribution.client.search.RosterSearch;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Group;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Practitioner;

import java.util.UUID;

/**
 * Client contract for the attribution service's R4 endpoints.
 *
 * <p>Searches and bulk operations return R4 {@link Bundle}s. Create operations return
 * {@link MethodOutcome}s.  Operation implementations wrap submitted resources in whatever FHIR
 * resource makes sense and deletes return nothing.  They either work or throw an exception.
 */
public interface AttributionClient {

    /**
     * Search for an {@link Organization}
     *
     * @param identifier optional FHIR identifier token.  Can be a comma separated list, or null.
     *                   Null lists all organizations.
     */
    Bundle searchOrganizations(String identifier);

    /**
     * Read an {@link Organization}
     */
    Organization getOrganization(UUID id);

    /**
     * Submit an {@link Organization}
     */
    MethodOutcome submitOrganization(Bundle bundle);

    /**
     * Overwrite an {@link Organization}
     */
    Organization updateOrganization(UUID id, Organization organization);

    /**
     * Delete an {@link Organization}
     */
    void deleteOrganization(UUID id);

    /**
     * Search for a {@link Practitioner} by their ID
     *
     * @param organizationId required organization ID
     * @param practitionerId required practitioner resource ID
     */
    Bundle searchPractitioners(UUID organizationId, UUID practitionerId);

    /**
     * Search for a {@link Practitioner} by their NPI
     *
     * @param organizationId required organization ID
     * @param providerNpi required provider NPI
     */
    Bundle searchPractitioners(UUID organizationId, String providerNpi);

    /**
     * Read a {@link Practitioner}
     */
    Practitioner getPractitioner(UUID id);

    /**
     * Create a {@link Practitioner}
     */
    MethodOutcome createPractitioner(Practitioner practitioner);

    /**
     * Submit a {@link Bundle} of {@link Practitioner}s
     */
    Bundle submitPractitioners(Bundle bundle);

    /**
     * Overwrite a {@link Practitioner}
     */
    Practitioner updatePractitioner(UUID id, Practitioner practitioner);

    /**
     * Delete a {@link Practitioner}
     */
    void deletePractitioner(UUID id);

    /**
     * Search for a {@link Patient}
     */
    Bundle searchPatients(PatientSearch query);

    /**
     * Read a {@link Patient}
     */
    Patient getPatient(UUID id);

    /**
     * Create a {@link Patient}
     */
    MethodOutcome createPatient(Patient patient);

    /**
     * Submit a {@link Bundle} of {@link Patient}s
     */
    Bundle submitPatients(Bundle bundle);

    /**
     * Overwrite a {@link Patient}
     */
    Patient updatePatient(UUID id, Patient patient);

    /**
     * Delete a {@link Patient}
     */
    void deletePatient(UUID id);

    /**
     * Search for a {@link Group}
     */
    Bundle searchRosters(RosterSearch query);

    /**
     * Read a {@link Group}
     */
    Group getRoster(UUID id);

    /**
     * Create a {@link Group}
     */
    MethodOutcome createRoster(Group group);

    /**
     * Overwrite a {@link Group}
     */
    Group replaceRoster(UUID id, Group group);

    /**
     * Delete a {@link Group}
     */
    void deleteRoster(UUID id);

    /**
     * Add {@link Patient}s to an existing {@link Group}.
     *
     * @param group {@link Group} containing the {@link Patient}s to be added
     */
    Group addRosterMembers(UUID id, Group group);

    /**
     * Remove {@link Patient}s from an existing {@link Group}.
     *
     * @param group {@link Group} containing the {@link Patient}s to be removed
     */
    Group removeRosterMembers(UUID id, Group group);

    /**
     * Return a {@link Bundle} of {@link Patient}s in the {@link Group}.  Only the MBIs will
     * be filled in.
     */
    Bundle getAttributedPatients(UUID id, boolean activeOnly);

    /**
     * Returns the attribution health endpoint's response.
     * Any exceptions get thrown to the caller.
     *
     * @throws IllegalStateException if the response is not a boolean
     */
    boolean checkHealth();

    /**
     * Returns the version of dpc-attribution
     */
    String getVersion();
}
