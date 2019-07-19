package gov.cms.dpc.common.interfaces;

import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Practitioner;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The Attribution Engine manages relationships between providers (represented as {@link Practitioner} resources) and {@link Patient}.
 * Currently, relationships must be expressed through a patient Roster (represented as a generic {@link Bundle} resource).
 * Eventually, we'll add support for analyzing claims patterns to determine whether or not an attribution relationship exists.
 */
public interface AttributionEngine {

    /**
     * Returns the MBI (Medicare Beneficiary ID) for the {@link org.hl7.fhir.dstu3.model.Patient} resources for the given {@link org.hl7.fhir.dstu3.model.Practitioner}.
     * If no patients are attributed, an empty {@link Optional} is returned.
     *
     * @param provider - {@link Practitioner} provider to retrieve attributed patients for
     * @return - {@link Optional} {@link List} of patient MBIs
     */
    Optional<List<String>> getAttributedPatientIDs(Practitioner provider);

    /**
     * Create an attribution relationship between the given provider and patient.
     *
     * @param provider - {@link Practitioner} provider to associate patient with
     * @param patient  - {@link Patient} patient to associate to provider
     */
    void addAttributionRelationship(Practitioner provider, Patient patient);

    /**
     * Submit a {@link Bundle} Roster resource which contains a {@link Practitioner} and a set of {@link Patient} resources to associate to the given {@link Practitioner}.
     * Currently, this is a write only operation, which means the previous attributions for the given {@link Practitioner} are removed and the newly supplied Roster is used.
     * The input format assumes that the {@link Bundle#getEntryFirstRep()} call returns a {@link Practitioner} resource and subsequent {@link org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent} are of type {@link Patient}.
     *
     * @param attributionBundle - {@link Bundle} which contains Roster information
     * @param organizationID    - {@link UUID} of {@link org.hl7.fhir.dstu3.model.Organization} to associate with attribution
     */
    void addAttributionRelationships(Bundle attributionBundle, UUID organizationID);

    /**
     * Remove an attribution relationship between the given provider and patient.
     *
     * @param provider - {@link Practitioner} to remove attribution relationship from
     * @param patient  - {@link Patient} to remove from provder's attribution list
     */
    void removeAttributionRelationship(Practitioner provider, Patient patient);

    /**
     * Determine if the given {@link Patient} is attributed to the {@link Practitioner}.
     *
     * @param provider - {@link Practitioner} to determine attribution relationship
     * @param patient  - {@link Patient} to verify is attributed to the given provider
     * @return - {@code true} patient is attributed to the provider. {@code false} patient is not attributed to the provider.
     */
    boolean isAttributed(Practitioner provider, Patient patient);

    /**
     * Determine if the {@link AttributionEngine} is accessible.
     * If the engine is accessible, the method will return successfully. Otherwise, it will thrown an error is there's a problem.
     * Note: This is not a full Healthcheck, which is handled by the dpc-attribution service itself, it just checks for a valid response.
     */
    void assertHealthy();
}
