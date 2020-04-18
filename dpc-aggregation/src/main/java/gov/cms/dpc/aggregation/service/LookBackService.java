package gov.cms.dpc.aggregation.service;

import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;

import java.util.UUID;

public interface LookBackService {

    UUID getProviderIDFromRoster(UUID orgID, String rosterID, String patientMBI);

    boolean hasClaimWithin(ExplanationOfBenefit explanationOfBenefit, UUID organizationID, UUID providerID, long withinMonth);
}
