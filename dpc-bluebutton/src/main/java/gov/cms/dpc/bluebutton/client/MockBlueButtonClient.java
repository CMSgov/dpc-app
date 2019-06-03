package gov.cms.dpc.bluebutton.client;


import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Single;
import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.instance.model.api.IBaseResource;

import java.io.InputStream;
import java.util.List;
import java.util.MissingResourceException;
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
    public Patient requestPatientFromServer(String patientID) {
        return requestFromServer(Patient.class, SAMPLE_PATIENT_PATH_PREFIX, patientID);
    }

    @Override
    public Bundle requestEOBBundleFromServer(String patientID) {
        return requestFromServer(Bundle.class, SAMPLE_EOB_PATH_PREFIX, patientID);
    }

    @Override
    public Observable <Coverage> requestCoverageFromServer(String patientID) throws ResourceNotFoundException {
        return generateFromServer(Coverage.class, SAMPLE_COVERAGE_PATH_PREFIX, patientID);
    }

    @Override
    public CapabilityStatement requestCapabilityStatement() throws ResourceNotFoundException {
        final var path = SAMPLE_METADATA_PATH_PREFIX + "meta.xml";
        return requestFromServer(CapabilityStatement.class, path, null);
    }

    /**
     * Read a FHIR Resource from the jar's resource file.
     *
     * @param resourceClass - FHIR Resource class
     * @param pathPrefix - Path to the XML sample data
     * @param patientID - id of patient
     * @return FHIR Resource
     */
    private <T extends IBaseResource> Observable<T> generateFromServer(Class<T> resourceClass, String pathPrefix, String patientID) {
        if (!TEST_PATIENT_IDS.contains(patientID)) {
            throw new ResourceNotFoundException("No patient found with ID: " + patientID);
        }
        final var path = pathPrefix + patientID + ".xml";
        FhirContext ctx = FhirContext.forDstu3();
        InputStream sampleData = MockBlueButtonClient.class.getClassLoader().getResourceAsStream(path);
        if(sampleData == null) {
            throw new MissingResourceException("Cannot find sample requests", MockBlueButtonClient.class.getName(), path);
        }
        if (resourceClass == ExplanationOfBenefit.class || resourceClass == Coverage.class) {
            // These are stored as Bundles. Need to unpack them.
            final Bundle bundle = ctx.newXmlParser().parseResource(Bundle.class, sampleData);
            final List<T> list = bundle.getEntry()
                    .stream()
                    .map(bundleEntryComponent -> (T)bundleEntryComponent.getResource())
                    .collect(Collectors.toList());
            return Observable.fromIterable(list);
        } else {
            return Observable.just(ctx.newXmlParser().parseResource(resourceClass, sampleData));
        }
    }

    /**
     * Read a FHIR Resource from the jar's resource file.
     *
     * @param resourceClass - FHIR Resource class
     * @param pathPrefix - Path to the XML sample data
     * @param patientID - id of patient
     * @return FHIR Resource
     */
    private <T extends IBaseResource> T requestFromServer(Class<T> resourceClass, String pathPrefix, String patientID) {
        if (!TEST_PATIENT_IDS.contains(patientID)) {
            throw new ResourceNotFoundException("No patient found with ID: " + patientID);
        }
        final var path = pathPrefix + patientID + ".xml";
        FhirContext ctx = FhirContext.forDstu3();
        InputStream sampleData = MockBlueButtonClient.class.getClassLoader().getResourceAsStream(path);
        if(sampleData == null) {
            throw new MissingResourceException("Cannot find sample requests", MockBlueButtonClient.class.getName(), path);
        }
        return ctx.newXmlParser().parseResource(resourceClass, sampleData);
    }
}
