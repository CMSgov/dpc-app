package gov.cms.dpc.bluebutton.client;


import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.CapabilityStatement;

import java.security.GeneralSecurityException;


public interface BlueButtonClient {

    Bundle requestPatientFromServerByMbi(String mbi) throws ResourceNotFoundException, GeneralSecurityException;

    Bundle requestPatientFromServerByMbiHash(String mbiHash) throws ResourceNotFoundException;

    Bundle requestPatientFromServer(String beneId, DateRangeParam lastUpdated) throws ResourceNotFoundException;

    Bundle requestEOBFromServer(String beneId, DateRangeParam lastUpdated) throws ResourceNotFoundException;

    Bundle requestCoverageFromServer(String beneId, DateRangeParam lastUpdated) throws ResourceNotFoundException;

    Bundle requestNextBundleFromServer(Bundle bundle) throws ResourceNotFoundException;

    CapabilityStatement requestCapabilityStatement() throws ResourceNotFoundException;

    String hashMbi(String mbi) throws GeneralSecurityException;
}

