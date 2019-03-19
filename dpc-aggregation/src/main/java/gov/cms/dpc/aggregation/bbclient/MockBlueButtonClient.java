package gov.cms.dpc.aggregation.bbclient;


import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.Patient;

public class MockBlueButtonClient implements BlueButtonClient {

    public MockBlueButtonClient(){

    }

    public Patient requestPatientFromServer(String patientID) {
        // TODO
        return null;
    }

    public ExplanationOfBenefit requestEOBFromServer(String patientID) {
        // TODO
        return null;
    }
}
