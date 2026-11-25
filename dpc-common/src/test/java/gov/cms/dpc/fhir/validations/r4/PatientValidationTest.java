package gov.cms.dpc.fhir.validations.r4;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ValidationResult;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import gov.cms.dpc.fhir.validations.profiles.r4.PatientProfile;
import gov.cms.dpc.testing.BufferedLoggerHandler;
import org.hl7.fhir.common.hapi.validation.support.InMemoryTerminologyServerValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.ValidationSupportChain;
import org.hl7.fhir.common.hapi.validation.validator.FhirInstanceValidator;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.sql.Date;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(BufferedLoggerHandler.class)
class PatientValidationTest {

    private static FhirValidator fhirValidator;
    private static DPCProfileSupport dpcModule;

    @BeforeAll
    static void setup() {
        FhirContext ctx = FhirContext.forR4();
        final FhirInstanceValidator instanceValidator = new FhirInstanceValidator(ctx);

        fhirValidator = ctx.newValidator();
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
        final StructureDefinition patientDefinition = assertInstanceOf(StructureDefinition.class,
                dpcModule.fetchStructureDefinition(PatientProfile.PROFILE_URI));
        assertTrue(patientDefinition.hasSnapshot(), "R4 patient profile should have a snapshot");
        final ValidationResult result = fhirValidator.validateWithResult(patientDefinition);
        assertTrue(result.isSuccessful(), () -> result.getMessages().toString());
    }

    @Test
    void testHasName() {
        final Patient patient = generateFakePatient();
        addRequiredData(patient);

        final ValidationResult result = fhirValidator.validateWithResult(patient);
        assertTrue(result.isSuccessful(), () -> result.getMessages().toString());

        patient.addName().setFamily("Missing");
        final ValidationResult r2 = fhirValidator.validateWithResult(patient);
        assertFalse(r2.isSuccessful(), "Every name should include a given name");
    }

    @Test
    void testHasBirthday() {
        final Patient patient = generateFakePatient();
        addRequiredData(patient);
        patient.setBirthDate(null);

        final ValidationResult result = fhirValidator.validateWithResult(patient);
        assertFalse(result.isSuccessful(), "Should require a birth date");
    }

    @Test
    void testIdentifier() {
        final Patient patient = generateFakePatient();
        addRequiredData(patient);
        patient.setIdentifier(null);

        final ValidationResult result = fhirValidator.validateWithResult(patient);
        assertFalse(result.isSuccessful(), "Should require an MBI");

        patient.addIdentifier().setSystem(DPCIdentifierSystem.NPPES.getSystem()).setValue("test-npi");
        final ValidationResult r2 = fhirValidator.validateWithResult(patient);
        assertFalse(r2.isSuccessful(), "An NPI should not satisfy the MBI requirement");

        patient.addIdentifier().setSystem(DPCIdentifierSystem.MBI.getSystem()).setValue("test-mpi");
        final ValidationResult r3 = fhirValidator.validateWithResult(patient);
        assertTrue(r3.isSuccessful(), () -> r3.getMessages().toString());
    }

    private Patient generateFakePatient() {
        final Patient patient = new Patient();
        patient.setMeta(new Meta().addProfile(PatientProfile.PROFILE_URI));
        patient.setId("test-patient");
        patient.setGender(Enumerations.AdministrativeGender.MALE);
        patient.setManagingOrganization(new Reference("Organization/test-organization"));
        return patient;
    }

    private void addRequiredData(Patient patient) {
        patient.addName().setFamily("Patient").addGiven("Test");
        patient.setBirthDate(Date.valueOf("1990-01-01"));
        patient.setMultipleBirth(new BooleanType(false));
        patient.addTelecom().setSystem(ContactPoint.ContactPointSystem.PHONE).setValue("555-555-5501").setUse(ContactPoint.ContactPointUse.MOBILE);
        patient.addIdentifier().setSystem(DPCIdentifierSystem.MBI.getSystem()).setValue("test-mpi");
        patient.addAddress(generateFakeAddress());
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
