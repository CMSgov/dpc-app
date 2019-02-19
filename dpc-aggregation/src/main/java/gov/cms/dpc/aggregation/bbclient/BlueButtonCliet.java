package gov.cms.dpc.aggregation.bbclient;

import org.hl7.fhir.dstu3.model.Patient;

public interface BlueButtonCliet {

    public Patient requestFhirFromServer(String BeneficiaryID) throws BlueButtonClientException;
}
