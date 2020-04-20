package gov.cms.dpc.aggregation.service;

import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;

import java.util.UUID;

public interface LookBackService {

    /**
     * Retrieves the ProviderID from the roster by orgID, patientMBI and either rosterID or providerID
     * @param orgID         The organizationID
     * @param ambiguousID   Either a rosterID or the providerID
     * @param patientMBI    The patient MBI
     * @return the provider ID for that roster
     */
    UUID getProviderIDFromRoster(UUID orgID, String ambiguousID, String patientMBI);

    /**
     * Checks to see if the explanation of benefits that is associated with the orgID and providerID has a claim
     * within the last withinMonths
     * @param explanationOfBenefit  The EoB
     * @param organizationID        The organizationID
     * @param providerID            The providerID
     * @param withinMonth           The limit of months to qualify for having a claim
     * @return true or false if the EoB matches the organizationID and providerID and has a claim within certain months
     */
    boolean hasClaimWithin(ExplanationOfBenefit explanationOfBenefit, UUID organizationID, UUID providerID, long withinMonth);
}
