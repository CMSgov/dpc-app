package gov.cms.dpc.common.interfaces;

import org.hl7.fhir.dstu3.model.Bundle;

import java.util.Optional;
import java.util.Set;

public interface AttributionEngine {

    Optional<Set<String>> getAttributedBeneficiaries(String providerID);

    void addAttributionRelationship(String providerID, String beneficiaryID);

    void addAttributionRelationships(Bundle attributionBundle);

    void removeAttributionRelationship(String providerID, String beneficiaryID);

    boolean isAttributed(String providerID, String beneficiaryID);
}
