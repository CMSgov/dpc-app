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
    public static final List<String> TEST_PATIENT_IDS = List.of("20140000008325", "20140000009893");
    public static final List<String> TEST_PATIENT_MBIS = List.of("2SW4N00AA00", "4SP0P00AA00");
    public static final List<String> TEST_PATIENT_WITH_BAD_IDS = List.of("-1", "-2", "2SW4N00AA00", "4SP0P00AA00", "-3");
    private static final Map<String, String> MBI_BENE_MAP = Map.of("2SW4N00AA00", "20140000008325",
            "4SP0P00AA00", "20140000009893",
            "-1", "-1",
            "-2", "-2",
            "-3", "-3");

    private final IParser parser;

    public MockBlueButtonClient(FhirContext fhirContext) {
        fhirContext.setPerformanceOptions(PerformanceOptionsEnum.DEFERRED_MODEL_SCANNING);
        parser = fhirContext.newXmlParser();
    }

    @Override
    public Patient requestPatientFromServer(String patientID) throws ResourceNotFoundException {
        return loadOne(Patient.class, SAMPLE_PATIENT_PATH_PREFIX, patientID);
    }

    @Override
    public Bundle requestPatientFromServerByMbi(String mbi) throws ResourceNotFoundException {
        Patient patient = loadOne(Patient.class, SAMPLE_PATIENT_PATH_PREFIX, TEST_PATIENT_MBIS.get(0));
        Bundle bundle = new Bundle();
        bundle.addEntry().setResource(patient);
        return bundle;
    }

    @Override
    public Bundle requestPatientFromServerByMbiHash(String mbiHash) throws ResourceNotFoundException {
        Patient patient = loadOne(Patient.class, SAMPLE_PATIENT_PATH_PREFIX, TEST_PATIENT_MBIS.get(0));
        Bundle bundle = new Bundle();
        bundle.addEntry().setResource(patient);
        return bundle;
    }

    @Override
    public Bundle requestEOBFromServer(String patientID) throws ResourceNotFoundException {
        return loadBundle(SAMPLE_EOB_PATH_PREFIX, patientID);
    }

    @Override
    public Bundle requestCoverageFromServer(String patientID) throws ResourceNotFoundException {
        return loadBundle(SAMPLE_COVERAGE_PATH_PREFIX, patientID);
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
        return "";
    }

    /**
     * Read a Bundle FHIR Resource from jar's Bundle resource file.
     *
     * @param pathPrefix - Path to the XML sample data
     * @param patientID - id of patient
     * @return FHIR Resource
     */
    private Bundle loadBundle(String pathPrefix, String patientID) {
        try(InputStream sampleData = loadResource(pathPrefix, patientID)) {
            return parser.parseResource(Bundle.class, sampleData);
        } catch(IOException ex) {
            throw formNoPatientException(patientID);
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
     * @param patientID - The patient associated with the file
     * @return the stream associated with the resource
     */
    private InputStream loadResource(String pathPrefix, String patientID) throws ResourceNotFoundException {
        if (!TEST_PATIENT_IDS.contains(patientID)) {
            throw formNoPatientException(patientID);
        }
        final var path = pathPrefix + patientID + ".xml";
        return MockBlueButtonClient.class.getClassLoader().getResourceAsStream(path);
    }

    private ResourceNotFoundException formNoPatientException(String patientID) {
        return new ResourceNotFoundException("No patient found with ID: " + patientID);
    }
}
