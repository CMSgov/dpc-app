package gov.cms.dpc.web.core;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.SchemaBaseValidator;
import ca.uhn.fhir.validation.ValidationResult;
import ca.uhn.fhir.validation.schematron.SchematronBaseValidator;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CapabilitiesTest {

    private FhirValidator validator;

    @BeforeEach
    public void setupValidator() {
        final FhirContext ctx = FhirContext.forR4();

        validator = ctx.newValidator();

        final SchemaBaseValidator val1 = new SchemaBaseValidator(ctx);
        final SchematronBaseValidator val2 = new SchematronBaseValidator(ctx);
//        new FhirInstanceValidator();
        validator.registerValidatorModule(val1);
        validator.registerValidatorModule(val2);
    }

    @Test
    public void capabilitiesIsValid() {
        final CapabilityStatement capabilities = Capabilities.buildCapabilities("http://localhost:3002", "/v1");
        final ValidationResult validationResult = validator.validateWithResult(capabilities);
        assertTrue(validationResult.isSuccessful(), "Capabilities should be valid");

        // Verify properties
        assertAll(() -> assertEquals(1, capabilities.getRest().size(), "Should have a single server operation"),
                () -> assertEquals(3, capabilities.getRest().get(0).getOperation().size(), "Should have three routes"));
    }
}
