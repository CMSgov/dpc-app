package gov.cms.dpc.aggregation.bbclient;


import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.CapabilityStatement;
import org.hl7.fhir.dstu3.model.Enumerations;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.instance.model.api.IBaseResource;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.MissingResourceException;

public class MockBlueButtonClient implements BlueButtonClient {

    private static final String SAMPLE_EOB_PATH_PREFIX = "bb-test-data/eob/";
    private static final String SAMPLE_PATIENT_PATH_PREFIX = "bb-test-data/patient/";
    private static final String SAMPLE_METADATA_PATH_PREFIX = "bb-test-data/";
    public static final List<String> TEST_PATIENT_IDS = List.of("20140000008325", "20140000009893");

    public MockBlueButtonClient(){

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
    public CapabilityStatement requestCapabilityStatement() throws ResourceNotFoundException {
        final var path = SAMPLE_METADATA_PATH_PREFIX + "meta.xml";
        return requestFromServer(CapabilityStatement.class, path);
    }

    /**
     * Read a FHIR Resource from the jars resource file.
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
