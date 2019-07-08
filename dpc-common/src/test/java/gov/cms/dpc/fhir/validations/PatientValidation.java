package gov.cms.dpc.fhir.validations;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ValidationResult;
import gov.cms.dpc.fhir.validations.definitions.MatchablePatient;
import org.hl7.fhir.dstu3.conformance.ProfileUtilities;
import org.hl7.fhir.dstu3.hapi.ctx.DefaultProfileValidationSupport;
import org.hl7.fhir.dstu3.hapi.ctx.HapiWorkerContext;
import org.hl7.fhir.dstu3.hapi.validation.FhirInstanceValidator;
import org.hl7.fhir.dstu3.hapi.validation.PrePopulatedValidationSupport;
import org.hl7.fhir.dstu3.hapi.validation.ValidationSupportChain;
import org.hl7.fhir.dstu3.model.Meta;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.StructureDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Date;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PatientValidation {

    private FhirValidator fhirValidator;

    @BeforeEach
    void setup() {
        final FhirContext ctx = FhirContext.forDstu3();
        final FhirInstanceValidator instanceValidator = new FhirInstanceValidator();

        final PrePopulatedValidationSupport ts = new PrePopulatedValidationSupport();
        final StructureDefinition patientDef = MatchablePatient.definition();

        fhirValidator = ctx.newValidator();
        fhirValidator.setValidateAgainstStandardSchematron(false);
        fhirValidator.setValidateAgainstStandardSchema(false);
        fhirValidator.registerValidatorModule(instanceValidator);

        ts.addStructureDefinition(patientDef);


        final ValidationSupportChain chain = new ValidationSupportChain(new DefaultProfileValidationSupport(), new ValidationModule());
        instanceValidator.setValidationSupport(chain);
    }

    @Test
    void definitionIsValid() {
        final ValidationResult result = fhirValidator.validateWithResult(MatchablePatient.definition());
        assertTrue(result.isSuccessful(), "Should be a valid structure definition");
    }

    @Test
    void testHasName() {
        final Patient patient = new Patient();
        final Meta meta = new Meta();
        meta.addProfile(MatchablePatient.definition().getUrl());

        patient.setMeta(meta);

        patient.setId("test-patient");
        patient.addName().setFamily("Patient").addGiven("Test");
        patient.addName().setFamily("Other");
        patient.setBirthDate(Date.valueOf("1990-01-01"));

        final ValidationResult result = fhirValidator.validateWithResult(patient);

        assertTrue(result.isSuccessful(), "Should have passed");
    }
}
