package gov.cms.dpc.aggregation.bbclient;


import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.Patient;

public interface BlueButtonClient {

    public Patient requestPatientFromServer(String patientID) throws BlueButtonClientException;

    public ExplanationOfBenefit requestEOBFromServer(String patientID) throws BlueButtonClientException;
}
