package gov.cms.dpc.bluebutton.client;


import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.CapabilityStatement;

import java.security.GeneralSecurityException;


public interface BlueButtonClient {

    Patient requestPatientFromServer(String patientID) throws ResourceNotFoundException;

    Bundle requestPatientFromServerByMbiHash(String mbiHash) throws ResourceNotFoundException;

    Bundle requestEOBFromServer(String patientID) throws ResourceNotFoundException;

    Bundle requestCoverageFromServer(String patientID) throws ResourceNotFoundException;

    Bundle requestNextBundleFromServer(Bundle bundle) throws ResourceNotFoundException;

    CapabilityStatement requestCapabilityStatement() throws ResourceNotFoundException;

    String hashMbi(String mbi) throws GeneralSecurityException;
}

