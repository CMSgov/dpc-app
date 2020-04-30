package gov.cms.dpc.aggregation.service;

import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;

import java.util.UUID;

/**
 * This class is used in environments that need to disable look back
 */
public class EveryoneGetsDataLookBackServiceImpl implements LookBackService {

    @Override
    public UUID getProviderIDFromRoster(UUID orgID, String providerOrRosterID, String patientMBI) {
        return UUID.randomUUID();
    }

    @Override
    public boolean hasClaimWithin(ExplanationOfBenefit explanationOfBenefit, UUID organizationID, UUID providerID, long withinMonth) {
        return true;
    }
}
