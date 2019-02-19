package gov.cms.dpc.aggregation.bbclient;

import org.hl7.fhir.dstu3.model.Bundle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BlueButtonClientTest {
    private static final String TEST_BLUEBUTTON_ENDPOINT = "https://fhir.backend.bluebutton.hhsdevcloud.us/v1/fhir/";
    private static final String TEST_BENEFICIARY_ID = "20140000008325";
    private DefaultBlueButtonClient bbc;

    @BeforeEach
    public void setupBlueButtonClient(){
        bbc = new DefaultBlueButtonClient(TEST_BLUEBUTTON_ENDPOINT);
    }

    @Test
    void testGetFHIRFromBeneficiaryID() throws BlueButtonClientException {
        Bundle ret = bbc.requestFhirBundle(TEST_BENEFICIARY_ID);
        assertNotEquals(ret, null);
        System.out.println("Identifier: " + ret.getIdentifier().getValue());
        System.out.println("Resource Type: " + ret.getResourceType());
        System.out.println("FHIR Type: " + ret.fhirType());
    }
}