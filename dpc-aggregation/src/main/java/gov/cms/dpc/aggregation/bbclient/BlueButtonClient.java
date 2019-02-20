package gov.cms.dpc.aggregation.bbclient;


import org.hl7.fhir.dstu3.model.Patient;

public interface BlueButtonClient {

    public Patient requestFHIRFromServer(String BeneficiaryID) throws BlueButtonClientException;
}
