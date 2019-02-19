package gov.cms.dpc.aggregation.bbclient;

import org.hl7.fhir.dstu3.model.Bundle;

public class MockBlueButtonClient implements BlueButtonCliet {

    public MockBlueButtonClient(){

    }

    public Bundle requestFhirBundle(String beneficiaryId) {
        // TODO
        return null;
    }
}
