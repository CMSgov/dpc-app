package gov.cms.dpc.attribution;

import java.util.Set;

public interface AttributionEngine {

    Set<String> getAttributedBeneficiaries(String providerID);

    void addAttributionRelationship(String providerID, String beneficiaryID);

    void removeAttributionRelationship(String providerID, String beneficiaryID);
}
