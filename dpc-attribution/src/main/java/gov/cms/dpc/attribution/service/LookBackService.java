package gov.cms.dpc.attribution.service;

import java.util.UUID;

@FunctionalInterface
public interface LookBackService {

    boolean isValidProviderPatientRelation(UUID organizationID, UUID patientID, UUID providerID, long withinMonth);
}
