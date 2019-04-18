package gov.cms.dpc.aggregation.bbclient;


import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Patient;

import java.io.InputStream;
import java.util.MissingResourceException;

public class MockBlueButtonClient implements BlueButtonClient {

    private static final String SAMPLE_EOB_PATH_PREFIX = "bb-test-data/eob/";
    private static final String SAMPLE_PATIENT_PATH_PREFIX = "bb-test-data/patient/";
    public static final String[] TEST_PATIENT_IDS = {"20140000008325", "20140000009893"};

    public MockBlueButtonClient(){

    }

    public Patient requestPatientFromServer(String patientID) {
        FhirContext ctx = FhirContext.forDstu3();
        final var path = SAMPLE_PATIENT_PATH_PREFIX + patientID + ".xml";
        InputStream sampleData = MockBlueButtonClient.class.getClassLoader().getResourceAsStream(path);
        if(sampleData == null) {
            throw new MissingResourceException("Cannot find sample requests", MockBlueButtonClient.class.getName(), path);
        }
        return ctx.newXmlParser().parseResource(Patient.class, sampleData);
    }

    public Bundle requestEOBBundleFromServer(String patientID) {
        FhirContext ctx = FhirContext.forDstu3();
        final var path = SAMPLE_EOB_PATH_PREFIX + patientID + ".xml";
        InputStream sampleData = MockBlueButtonClient.class.getClassLoader().getResourceAsStream(path);
        if(sampleData == null) {
            throw new MissingResourceException("Cannot find sample requests", MockBlueButtonClient.class.getName(), path);
        }
        return ctx.newXmlParser().parseResource(Bundle.class, sampleData);
    }
}
