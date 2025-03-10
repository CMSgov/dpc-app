package gov.cms.dpc.fhir.configuration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DPCFHIRConfigurationUnitTest {

    @Test
    void testFHIRValidationConfiguration() {
        DPCFHIRConfiguration.FHIRValidationConfiguration configuration = new DPCFHIRConfiguration.FHIRValidationConfiguration();
        assertAll(
                () -> assertFalse(configuration.isEnabled()),
                () -> assertFalse(configuration.isSchemaValidation()),
                () -> assertFalse(configuration.isSchematronValidation()),
                () -> assertFalse(configuration.isDebugValidation())
        );

        configuration.setEnabled(true);
        configuration.setSchemaValidation(true);
        configuration.setSchematronValidation(true);
        configuration.setDebugValidation(true);
        assertAll(
                () -> assertTrue(configuration.isEnabled()),
                () -> assertTrue(configuration.isSchemaValidation()),
                () -> assertTrue(configuration.isSchematronValidation()),
                () -> assertTrue(configuration.isDebugValidation())
        );
    }
}
