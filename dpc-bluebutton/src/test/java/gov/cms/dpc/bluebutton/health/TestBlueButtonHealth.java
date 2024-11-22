package gov.cms.dpc.bluebutton.health;

import com.codahale.metrics.health.HealthCheck;
import gov.cms.dpc.bluebutton.client.BlueButtonClient;
import gov.cms.dpc.testing.BufferedLoggerHandler;
import org.hl7.fhir.dstu3.model.CapabilityStatement;
import org.hl7.fhir.dstu3.model.Enumerations;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;
import static org.mockito.Mockito.when;

@ExtendWith(BufferedLoggerHandler.class)
@DisplayName("Blue Button health checking")
class TestBlueButtonHealth {

    private static final String EXCEPTION_MESSAGE = "Timeout, not good";

    @Test
    @DisplayName("BlueButton health check  🥳")
    void testHealthy() {

        final BlueButtonClient bbc = Mockito.mock(BlueButtonClient.class);
        when(bbc.requestCapabilityStatement()).thenReturn(new CapabilityStatement().setStatus(Enumerations.PublicationStatus.ACTIVE));
        final BlueButtonHealthCheck blueButtonHealthCheck = new BlueButtonHealthCheck(bbc);

        final HealthCheck.Result check = blueButtonHealthCheck.check();
        assertTrue(check.isHealthy(), "Mock BlueButton should be healthy");
    }

    @Test
    @DisplayName("Blue Button health check 🤮")
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
    @DisplayName("Invalid health check response 🤮")
    void testInvalidResponse() {

        final BlueButtonClient bbc = Mockito.mock(BlueButtonClient.class);
        when(bbc.requestCapabilityStatement()).thenReturn(new CapabilityStatement().setStatus(Enumerations.PublicationStatus.DRAFT));
        final BlueButtonHealthCheck blueButtonHealthCheck = new BlueButtonHealthCheck(bbc);

        final HealthCheck.Result check = blueButtonHealthCheck.check();
        assertAll(() -> assertFalse(check.isHealthy(), "Mock BlueButton should not healthy"),
                () -> assertEquals(BlueButtonHealthCheck.INVALID_MESSAGE, check.getMessage(), "Should throw exact message"));
    }
}
