package gov.cms.dpc.aggregation.bbclient;

import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Patient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.Date;

import static org.junit.jupiter.api.Assertions.*;

class BlueButtonClientTest {
    private static final String TEST_PATIENT_ID = "20140000008325";
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
        assertNotEquals(ret, null);
        assertEquals(ret.getBirthDate(), Date.valueOf("2014-06-01"));
        assertEquals(ret.getGender().getDisplay(), "Unknown");
        assertEquals(ret.getName().size(), 1);
        assertEquals(ret.getName().get(0).getFamily(), "Doe");
        assertEquals(ret.getName().get(0).getGiven().get(0).toString(), "Jane");
    }

    @Test
    void shouldGetEOBFromPatientID() {
        Bundle explanationOfBenefits = bbc.requestEOBBundleFromServer(TEST_PATIENT_ID);

        assertNotEquals(explanationOfBenefits, null);
    }

    @Test
    void shouldThrowExceptionWhenResourceNotFound() {
        assertThrows(ResourceNotFoundException.class, () -> {
            bbc.requestPatientFromServer(TEST_NONEXISTENT_PATIENT_ID);
        });

        assertThrows(ResourceNotFoundException.class, () -> {
            bbc.requestEOBBundleFromServer(TEST_NONEXISTENT_PATIENT_ID);
        });

    }

}