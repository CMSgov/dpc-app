package gov.cms.dpc.aggregation.bbclient;

import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.hl7.fhir.dstu3.model.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.Date;

import static org.junit.jupiter.api.Assertions.*;

class BlueButtonClientTest {
    // A random example patient (Jane Doe)
    private static final String TEST_PATIENT_ID = "20140000008325";
    // A patient that only has a single EOB record in bluebutton
    private static final String TEST_SINGLE_EOB_PATIENT_ID = "20140000009893";
    // A patient with very many (592) EOB records in bluebutton
    private static final String TEST_LARGE_EOB_PATIENT_ID = "20140000001827";
    // A patient id that should not exist in bluebutton
    private static final String TEST_NONEXISTENT_PATIENT_ID = "31337";

    private static BlueButtonClient bbc;

    @BeforeAll
    public static void setupBlueButtonClient() {
        final Injector injector = Guice.createInjector(new TestModule(), new BlueButtonClientModule());
        bbc = injector.getInstance(BlueButtonClient.class);
    }

    @Test
    void shouldGetFHIRFromPatientID() {
        Patient ret = bbc.requestPatientFromServer(TEST_PATIENT_ID);

        // Verify basic demo patient information
        assertNotNull(ret, "The demo Patient object returned from BlueButtonClient should not be null");

        String patientDataCorrupted = "The demo Patient object data differs from what is expected";
        assertEquals(ret.getBirthDate(), Date.valueOf("2014-06-01"), patientDataCorrupted);
        assertEquals(ret.getGender().getDisplay(), "Unknown", patientDataCorrupted);
        assertEquals(ret.getName().size(), 1, patientDataCorrupted);
        assertEquals(ret.getName().get(0).getFamily(), "Doe", patientDataCorrupted);
        assertEquals(ret.getName().get(0).getGiven().get(0).toString(), "Jane", patientDataCorrupted);
    }

    @Test
    void shouldGetEOBFromPatientID() {
        Bundle response = bbc.requestEOBBundleFromServer(TEST_PATIENT_ID);

        assertNotNull(response, "The demo patient should have a non-null EOB bundle");
        assertEquals(response.getTotal(), 32, "The demo patient should have exactly 32 EOBs");
    }

    @Test
    void shouldReturnBundleContainingOnlyEOBs() {
        Bundle response = bbc.requestEOBBundleFromServer(TEST_PATIENT_ID);

        response.getEntry().forEach((entry) -> {
            assertEquals(
                    entry.getResource().getResourceType(),
                    ResourceType.ExplanationOfBenefit,
                    "EOB bundles returned by the BlueButton client should only contain EOB objects"
            );
        });
    }

    @Test
    void shouldHandlePatientsWithOnlyOneEOB() {
        Bundle response = bbc.requestEOBBundleFromServer(TEST_SINGLE_EOB_PATIENT_ID);

        assertEquals(response.getTotal(), 1, "This demo patient should have exactly 1 EOB");
    }

    @Test
    void shouldHandlePatientsWithVeryManyEOBs() {
        Bundle response = bbc.requestEOBBundleFromServer(TEST_LARGE_EOB_PATIENT_ID);

        assertEquals(response.getTotal(), 592, "This demo patient should have exactly 592 EOBs");
    }

    @Test
    void shouldThrowExceptionWhenResourceNotFound() {
        assertThrows(
                ResourceNotFoundException.class, () -> {
                    bbc.requestPatientFromServer(TEST_NONEXISTENT_PATIENT_ID);
                },
                "BlueButton client should throw exceptions when asked to retrieve a non-existent patient"
        );

        assertThrows(
                ResourceNotFoundException.class, () -> {
                    bbc.requestEOBBundleFromServer(TEST_NONEXISTENT_PATIENT_ID);
                },
                "BlueButton client should throw exceptions when asked to retrieve EOBs for a non-existent patient"
        );

    }

}