package gov.cms.dpc.aggregation.bbclient;

import org.hl7.fhir.dstu3.model.Patient;

public class MockBlueButtonClient implements BlueButtonCliet {

    public MockBlueButtonClient(){

    }

    public Patient requestFhirFromServer(String beneficiaryId) {
        // TODO
        return null;
    }
}
