package gov.cms.dpc.aggregation.bbclient;


import org.hl7.fhir.dstu3.model.Patient;

public interface BlueButtonClient {

    public Patient getBeneficiaryDataAsFHIR(String BeneficiaryID);

    public String getBeneficiaryDataAsJSON(String BeneficiaryID);
}
