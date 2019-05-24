package gov.cms.dpc.bluebutton.health;

import com.codahale.metrics.health.HealthCheck;
import gov.cms.dpc.bluebutton.client.BlueButtonClient;
import org.hl7.fhir.dstu3.model.CapabilityStatement;
import org.hl7.fhir.dstu3.model.Enumerations;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class TestBlueButtonHealth {

    private static final String EXCEPTION_MESSAGE = "Timeout, not good";

    @Test
    void testHealthy() {

        final BlueButtonClient bbc = Mockito.mock(BlueButtonClient.class);
        when(bbc.requestCapabilityStatement()).thenReturn(new CapabilityStatement().setStatus(Enumerations.PublicationStatus.ACTIVE));
        final BlueButtonHealthCheck blueButtonHealthCheck = new BlueButtonHealthCheck(bbc);

        final HealthCheck.Result check = blueButtonHealthCheck.check();
        assertTrue(check.isHealthy(), "Mock BlueButton should be healthy");
    }

    @Test
    void testException() {

        final BlueButtonClient bbc = Mockito.mock(BlueButtonClient.class);
        when(bbc.requestCapabilityStatement())
                .then(answer -> {
                    throw new RuntimeException(EXCEPTION_MESSAGE);
                });
        final BlueButtonHealthCheck blueButtonHealthCheck = new BlueButtonHealthCheck(bbc);

        final HealthCheck.Result check = blueButtonHealthCheck.check();
        assertAll(() -> assertFalse(check.isHealthy(), "Mock BlueButton should not healthy"),
                () -> assertEquals(EXCEPTION_MESSAGE, check.getMessage(), "Should throw exact message"));

    }

    @Test
    void testInvalidResponse() {

        final BlueButtonClient bbc = Mockito.mock(BlueButtonClient.class);
        when(bbc.requestCapabilityStatement()).thenReturn(new CapabilityStatement().setStatus(Enumerations.PublicationStatus.DRAFT));
        final BlueButtonHealthCheck blueButtonHealthCheck = new BlueButtonHealthCheck(bbc);

        final HealthCheck.Result check = blueButtonHealthCheck.check();
        assertAll(() -> assertFalse(check.isHealthy(), "Mock BlueButton should not healthy"),
                () -> assertEquals(BlueButtonHealthCheck.INVALID_MESSAGE, check.getMessage(), "Should throw exact message"));
    }
}
