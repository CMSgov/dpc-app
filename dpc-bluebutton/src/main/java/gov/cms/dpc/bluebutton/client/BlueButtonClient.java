package gov.cms.dpc.bluebutton.client;


import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.CapabilityStatement;
import org.hl7.fhir.dstu3.model.Resource;

import java.security.GeneralSecurityException;


public interface BlueButtonClient {

    Patient requestPatientFromServer(String beneId) throws ResourceNotFoundException;

    Bundle requestPatientFromServerByMbi(String mbi) throws ResourceNotFoundException, GeneralSecurityException;

    Bundle requestPatientFromServerByMbiHash(String mbiHash) throws ResourceNotFoundException;

    Bundle requestEOBFromServer(String beneId) throws ResourceNotFoundException;

    Bundle requestCoverageFromServer(String beneId) throws ResourceNotFoundException;

    Bundle requestNextBundleFromServer(Bundle bundle) throws ResourceNotFoundException;

    CapabilityStatement requestCapabilityStatement() throws ResourceNotFoundException;

    String hashMbi(String mbi) throws GeneralSecurityException;
}

