package gov.cms.dpc.bluebutton.client;


import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.instance.model.api.IBaseResource;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;
import java.util.stream.Collectors;

public class MockBlueButtonClient implements BlueButtonClient {

    private static final String SAMPLE_EOB_PATH_PREFIX = "bb-test-data/eob/";
    private static final String SAMPLE_PATIENT_PATH_PREFIX = "bb-test-data/patient/";
    private static final String SAMPLE_COVERAGE_PATH_PREFIX = "bb-test-data/coverage/";
    private static final String SAMPLE_METADATA_PATH_PREFIX = "bb-test-data/";
    public static final List<String> TEST_PATIENT_IDS = List.of("20140000008325", "20140000009893");

    public MockBlueButtonClient() {
        // Not used
    }

    @Override
    public Patient requestPatientFromServer(String patientID) throws ResourceNotFoundException {
        return loadOne(Patient.class, SAMPLE_PATIENT_PATH_PREFIX, patientID);
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
        final String link = bundle.getLink(Bundle.LINK_NEXT).getUrl();
        final List<NameValuePair> params = URLEncodedUtils.parse(link, Charset.forName("UTF-8"));
        final var patient = params.stream().filter(pair -> pair.getName().equals("patient")).findFirst().get().getValue();
        final var startIndex = params.stream().filter(pair -> pair.getName().equals("startIndex")).findFirst().get().getValue();
        return loadBundle(SAMPLE_EOB_PATH_PREFIX, patient + "_" + startIndex);
    }

    @Override
    public CapabilityStatement requestCapabilityStatement() throws ResourceNotFoundException {
        final var path = SAMPLE_METADATA_PATH_PREFIX + "meta.xml";
        return loadOne(CapabilityStatement.class, path, null);
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
            final var parser = FhirContext.forDstu3().newXmlParser();
            return parser.parseResource(Bundle.class, sampleData);
        } catch(IOException ex) {
            throw new ResourceNotFoundException("No patient found with ID: " + patientID);
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
            final var parser = FhirContext.forDstu3().newXmlParser();
            return parser.parseResource(resourceClass, sampleData);
        } catch(IOException ex) {
            throw new ResourceNotFoundException("No patient found with ID: " + patientID);
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
            throw new ResourceNotFoundException("No patient found with ID: " + patientID);
        }
        final var path = pathPrefix + patientID + ".xml";
        return MockBlueButtonClient.class.getClassLoader().getResourceAsStream(path);
    }
}
