package gov.cms.dpc.bluebutton.client;


import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.PerformanceOptionsEnum;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.apache.http.client.utils.URLEncodedUtils;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.CapabilityStatement;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.instance.model.api.IBaseResource;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Map;

public class MockBlueButtonClient implements BlueButtonClient {

    private static final String SAMPLE_EOB_PATH_PREFIX = "bb-test-data/eob/";
    private static final String SAMPLE_PATIENT_PATH_PREFIX = "bb-test-data/patient/";
    private static final String SAMPLE_COVERAGE_PATH_PREFIX = "bb-test-data/coverage/";
    private static final String SAMPLE_METADATA_PATH_PREFIX = "bb-test-data/";
    public static final List<String> TEST_PATIENT_MBIS = List.of("2SW4N00AA00", "4SP0P00AA00");
    public static final Map<String, String> MBI_BENE_ID_MAP = Map.of(
            TEST_PATIENT_MBIS.get(0), "-20140000008325",
            TEST_PATIENT_MBIS.get(1), "-20140000009893"
    );
    public static final Map<String, String> MBI_HASH_MAP = Map.of(
            TEST_PATIENT_MBIS.get(0), "abadf57ff8dc94610ca0d479feadb1743c9cd3c77caf1eafde5719a154379fb6",
            TEST_PATIENT_MBIS.get(1), "8930cab29ba5fe4311a5f5bcfd5b7384f3722b711402aacf796d2ae6fea54242"
    );
    public static final List<String> TEST_PATIENT_WITH_BAD_IDS = List.of("-1", "-2", TEST_PATIENT_MBIS.get(0), TEST_PATIENT_MBIS.get(1), "-3");

    private final IParser parser;

    public MockBlueButtonClient(FhirContext fhirContext) {
        fhirContext.setPerformanceOptions(PerformanceOptionsEnum.DEFERRED_MODEL_SCANNING);
        parser = fhirContext.newXmlParser();
    }

    @Override
    public Patient requestPatientFromServer(String beneId) throws ResourceNotFoundException {
        return loadOne(Patient.class, SAMPLE_PATIENT_PATH_PREFIX, beneId);
    }

    @Override
    public Bundle requestPatientFromServerByMbi(String mbi) throws ResourceNotFoundException {
        Patient p = loadOne(Patient.class, SAMPLE_PATIENT_PATH_PREFIX, MBI_BENE_ID_MAP.get(mbi));
        Bundle b = new Bundle();
        b.setTotal(1);
        b.addEntry().setResource(p);
        return b;
    }

    @Override
    public Bundle requestPatientFromServerByMbiHash(String mbiHash) throws ResourceNotFoundException {
        String mbi = MBI_HASH_MAP.values().stream()
                .filter(h -> h.equals(mbiHash))
                .findFirst()
                .orElse("");
        Patient p = loadOne(Patient.class, SAMPLE_PATIENT_PATH_PREFIX, MBI_BENE_ID_MAP.get(mbi));
        Bundle b = new Bundle();
        b.setTotal(1);
        b.addEntry().setResource(p);
        return b;
    }

    @Override
    public Bundle requestEOBFromServer(String beneId) throws ResourceNotFoundException {
        return loadBundle(SAMPLE_EOB_PATH_PREFIX, beneId);
    }

    @Override
    public Bundle requestCoverageFromServer(String beneId) throws ResourceNotFoundException {
        return loadBundle(SAMPLE_COVERAGE_PATH_PREFIX, beneId);
    }

    @Override
    public Bundle requestNextBundleFromServer(Bundle bundle) throws ResourceNotFoundException {
        // This is code is very specific to the bb-test-data directory and its contents
        final var nextLink = bundle.getLink(Bundle.LINK_NEXT).getUrl();
        final var nextUrl = URI.create(nextLink);
        final var params = URLEncodedUtils.parse(nextUrl.getQuery(), Charset.forName("UTF-8"));
        final var patient = params.stream().filter(pair -> pair.getName().equals("patient")).findFirst().orElseThrow().getValue();
        final var startIndex = params.stream().filter(pair -> pair.getName().equals("startIndex")).findFirst().orElseThrow().getValue();
        var path = SAMPLE_EOB_PATH_PREFIX + patient + "_" + startIndex + ".xml";

        try(InputStream sampleData = MockBlueButtonClient.class.getClassLoader().getResourceAsStream(path)) {
            return parser.parseResource(Bundle.class, sampleData);
        } catch(IOException ex) {
            throw new ResourceNotFoundException("Missing next bundle");
        }
    }

    @Override
    public CapabilityStatement requestCapabilityStatement() throws ResourceNotFoundException {
        final var path = SAMPLE_METADATA_PATH_PREFIX + "meta.xml";
        return loadOne(CapabilityStatement.class, path, null);
    }

    @Override
    public String hashMbi(String mbi) throws GeneralSecurityException {
        return MBI_HASH_MAP.get(mbi);
    }

    /**
     * Read a Bundle FHIR Resource from jar's Bundle resource file.
     *
     * @param pathPrefix - Path to the XML sample data
     * @param beneId - CCW/BFD beneficiary ID of patient (https://bluebutton.cms.gov/resources/variables/bene_id)
     * @return FHIR Resource
     */
    private Bundle loadBundle(String pathPrefix, String beneId) {
        try(InputStream sampleData = loadResource(pathPrefix, beneId)) {
            return parser.parseResource(Bundle.class, sampleData);
        } catch(IOException ex) {
            throw formNoPatientException(beneId);
        }
    }

    /**
     * Read a FHIR Resource from the jar's resource file.
     *
     * @param resourceClass - FHIR Resource class
     * @param pathPrefix - Path to the XML sample data
     * @return FHIR Resource
     */
    private <T extends IBaseResource> T loadOne(Class<T> resourceClass, String pathPrefix, String patientID) {
        try(InputStream sampleData = loadResource(pathPrefix, patientID)) {
            return parser.parseResource(resourceClass, sampleData);
        } catch(IOException ex) {
            throw formNoPatientException(patientID);
        }
    }

    /**
     * Create a stream from a resource.
     *
     * @param pathPrefix - The path to the resource file
     * @param beneId - The beneficiary ID of the patient associated with the file
     * @return the stream associated with the resource
     */
    private InputStream loadResource(String pathPrefix, String beneId) throws ResourceNotFoundException {
        if (!MBI_BENE_ID_MAP.values().contains(beneId)) {
            throw formNoPatientException(beneId);
        }
        final var path = pathPrefix + beneId + ".xml";
        return MockBlueButtonClient.class.getClassLoader().getResourceAsStream(path);
    }

    private ResourceNotFoundException formNoPatientException(String patientID) {
        return new ResourceNotFoundException("No patient found with ID: " + patientID);
    }
}
