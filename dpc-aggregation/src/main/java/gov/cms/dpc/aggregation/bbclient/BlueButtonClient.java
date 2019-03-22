package gov.cms.dpc.aggregation.bbclient;


import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Patient;

public interface BlueButtonClient {

    public Patient requestPatientFromServer(String patientID) throws ResourceNotFoundException;

    public Bundle requestEOBBundleFromServer(String patientID) throws ResourceNotFoundException;
}
