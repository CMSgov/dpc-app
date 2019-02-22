package gov.cms.dpc.aggregation.bbclient;


import org.hl7.fhir.dstu3.model.Patient;

public class MockBlueButtonClient implements BlueButtonClient {

    public MockBlueButtonClient(){

    }

    public Patient getBeneficiaryDataAsFHIR(String beneficiaryId) {
        // TODO
        return null;
    }

    public String getBeneficiaryDataAsJSON(String beneficiaryId) {
        // TODO
        return null;
    }
}
