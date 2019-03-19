package gov.cms.dpc.aggregation.bbclient;


import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Patient;

public class MockBlueButtonClient implements BlueButtonClient {

    public MockBlueButtonClient(){

    }

    public Patient requestPatientFromServer(String patientID) {
        // TODO
        return null;
    }

    public Bundle requestEOBBundleFromServer(String patientID) {
        // TODO
        return null;
    }
}
