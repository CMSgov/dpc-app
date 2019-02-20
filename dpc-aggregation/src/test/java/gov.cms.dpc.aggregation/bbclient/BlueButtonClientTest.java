package gov.cms.dpc.aggregation.bbclient;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.hl7.fhir.dstu3.model.Patient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class BlueButtonClientTest {
    private static final String TEST_BENEFICIARY_ID = "20140000008325";
    private BlueButtonClient bbc;

    @BeforeEach
    public void setupBlueButtonClient() {
        final Injector injector = Guice.createInjector(new TestModule(), new BlueButtonClientModule());
        bbc = injector.getInstance(BlueButtonClient.class);
    }

    @Test
    void testGetFHIRFromBeneficiaryID() throws BlueButtonClientException {
        Patient ret = bbc.requestFHIRFromServer(TEST_BENEFICIARY_ID);

        // Verify basic demo patient information
        assertNotEquals(ret, null);
        assertEquals(ret.getBirthDate(), Date.valueOf("2014-06-01"));
        assertEquals(ret.getGender().getDisplay(), "Unknown");
        assertEquals(ret.getName().size(), 1);
        assertEquals(ret.getName().get(0).getFamily(), "Doe");
        assertEquals(ret.getName().get(0).getGiven().get(0).toString(), "Jane");
    }

}