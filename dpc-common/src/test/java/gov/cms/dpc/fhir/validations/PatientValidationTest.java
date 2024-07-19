package gov.cms.dpc.fhir.validations;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ValidationResult;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import gov.cms.dpc.fhir.validations.profiles.PatientProfile;
import gov.cms.dpc.testing.BufferedLoggerHandler;
import org.hl7.fhir.common.hapi.validation.support.InMemoryTerminologyServerValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.ValidationSupportChain;
import org.hl7.fhir.common.hapi.validation.validator.FhirInstanceValidator;
import org.hl7.fhir.dstu3.model.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.sql.Date;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(BufferedLoggerHandler.class)
class PatientValidationTest {

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
                new InMemoryTerminologyServerValidationSupport(ctx)
        );
        instanceValidator.setValidationSupport(chain);
    }

    @Test
    void definitionIsValid() {
        final StructureDefinition patientDefinition = dpcModule.fetchStructureDefinition(PatientProfile.PROFILE_URI);
        final ValidationResult result = fhirValidator.validateWithResult(patientDefinition);

        assertEquals(1, result.getMessages().size(), "Should have a single message");
        assertTrue(result.getMessages().get(0).getMessage().contains("Found # expecting a token name"));
    }

    @Test
    void testHasName() {

        final Patient patient = generateFakePatient();
        patient.addName().setFamily("Patient").addGiven("Test");
        patient.setBirthDate(Date.valueOf("1990-01-01"));
        patient.setMultipleBirth(new BooleanType(false));
        patient.addTelecom().setSystem(ContactPoint.ContactPointSystem.PHONE).setValue("555-555-5501").setUse(ContactPoint.ContactPointUse.MOBILE);
        patient.addIdentifier().setSystem(DPCIdentifierSystem.MBI.getSystem()).setValue("test-mpi");
        patient.addAddress(generateFakeAddress());

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
        patient.setMultipleBirth(new BooleanType(false));
        patient.addTelecom().setSystem(ContactPoint.ContactPointSystem.PHONE).setValue("555-555-5501").setUse(ContactPoint.ContactPointUse.MOBILE);
        patient.addIdentifier().setSystem(DPCIdentifierSystem.MBI.getSystem()).setValue("test-mpi");
        patient.addAddress(generateFakeAddress());

        final ValidationResult result = fhirValidator.validateWithResult(patient);

        assertAll(() -> assertFalse(result.isSuccessful(), "Should have failed validation"),
                () -> assertEquals(1, result.getMessages().size(), "Should have a single failure"));

    }

    @Test
    void testIdentifier() {
        final Patient patient = generateFakePatient();
        patient.addName().setFamily("Patient").addGiven("Test");
        patient.setBirthDate(Date.valueOf("1990-01-01"));
        patient.setMultipleBirth(new BooleanType(false));
        patient.addTelecom().setSystem(ContactPoint.ContactPointSystem.PHONE).setValue("555-555-5501").setUse(ContactPoint.ContactPointUse.MOBILE);
        patient.addAddress(generateFakeAddress());

        final ValidationResult result = fhirValidator.validateWithResult(patient);
        assertAll(() -> assertFalse(result.isSuccessful(), "Should have failed validation"),
                () -> assertEquals(1, result.getMessages().size(), "Should have a single failure"));

        // Add an NPI
        patient.addIdentifier().setSystem(DPCIdentifierSystem.NPPES.getSystem()).setValue("test-npi");

        final ValidationResult r2 = fhirValidator.validateWithResult(patient);
        assertAll(() -> assertFalse(r2.isSuccessful(), "Should have failed validation"),
                () -> assertEquals(2, r2.getMessages().size(), "Should have two failures for ID slice"));

        patient.addIdentifier().setSystem(DPCIdentifierSystem.MBI.getSystem()).setValue("test-mpi");
        final ValidationResult r3 = fhirValidator.validateWithResult(patient);
        assertTrue(r3.isSuccessful(), "Should have passed");
    }

    Patient generateFakePatient() {

        final Patient patient = new Patient();
        final Meta meta = new Meta();
        meta.addProfile(PatientProfile.PROFILE_URI);

        patient.setMeta(meta);

        patient.setId("test-patient");
        patient.setGender(Enumerations.AdministrativeGender.MALE);
        patient.setManagingOrganization(new Reference("Organization/test-organization"));

        return patient;
    }

    private Address generateFakeAddress() {
        final Address address = new Address();
        address.addLine("1800 Pennsylvania Ave NW");
        address.setCity("Washington");
        address.setState("DC");
        address.setPostalCode("20006");
        address.setCountry("US");

        return address;
    }
}
