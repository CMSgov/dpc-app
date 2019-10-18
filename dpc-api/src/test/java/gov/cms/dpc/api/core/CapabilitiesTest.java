package gov.cms.dpc.api.core;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.SchemaBaseValidator;
import ca.uhn.fhir.validation.SingleValidationMessage;
import ca.uhn.fhir.validation.ValidationResult;
import ca.uhn.fhir.validation.schematron.SchematronBaseValidator;
import gov.cms.dpc.testing.BufferedLoggerHandler;
import org.hl7.fhir.dstu3.model.CapabilityStatement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(BufferedLoggerHandler.class)
class CapabilitiesTest {

    private FhirValidator validator;

    @BeforeEach
    void setupValidator() {
        final FhirContext ctx = FhirContext.forDstu3();

        validator = ctx.newValidator();

        final SchemaBaseValidator val1 = new SchemaBaseValidator(ctx);
        final SchematronBaseValidator val2 = new SchematronBaseValidator(ctx);
        validator.registerValidatorModule(val1);
        validator.registerValidatorModule(val2);
    }

    @Test
    void capabilitiesIsValid() {
        final CapabilityStatement capabilities = Capabilities.getCapabilities();
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
