package gov.cms.dpc.bluebutton.client;


import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.CapabilityStatement;
import org.hl7.fhir.dstu3.model.Patient;

public interface BlueButtonClient {

    Patient requestPatientFromServer(String patientID) throws ResourceNotFoundException;

    Bundle requestEOBBundleFromServer(String patientID) throws ResourceNotFoundException;

    CapabilityStatement requestCapabilityStatement() throws ResourceNotFoundException;
}
