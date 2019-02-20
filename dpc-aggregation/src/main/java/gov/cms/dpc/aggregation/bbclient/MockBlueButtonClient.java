package gov.cms.dpc.aggregation.bbclient;


import org.hl7.fhir.dstu3.model.Patient;

public class MockBlueButtonClient implements BlueButtonClient {

    public MockBlueButtonClient(){

    }

    public Patient requestFHIRFromServer(String beneficiaryId) {
        // TODO
        return null;
    }
}
