package gov.cms.dpc.bluebuttonv2.client;


import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CapabilityStatement;

import java.security.GeneralSecurityException;
import java.util.Map;


public interface BlueButtonClientV2 {

    Bundle requestPatientFromServerByMbi(String mbi, Map<String, String> headers) throws ResourceNotFoundException, GeneralSecurityException;

    Bundle requestPatientFromServerByMbiHash(String mbiHash, Map<String, String> headers) throws ResourceNotFoundException;

    Bundle requestPatientFromServer(String beneId, DateRangeParam lastUpdated, Map<String, String> headers) throws ResourceNotFoundException;

    Bundle requestEOBFromServer(String beneId, DateRangeParam lastUpdated, Map<String, String> headers) throws ResourceNotFoundException;

    Bundle requestCoverageFromServer(String beneId, DateRangeParam lastUpdated, Map<String, String> headers) throws ResourceNotFoundException;

    Bundle requestNextBundleFromServer(Bundle bundle, Map<String, String> headers) throws ResourceNotFoundException;

    CapabilityStatement requestCapabilityStatement() throws ResourceNotFoundException;

    String hashMbi(String mbi) throws GeneralSecurityException;
}

