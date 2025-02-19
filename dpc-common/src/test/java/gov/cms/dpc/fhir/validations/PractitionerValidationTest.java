package gov.cms.dpc.fhir.validations;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ValidationResult;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import gov.cms.dpc.fhir.validations.profiles.PractitionerProfile;
import gov.cms.dpc.testing.BufferedLoggerHandler;
import org.hl7.fhir.common.hapi.validation.support.InMemoryTerminologyServerValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.ValidationSupportChain;
import org.hl7.fhir.common.hapi.validation.validator.FhirInstanceValidator;
import org.hl7.fhir.dstu3.model.Meta;
import org.hl7.fhir.dstu3.model.Practitioner;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(BufferedLoggerHandler.class)
public class PractitionerValidationTest {

    private static FhirValidator fhirValidator;
    private static DPCProfileSupport dpcModule;
    private static FhirContext ctx;

    @BeforeAll
    static void setup() {
        ctx = FhirContext.forDstu3();
        final FhirInstanceValidator instanceValidator = new FhirInstanceValidator(ctx);

        fhirValidator = ctx.newValidator();
        fhirValidator.setValidateAgainstStandardSchematron(false);
        fhirValidator.setValidateAgainstStandardSchema(false);
        fhirValidator.registerValidatorModule(instanceValidator);


        dpcModule = new DPCProfileSupport(ctx);
        final ValidationSupportChain chain = new ValidationSupportChain(
                dpcModule,
                new DefaultProfileValidationSupport(ctx),
                new InMemoryTerminologyServerValidationSupport(ctx));
        instanceValidator.setValidationSupport(chain);
    }

    @Test
    void testHasName() {
        final Practitioner practitioner = generateFakePractitioner();
        practitioner.addName().setFamily("Patient").addGiven("Test");
        practitioner.addIdentifier().setSystem(DPCIdentifierSystem.NPPES.getSystem()).setValue("test-mpi");

        final ValidationResult result = fhirValidator.validateWithResult(practitioner);

        assertTrue(result.isSuccessful(), "Should have passed");

        // Add a bad name
        practitioner.addName().setFamily("Missing");
        final ValidationResult r2 = fhirValidator.validateWithResult(practitioner);

        assertAll(() -> assertFalse(r2.isSuccessful(), "Should have failed validation"),
                () -> assertEquals(1, r2.getMessages().size(), "Should have a single failure"));
    }

    @Test
    void testHasIdentifier() {
        final Practitioner practitioner = generateFakePractitioner();
        practitioner.addName().setFamily("Patient").addGiven("Test");

        final ValidationResult result = fhirValidator.validateWithResult(practitioner);
        assertAll(() -> assertFalse(result.isSuccessful(), "Should have failed validation"),
                () -> assertEquals(1, result.getMessages().size(), "Should have a single failure"));

        // Add an NPI
        practitioner.addIdentifier().setSystem(DPCIdentifierSystem.MBI.getSystem()).setValue("test-mbi");

        final ValidationResult r2 = fhirValidator.validateWithResult(practitioner);
        assertAll(() -> assertFalse(r2.isSuccessful(), "Should have failed validation"),
                () -> assertEquals(2, r2.getMessages().size(), "Should have two failures for ID slice"));

        practitioner.addIdentifier().setSystem(DPCIdentifierSystem.NPPES.getSystem()).setValue("test-npi");
        final ValidationResult r3 = fhirValidator.validateWithResult(practitioner);
        assertTrue(r3.isSuccessful(), "Should have passed");
    }

    private Practitioner generateFakePractitioner() {
        final Practitioner practitioner = new Practitioner();
        final Meta meta = new Meta();
        meta.addProfile(PractitionerProfile.PROFILE_URI);
        practitioner.setMeta(meta);

        practitioner.setId("test-practitioner");
        return practitioner;
    }
}
