package gov.cms.dpc.core;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.SchemaBaseValidator;
import ca.uhn.fhir.validation.ValidationResult;
import ca.uhn.fhir.validation.schematron.SchematronBaseValidator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class CapabilitiesTest {

    @Test
    public void testCapabilities() {
        final FhirContext ctx = FhirContext.forR4();

        final FhirValidator validator = ctx.newValidator();

        final SchemaBaseValidator val1 = new SchemaBaseValidator(ctx);
        final SchematronBaseValidator val2 = new SchematronBaseValidator(ctx);
//        new FhirInstanceValidator();
        validator.registerValidatorModule(val1);
        validator.registerValidatorModule(val2);

        final ValidationResult validationResult = validator.validateWithResult(Capabilities.buildCapabilities());
        assertTrue(validationResult.isSuccessful(), "Should successfully validate");
    }
}
