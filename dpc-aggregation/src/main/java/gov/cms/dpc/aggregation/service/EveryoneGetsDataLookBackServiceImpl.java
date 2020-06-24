package gov.cms.dpc.aggregation.service;

import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;

import java.util.UUID;

/**
 * This class is used in environments that need to disable look back
 */
public class EveryoneGetsDataLookBackServiceImpl implements LookBackService {

    @Override
    public String getProviderNPIFromRoster(UUID orgID, String providerOrRosterID, String patientMBI) {
        return UUID.randomUUID().toString();
    }

    @Override
    public boolean hasClaimWithin(ExplanationOfBenefit explanationOfBenefit, UUID organizationID, String providerNPI, long withinMonth) {
        return true;
    }
}
