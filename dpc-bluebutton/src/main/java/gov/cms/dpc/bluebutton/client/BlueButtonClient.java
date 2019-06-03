package gov.cms.dpc.bluebutton.client;


import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import io.reactivex.Observable;
import org.hl7.fhir.dstu3.model.*;


public interface BlueButtonClient {

    Patient requestPatientFromServer(String patientID) throws ResourceNotFoundException;

    Bundle requestEOBBundleFromServer(String patientID) throws ResourceNotFoundException;

    Observable<Coverage> requestCoverageFromServer(String patientID) throws ResourceNotFoundException;

    CapabilityStatement requestCapabilityStatement() throws ResourceNotFoundException;
}

