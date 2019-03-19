package gov.cms.dpc.aggregation.bbclient;


import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Patient;

public interface BlueButtonClient {

    public Patient requestPatientFromServer(String patientID) throws BlueButtonClientException;

    public Bundle requestEOBBundleFromServer(String patientID) throws BlueButtonClientException;
}
