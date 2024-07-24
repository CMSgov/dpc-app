package gov.cms.dpc.bluebutton.client;


import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.CapabilityStatement;

import java.security.GeneralSecurityException;
import java.util.Map;


public interface BlueButtonClient {

    Bundle requestPatientFromServerByMbi(String mbi, Map<String, String> headers) throws ResourceNotFoundException, GeneralSecurityException;

    Bundle requestPatientFromServer(String beneId, DateRangeParam lastUpdated, Map<String, String> headers) throws ResourceNotFoundException;

    Bundle requestEOBFromServer(String beneId, DateRangeParam lastUpdated, Map<String, String> headers) throws ResourceNotFoundException;

    Bundle requestCoverageFromServer(String beneId, DateRangeParam lastUpdated, Map<String, String> headers) throws ResourceNotFoundException;

    Bundle requestNextBundleFromServer(Bundle bundle, Map<String, String> headers) throws ResourceNotFoundException;

    CapabilityStatement requestCapabilityStatement() throws ResourceNotFoundException;
}

