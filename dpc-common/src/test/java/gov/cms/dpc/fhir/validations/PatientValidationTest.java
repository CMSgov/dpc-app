package gov.cms.dpc.fhir.validations;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ResultSeverityEnum;
import ca.uhn.fhir.validation.ValidationResult;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import gov.cms.dpc.fhir.validations.profiles.PatientProfile;
import org.hl7.fhir.dstu3.hapi.ctx.DefaultProfileValidationSupport;
import org.hl7.fhir.dstu3.hapi.validation.FhirInstanceValidator;
import org.hl7.fhir.dstu3.hapi.validation.ValidationSupportChain;
import org.hl7.fhir.dstu3.model.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.sql.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PatientValidationTest {

    private static FhirValidator fhirValidator;
    private static DPCProfileSupport dpcModule;
    private static FhirContext ctx;

    @BeforeAll
    static void setup() {
        ctx = FhirContext.forDstu3();
        final FhirInstanceValidator instanceValidator = new FhirInstanceValidator();

        fhirValidator = ctx.newValidator();
        fhirValidator.setValidateAgainstStandardSchematron(false);
        fhirValidator.setValidateAgainstStandardSchema(false);
        fhirValidator.registerValidatorModule(instanceValidator);


        dpcModule = new DPCProfileSupport(ctx);
        final ValidationSupportChain chain = new ValidationSupportChain(new DefaultProfileValidationSupport(), dpcModule);
        instanceValidator.setValidationSupport(chain);
    }

    @Test
    void definitionIsValid() {
        final StructureDefinition patientDefinition = dpcModule.fetchStructureDefinition(ctx, PatientProfile.PROFILE_URI);
        final ValidationResult result = fhirValidator.validateWithResult(patientDefinition);
        // There should be a single failure, but we know about it.
        // This needs to stay until https://github.com/jamesagnew/hapi-fhir/pull/1375 lands in upstream.
        assertAll(() -> assertEquals(1, result.getMessages().size(), "Should have a single failure"),
                () -> assertEquals("URI values cannot have whitespace", result.getMessages().get(0).getMessage(), "Should have URI failure"));
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
    void testBirthOrder() {
        // Test optional birth order
        final Patient patient = generateFakePatient();
        patient.addName().setFamily("Patient").addGiven("Test");
        patient.setBirthDate(Date.valueOf("1990-01-01"));
        patient.addTelecom().setSystem(ContactPoint.ContactPointSystem.PHONE).setValue("555-555-5501").setUse(ContactPoint.ContactPointUse.MOBILE);
        patient.addIdentifier().setSystem(DPCIdentifierSystem.MBI.getSystem()).setValue("test-mpi");
        patient.addAddress(generateFakeAddress());

        final ValidationResult result = fhirValidator.validateWithResult(patient);
        final Executable singleWarning = () -> assertEquals(ResultSeverityEnum.WARNING, result.getMessages().get(0).getSeverity(), "Should have warning message");
        assertAll(() -> assertTrue(result.isSuccessful(), "Should have passed with warning"),
                () -> assertEquals(1, result.getMessages().size(), "Should have a single warning message"),
                singleWarning);

        // Add a boolean birth order
        patient.setMultipleBirth(new BooleanType(true));
        final ValidationResult r2 = fhirValidator.validateWithResult(patient);
        assertAll(() -> assertTrue(r2.isSuccessful(), "Should have failed validation"),
                () -> assertEquals(1, r2.getMessages().size(), "Should have a single failure"),
                singleWarning);

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
        patient.setMultipleBirth(new BooleanType(false));
        patient.addTelecom().setSystem(ContactPoint.ContactPointSystem.PHONE).setValue("555-555-5500");
        patient.addIdentifier().setSystem(DPCIdentifierSystem.MBI.getSystem()).setValue("test-mpi");
        patient.addAddress(generateFakeAddress());

        final ValidationResult result = fhirValidator.validateWithResult(patient);
        assertAll(() -> assertFalse(result.isSuccessful(), "Should have failed validation"),
                () -> assertEquals(1, result.getMessages().size(), "Should have a single failure"));

        patient.addTelecom().setSystem(ContactPoint.ContactPointSystem.SMS).setValue("555-555-5501").setUse(ContactPoint.ContactPointUse.MOBILE);
        final ValidationResult r2 = fhirValidator.validateWithResult(patient);
        assertTrue(r2.isSuccessful(), "Should have passed");
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

    @Test
    void testAddress() {
        final Patient patient = generateFakePatient();
        patient.addName().setFamily("Patient").addGiven("Test");
        patient.setBirthDate(Date.valueOf("1990-01-01"));
        patient.setMultipleBirth(new BooleanType(false));
        patient.addTelecom().setSystem(ContactPoint.ContactPointSystem.PHONE).setValue("555-555-5501").setUse(ContactPoint.ContactPointUse.MOBILE);
        patient.addIdentifier().setSystem(DPCIdentifierSystem.MBI.getSystem()).setValue("test-mpi");

        final ValidationResult result = fhirValidator.validateWithResult(patient);
        assertAll(() -> assertFalse(result.isSuccessful(), "Should have failed validation"),
                () -> assertEquals(1, result.getMessages().size(), "Should have a single failure"));

        // Try with address text

        final Address address = new Address();
        address.setText("1800 Nothing Nowhere, CO 11111");

        patient.setAddress(List.of(address));

        final ValidationResult r2 = fhirValidator.validateWithResult(patient);
        assertAll(() -> assertFalse(r2.isSuccessful(), "Should have failed validation"),
                () -> assertEquals(5, r2.getMessages().size(), "Should have failures for all elements"));

        patient.setAddress(List.of(generateFakeAddress()));

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

        return patient;
    }

    Address generateFakeAddress() {
        final Address address = new Address();
        address.addLine("1800 Pennsylvania Ave NW");
        address.setCity("Washington");
        address.setState("DC");
        address.setPostalCode("20006");
        address.setCountry("US");

        return address;
    }
}
