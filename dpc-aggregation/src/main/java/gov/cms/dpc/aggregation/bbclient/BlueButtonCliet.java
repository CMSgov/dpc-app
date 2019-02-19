package gov.cms.dpc.aggregation.bbclient;

import org.hl7.fhir.dstu3.model.Bundle;

public interface BlueButtonCliet {

    public Bundle requestFhirBundle(String BeneficiaryID) throws BlueButtonClientException;
}
