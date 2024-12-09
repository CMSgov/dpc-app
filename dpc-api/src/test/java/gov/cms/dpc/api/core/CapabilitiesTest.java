package gov.cms.dpc.api.core;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.SchemaBaseValidator;
import ca.uhn.fhir.validation.SingleValidationMessage;
import ca.uhn.fhir.validation.ValidationResult;
import gov.cms.dpc.testing.BufferedLoggerHandler;
import org.hl7.fhir.dstu3.model.CapabilityStatement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;

@ExtendWith(BufferedLoggerHandler.class)
@DisplayName("Capabilities statement verification")
class CapabilitiesTest {

    private FhirValidator validator;

    @BeforeEach
    void setupValidator() {
        final FhirContext ctx = FhirContext.forDstu3();

        validator = ctx.newValidator();

        final SchemaBaseValidator val = new SchemaBaseValidator(ctx);
        validator.registerValidatorModule(val);
    }

    @Test
    @DisplayName("Validate capabilities statement 🥳")
    void capabilitiesIsValid() {
        final CapabilityStatement capabilities = Capabilities.getCapabilities("https://sandbox.dpc.cms.gov/api/v1");
        final ValidationResult validationResult = validator.validateWithResult(capabilities);
        assertTrue(validationResult.isSuccessful(), validationResultsToString(validationResult));
    }

    private static String validationResultsToString(ValidationResult result) {
        return result
                .getMessages()
                .stream()
                .map(SingleValidationMessage::getMessage)
                .collect(Collectors.joining("\n"));
    }
}
