package gov.cms.dpc.aggregation.bbclient;


import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.instance.model.api.IBaseResource;

import java.io.InputStream;
import java.util.MissingResourceException;

public class MockBlueButtonClient implements BlueButtonClient {

    private static final String SAMPLE_EOB_PATH_PREFIX = "bb-test-data/eob/";
    private static final String SAMPLE_PATIENT_PATH_PREFIX = "bb-test-data/patient/";
    public static final String[] TEST_PATIENT_IDS = {"20140000008325", "20140000009893"};

    public MockBlueButtonClient(){

    }

    public Patient requestPatientFromServer(String patientID) {
        final var path = SAMPLE_PATIENT_PATH_PREFIX + patientID + ".xml";
        return requestFromServer(Patient.class, path);
    }

    public Bundle requestEOBBundleFromServer(String patientID) {
        final var path = SAMPLE_EOB_PATH_PREFIX + patientID + ".xml";
        return requestFromServer(Bundle.class, path);
    }

    /**
     * Read a FHIR Resource from the jars resource file.
     *
     * @param resourceClass - FHIR Resource class
     * @param resourceFilePath - Path to the XML sample data
     * @param <T> FHIR Resource Type
     * @return FHIR Resource
     */
    private <T extends IBaseResource> T requestFromServer(Class<T> resourceClass, String resourceFilePath) {
        FhirContext ctx = FhirContext.forDstu3();
        InputStream sampleData = MockBlueButtonClient.class.getClassLoader().getResourceAsStream(resourceFilePath);
        if(sampleData == null) {
            throw new MissingResourceException("Cannot find sample requests", MockBlueButtonClient.class.getName(), resourceFilePath);
        }
        return ctx.newXmlParser().parseResource(resourceClass, sampleData);
    }
}
