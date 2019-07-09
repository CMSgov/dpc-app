package gov.cms.dpc.fhir.validations;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ValidationResult;
import gov.cms.dpc.fhir.validations.definitions.DefinitionConstants;
import org.hl7.fhir.dstu3.hapi.ctx.DefaultProfileValidationSupport;
import org.hl7.fhir.dstu3.hapi.validation.FhirInstanceValidator;
import org.hl7.fhir.dstu3.hapi.validation.ValidationSupportChain;
import org.hl7.fhir.dstu3.model.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Date;

import static org.junit.jupiter.api.Assertions.*;

class PatientValidation {

    private static FhirValidator fhirValidator;
    private static ValidationModule dpcModule;
    private static FhirContext ctx;

    @BeforeAll
    static void setup() {
        ctx = FhirContext.forDstu3();
        final FhirInstanceValidator instanceValidator = new FhirInstanceValidator();

        fhirValidator = ctx.newValidator();
        fhirValidator.setValidateAgainstStandardSchematron(false);
        fhirValidator.setValidateAgainstStandardSchema(false);
        fhirValidator.registerValidatorModule(instanceValidator);


        dpcModule = new ValidationModule(ctx);
        final ValidationSupportChain chain = new ValidationSupportChain(new DefaultProfileValidationSupport(), dpcModule);
        instanceValidator.setValidationSupport(chain);
    }

    @Test
    void definitionIsValid() {
        final StructureDefinition patientDefinition = dpcModule.fetchStructureDefinition(ctx, DefinitionConstants.DPC_PATIENT_URI.toString());
        final ValidationResult result = fhirValidator.validateWithResult(patientDefinition);
        assertTrue(result.isSuccessful(), "Should be a valid structure definition");
    }

    @Test
    void testHasName() {

        final Patient patient = generateFakePatient();
        patient.addName().setFamily("Patient").addGiven("Test");
        patient.setBirthDate(Date.valueOf("1990-01-01"));
        patient.addTelecom().setSystem(ContactPoint.ContactPointSystem.EMAIL).setValue("test@example.com");

        final ValidationResult result = fhirValidator.validateWithResult(patient);

        assertTrue(result.isSuccessful(), "Should have passed");

        // Add a bad name
        patient.addName().setFamily("Missing");
        final ValidationResult r2 = fhirValidator.validateWithResult(patient);

        assertAll(() -> assertFalse(r2.isSuccessful(), "Should have failed validation"),
                () -> assertEquals(1, r2.getMessages().size(), "Should have a single failure"));
    }

    @Test
    void testHasBirthday() {

        final Patient patient = generateFakePatient();
        patient.addName().setFamily("Patient").addGiven("Test");
        patient.addTelecom().setSystem(ContactPoint.ContactPointSystem.EMAIL).setValue("test@example.com");

        final ValidationResult result = fhirValidator.validateWithResult(patient);

        assertAll(() -> assertFalse(result.isSuccessful(), "Should have failed validation"),
                () -> assertEquals(1, result.getMessages().size(), "Should have a single failure"));

    }

    @Test
    void testBirthOrder() {
        // Test optional birth order
        final Patient patient = generateFakePatient();
        patient.addName().setFamily("Patient").addGiven("Test");
        patient.setBirthDate(Date.valueOf("1990-01-01"));
        patient.addTelecom().setSystem(ContactPoint.ContactPointSystem.EMAIL).setValue("test@example.com");

        final ValidationResult result = fhirValidator.validateWithResult(patient);
        assertTrue(result.isSuccessful(), "Should have passed");

        // Add a boolean birth order
        patient.setMultipleBirth(new BooleanType(true));
        final ValidationResult r2 = fhirValidator.validateWithResult(patient);
        assertAll(() -> assertFalse(r2.isSuccessful(), "Should have failed validation"),
                () -> assertEquals(1, r2.getMessages().size(), "Should have a single failure"));

        patient.setMultipleBirth(new IntegerType(2));
        final ValidationResult r3 = fhirValidator.validateWithResult(patient);
        assertTrue(r3.isSuccessful(), "Should have passed");
    }

    @Test
    void testContact() {
        // Test must have one of Email or SMS
        final Patient patient = generateFakePatient();
        patient.addName().setFamily("Patient").addGiven("Test");
        patient.setBirthDate(Date.valueOf("1990-01-01"));
//        patient.addTelecom().setSystem(ContactPoint.ContactPointSystem.PHONE).setValue("555-555-5500");

        final ValidationResult result = fhirValidator.validateWithResult(patient);
        assertAll(() -> assertFalse(result.isSuccessful(), "Should have failed validation"),
                () -> assertEquals(1, result.getMessages().size(), "Should have a single failure"));

        patient.addTelecom().setSystem(ContactPoint.ContactPointSystem.SMS).setValue("555-555-5501").setUse(ContactPoint.ContactPointUse.MOBILE);
        final ValidationResult r2 = fhirValidator.validateWithResult(patient);
        assertTrue(r2.isSuccessful(), "Should have passed");
    }

    Patient generateFakePatient() {

        final Patient patient = new Patient();
        final Meta meta = new Meta();
        meta.addProfile(DefinitionConstants.DPC_PATIENT_URI.toString());

        patient.setMeta(meta);

        patient.setId("test-patient");
        patient.setGender(Enumerations.AdministrativeGender.MALE);

        return patient;
    }
}
