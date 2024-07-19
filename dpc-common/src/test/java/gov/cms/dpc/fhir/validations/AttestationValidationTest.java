package gov.cms.dpc.fhir.validations;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ResultSeverityEnum;
import ca.uhn.fhir.validation.SingleValidationMessage;
import ca.uhn.fhir.validation.ValidationResult;
import gov.cms.dpc.fhir.validations.profiles.AttestationProfile;
import gov.cms.dpc.testing.BufferedLoggerHandler;
import org.hl7.fhir.common.hapi.validation.support.InMemoryTerminologyServerValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.ValidationSupportChain;
import org.hl7.fhir.common.hapi.validation.validator.FhirInstanceValidator;
import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.dstu3.model.codesystems.V3RoleClass;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.sql.Date;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(BufferedLoggerHandler.class)
class AttestationValidationTest {

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
        final StructureDefinition provenanceDefinition = dpcModule.fetchStructureDefinition(AttestationProfile.PROFILE_URI);
        final ValidationResult result = fhirValidator.validateWithResult(provenanceDefinition);
        ArrayList<String> errorMessages = new ArrayList<>();
        for (SingleValidationMessage msg: result.getMessages()) {
            if (msg.getSeverity().equals(ResultSeverityEnum.ERROR)) {
                errorMessages.add(msg.getMessage());
            }
        }
        assertAll(() -> assertEquals(1, errorMessages.size(), "Should have a single message"),
                () -> assertTrue(errorMessages.get(0).contains("Found # expecting a token name")));
    }

    @Test
    void testHasReason() {
        final Provenance provenance = new Provenance();
        provenance.setRecorded(Date.valueOf("1990-01-01"));

        final Meta meta = new Meta();
        meta.addProfile(AttestationProfile.PROFILE_URI);
        provenance.setMeta(meta);
        addAgent(provenance);

        final ValidationResult result = fhirValidator.validateWithResult(provenance);

        assertAll(() -> assertFalse(result.isSuccessful(), "Should not have passed"),
                () -> assertEquals(2, result.getMessages().size(), "Should have two messages for reason slice"));

        // Add a reason, but the wrong system
        Coding c1 = new Coding().setSystem("http://test.local").setCode("TREAT");
        provenance.setReason(List.of(c1));
        final ValidationResult r2 = fhirValidator.validateWithResult(provenance);

        assertAll(() -> assertFalse(r2.isSuccessful(), "Should not have passed"),
                () -> assertEquals(3, r2.getMessages().size(), "Should have errors for the given reason"));

        // Add a reason, but the wrong value
        Coding c2 = new Coding().setSystem("http://hl7.org/fhir/v3/ActReason").setCode("wrongz");
        provenance.setReason(List.of(c2));
        final ValidationResult r3 = fhirValidator.validateWithResult(provenance);

        assertAll(() -> assertFalse(r3.isSuccessful(), "Should not have passed"),
                () -> assertEquals(3 , r3.getMessages().size(), "Should errors for both reasons"));

        // Add a correct reason (which should cause everything to pass)
        Coding c3 = new Coding().setSystem("http://hl7.org/fhir/v3/ActReason").setCode("TREAT");
        provenance.setReason(List.of(c3));
        final ValidationResult r4 = fhirValidator.validateWithResult(provenance);
        assertTrue(r4.isSuccessful(), "Should have passed");
    }

    @Test
    void testRoleAgent() {
        final Provenance provenance = new Provenance();
        provenance.setRecorded(Date.valueOf("1990-01-01"));

        final Meta meta = new Meta();
        meta.addProfile(AttestationProfile.PROFILE_URI);
        provenance.setMeta(meta);
//        provenance.addTarget(new Reference("Patient/test"));
        provenance.addReason().setSystem("http://hl7.org/fhir/v3/ActReason").setCode("TREAT");

        // Add an agent
        final Provenance.ProvenanceAgentComponent agent = new Provenance.ProvenanceAgentComponent();
        agent.setWho(new Reference("Organization/test"));
        agent.setOnBehalfOf(new Reference("Practitioner/test"));

        final Coding roleCode = new Coding();
        roleCode.setSystem(V3RoleClass.AGNT.getSystem());
        roleCode.setCode(V3RoleClass.AGNT.toCode());

        final CodeableConcept roleConcept = new CodeableConcept();
        roleConcept.addCoding(roleCode);
        agent.setRole(Collections.singletonList(roleConcept));
        provenance.addAgent(agent);

        final ValidationResult result = fhirValidator.validateWithResult(provenance);

        assertTrue(result.isSuccessful(), "Should have passed");
    }

    @Test
    void testRoleNoAgent() {
        final Provenance provenance = new Provenance();
        provenance.setRecorded(Date.valueOf("1990-01-01"));

        final Meta meta = new Meta();
        meta.addProfile(AttestationProfile.PROFILE_URI);
        provenance.setMeta(meta);
//        provenance.addTarget(new Reference("Patient/test"));
        provenance.addReason().setSystem("http://hl7.org/fhir/v3/ActReason").setCode("TREAT");
        final Provenance.ProvenanceAgentComponent agent = new Provenance.ProvenanceAgentComponent();
        agent.setWho(new Reference("Organization/test"));
        agent.setOnBehalfOf(new Reference("Practitioner/test"));

        final Coding roleCode = new Coding();
        roleCode.setSystem(V3RoleClass.AFFL.getSystem());
        roleCode.setCode(V3RoleClass.AFFL.toCode());

        final CodeableConcept roleConcept = new CodeableConcept();
        roleConcept.addCoding(roleCode);
        agent.setRole(Collections.singletonList(roleConcept));
        provenance.addAgent(agent);

        final ValidationResult r2 = fhirValidator.validateWithResult(provenance);

        assertAll(() -> assertFalse(r2.isSuccessful(), "Should not have passed"),
                () -> assertEquals(2, r2.getMessages().size(), "Should messages for missing agent and missing slice"));
    }

    @Test
    void testRoleAgentNoBehalf() {
        final Provenance provenance = new Provenance();
        provenance.setRecorded(Date.valueOf("1990-01-01"));

        final Meta meta = new Meta();
        meta.addProfile(AttestationProfile.PROFILE_URI);
        provenance.setMeta(meta);
//        provenance.addTarget(new Reference("Patient/test"));
        provenance.addReason().setSystem("http://hl7.org/fhir/v3/ActReason").setCode("TREAT");

        // Add an agent
        final Provenance.ProvenanceAgentComponent agent = new Provenance.ProvenanceAgentComponent();
        agent.setWho(new Reference("Organization/test"));

        final Coding roleCode = new Coding();
        roleCode.setSystem(V3RoleClass.AGNT.getSystem());
        roleCode.setCode(V3RoleClass.AGNT.toCode());

        final CodeableConcept roleConcept = new CodeableConcept();
        roleConcept.addCoding(roleCode);
        agent.setRole(Collections.singletonList(roleConcept));
        provenance.addAgent(agent);

        final ValidationResult result = fhirValidator.validateWithResult(provenance);

        assertAll(() -> assertFalse(result.isSuccessful(), "Should not have passed"),
                () -> assertEquals(1, result.getMessages().size(), "Should have missing behalf of"));
    }

    @Test
    void testMultipleAgent() {
        final Provenance provenance = new Provenance();
        provenance.setRecorded(Date.valueOf("1990-01-01"));

        final Meta meta = new Meta();
        meta.addProfile(AttestationProfile.PROFILE_URI);
        provenance.setMeta(meta);
//        provenance.addTarget(new Reference("Patient/test"));
        provenance.addReason().setSystem("http://hl7.org/fhir/v3/ActReason").setCode("TREAT");

        // Add an agent
        final Provenance.ProvenanceAgentComponent agent = new Provenance.ProvenanceAgentComponent();
        agent.setWho(new Reference("Organization/test"));
        agent.setOnBehalfOf(new Reference("Practitioner/test"));

        final Coding roleCode = new Coding();
        roleCode.setSystem(V3RoleClass.AGNT.getSystem());
        roleCode.setCode(V3RoleClass.AGNT.toCode());

        final CodeableConcept roleConcept = new CodeableConcept();
        roleConcept.addCoding(roleCode);
        agent.setRole(Collections.singletonList(roleConcept));
        provenance.addAgent(agent);

        // Add affiliate
        final Provenance.ProvenanceAgentComponent a2 = new Provenance.ProvenanceAgentComponent();
        a2.setWho(new Reference("Organization/test"));
        a2.setOnBehalfOf(new Reference("Practitioner/test"));

        final Coding c2 = new Coding();
        c2.setSystem(V3RoleClass.AFFL.getSystem());
        c2.setCode(V3RoleClass.AFFL.toCode());

        final CodeableConcept rc2 = new CodeableConcept();
        rc2.addCoding(c2);
        a2.setRole(Collections.singletonList(rc2));
        provenance.addAgent(a2);

        final ValidationResult result = fhirValidator.validateWithResult(provenance);

        assertTrue(result.isSuccessful(), "Should have passed");
    }

    @Test
    void testDuplicateAgent() {
        final Provenance provenance = new Provenance();
        provenance.setRecorded(Date.valueOf("1990-01-01"));

        final Meta meta = new Meta();
        meta.addProfile(AttestationProfile.PROFILE_URI);
        provenance.setMeta(meta);
//        provenance.addTarget(new Reference("Patient/test"));
        provenance.addReason().setSystem("http://hl7.org/fhir/v3/ActReason").setCode("TREAT");

        // Add an agent
        final Provenance.ProvenanceAgentComponent agent = new Provenance.ProvenanceAgentComponent();
        agent.setWho(new Reference("Organization/test"));
        agent.setOnBehalfOf(new Reference("Practitioner/test"));

        final Coding roleCode = new Coding();
        roleCode.setSystem(V3RoleClass.AGNT.getSystem());
        roleCode.setCode(V3RoleClass.AGNT.toCode());

        final CodeableConcept roleConcept = new CodeableConcept();
        roleConcept.addCoding(roleCode);
        agent.setRole(Collections.singletonList(roleConcept));
        provenance.addAgent(agent);

        // Add another agent
        final Provenance.ProvenanceAgentComponent a2 = new Provenance.ProvenanceAgentComponent();
        a2.setWho(new Reference("Organization/test"));
        a2.setOnBehalfOf(new Reference("Practitioner/test2"));
        a2.setRole(Collections.singletonList(roleConcept));
        provenance.addAgent(a2);


        final ValidationResult result = fhirValidator.validateWithResult(provenance);

        assertAll(() -> assertFalse(result.isSuccessful(), "Should not have passed"),
                () -> assertEquals(1, result.getMessages().size(), "Can only have 1 agent"));
    }

    private void addAgent(Provenance provenance) {

        final Provenance.ProvenanceAgentComponent agent = new Provenance.ProvenanceAgentComponent();
        agent.setWho(new Reference("Organization/test"));
        agent.setOnBehalfOf(new Reference("Practitioner/test"));

        final Coding roleCode = new Coding();
        roleCode.setSystem("http://hl7.org/fhir/v3/RoleClass");
        roleCode.setCode("AGNT");

        final CodeableConcept roleConcept = new CodeableConcept();
        roleConcept.addCoding(roleCode);
        agent.setRole(Collections.singletonList(roleConcept));

        provenance.addAgent(agent);
    }
}
