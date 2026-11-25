package gov.cms.dpc.fhir.validations.r4;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ValidationResult;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import gov.cms.dpc.fhir.validations.profiles.r4.PractitionerProfile;
import gov.cms.dpc.testing.BufferedLoggerHandler;
import org.hl7.fhir.common.hapi.validation.support.InMemoryTerminologyServerValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.ValidationSupportChain;
import org.hl7.fhir.common.hapi.validation.validator.FhirInstanceValidator;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Practitioner;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(BufferedLoggerHandler.class)
class PractitionerValidationTest {

    private static FhirValidator fhirValidator;

    @BeforeAll
    static void setup() {
        FhirContext ctx = FhirContext.forR4();
        final FhirInstanceValidator instanceValidator = new FhirInstanceValidator(ctx);

        fhirValidator = ctx.newValidator();
        fhirValidator.registerValidatorModule(instanceValidator);

        DPCProfileSupport dpcModule = new DPCProfileSupport(ctx);
        final ValidationSupportChain chain = new ValidationSupportChain(
                dpcModule,
                new DefaultProfileValidationSupport(ctx),
                new InMemoryTerminologyServerValidationSupport(ctx)
        );
        instanceValidator.setValidationSupport(chain);
    }

    @Test
    void testHasName() {
        final Practitioner practitioner = generateFakePractitioner();
        practitioner.addName().setFamily("Patient").addGiven("Test");
        practitioner.addIdentifier().setSystem(DPCIdentifierSystem.NPPES.getSystem()).setValue("test-npi");

        final ValidationResult result = fhirValidator.validateWithResult(practitioner);
        assertTrue(result.isSuccessful(), () -> result.getMessages().toString());

        practitioner.addName().setFamily("Missing");
        final ValidationResult r2 = fhirValidator.validateWithResult(practitioner);
        assertFalse(r2.isSuccessful(), "Every name should include a given name");
    }

    @Test
    void testHasIdentifier() {
        final Practitioner practitioner = generateFakePractitioner();
        practitioner.addName().setFamily("Patient").addGiven("Test");

        final ValidationResult result = fhirValidator.validateWithResult(practitioner);
        assertFalse(result.isSuccessful(), "Should require an NPI");

        practitioner.addIdentifier().setSystem(DPCIdentifierSystem.MBI.getSystem()).setValue("test-mbi");
        final ValidationResult r2 = fhirValidator.validateWithResult(practitioner);
        assertFalse(r2.isSuccessful(), "An MBI should not satisfy the NPI requirement");

        practitioner.addIdentifier().setSystem(DPCIdentifierSystem.NPPES.getSystem()).setValue("test-npi");
        final ValidationResult r3 = fhirValidator.validateWithResult(practitioner);
        assertTrue(r3.isSuccessful(), () -> r3.getMessages().toString());
    }

    private Practitioner generateFakePractitioner() {
        final Practitioner practitioner = new Practitioner();
        practitioner.setMeta(new Meta().addProfile(PractitionerProfile.PROFILE_URI));
        practitioner.setId("test-practitioner");
        return practitioner;
    }
}
