package gov.cms.dpc.fhir.validations.r4;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ValidationResult;
import gov.cms.dpc.fhir.validations.profiles.r4.AttestationProfile;
import gov.cms.dpc.testing.BufferedLoggerHandler;
import org.hl7.fhir.common.hapi.validation.support.InMemoryTerminologyServerValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.ValidationSupportChain;
import org.hl7.fhir.common.hapi.validation.validator.FhirInstanceValidator;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.sql.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(BufferedLoggerHandler.class)
class AttestationValidationTest {

    private static final String REASON_SYSTEM = "http://hl7.org/fhir/v3/ActReason";
    private static final String ROLE_SYSTEM = "http://hl7.org/fhir/v3/RoleClass";
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
        final StructureDefinition provenanceDefinition = assertInstanceOf(StructureDefinition.class,
                dpcModule.fetchStructureDefinition(AttestationProfile.PROFILE_URI));
        assertTrue(provenanceDefinition.hasSnapshot(), "R4 attestation profile should have a snapshot");
        final ValidationResult result = fhirValidator.validateWithResult(provenanceDefinition);
        assertTrue(result.isSuccessful(), () -> result.getMessages().toString());
    }

    @Test
    void testHasReason() {
        final Provenance provenance = generateFakeProvenance();
        provenance.setReason(null);
        assertFalse(fhirValidator.validateWithResult(provenance).isSuccessful(), "Should require a TREAT reason");

        provenance.addReason(reason("http://test.local", "TREAT"));
        assertFalse(fhirValidator.validateWithResult(provenance).isSuccessful(), "A reason with the wrong system should fail");

        provenance.setReason(List.of(reason(REASON_SYSTEM, "wrongz")));
        assertFalse(fhirValidator.validateWithResult(provenance).isSuccessful(), "A reason with the wrong code should fail");

        provenance.setReason(List.of(reason(REASON_SYSTEM, "TREAT")));
        final ValidationResult result = fhirValidator.validateWithResult(provenance);
        assertTrue(result.isSuccessful(), () -> result.getMessages().toString());
    }

    @Test
    void testRoleAgent() {
        final ValidationResult result = fhirValidator.validateWithResult(generateFakeProvenance());
        assertTrue(result.isSuccessful(), () -> result.getMessages().toString());
    }

    @Test
    void testRoleNoAgent() {
        final Provenance provenance = generateFakeProvenance();
        provenance.getAgentFirstRep().setRole(List.of(role("AFFL")));

        final ValidationResult result = fhirValidator.validateWithResult(provenance);
        assertFalse(result.isSuccessful(), "An affiliate should not satisfy the performer requirement");
    }

    @Test
    void testRoleAgentNoBehalf() {
        final Provenance provenance = generateFakeProvenance();
        provenance.getAgentFirstRep().setOnBehalfOf(null);

        final ValidationResult result = fhirValidator.validateWithResult(provenance);
        assertFalse(result.isSuccessful(), "The performer should require an onBehalfOf practitioner");
    }

    @Test
    void testMultipleAgent() {
        final Provenance provenance = generateFakeProvenance();
        provenance.addAgent(agent("AFFL"));

        final ValidationResult result = fhirValidator.validateWithResult(provenance);
        assertTrue(result.isSuccessful(), () -> result.getMessages().toString());
    }

    @Test
    void testDuplicateAgent() {
        final Provenance provenance = generateFakeProvenance();
        provenance.addAgent(agent("AGNT"));

        final ValidationResult result = fhirValidator.validateWithResult(provenance);
        assertFalse(result.isSuccessful(), "The profile should allow only one performer");
    }

    private Provenance generateFakeProvenance() {
        final Provenance provenance = new Provenance();
        provenance.setMeta(new Meta().addProfile(AttestationProfile.PROFILE_URI));
        provenance.setRecorded(Date.valueOf("1990-01-01"));
        provenance.addTarget(new Reference("Patient/test"));
        provenance.addReason(reason(REASON_SYSTEM, "TREAT"));
        provenance.addAgent(agent("AGNT"));
        return provenance;
    }

    private CodeableConcept reason(String system, String code) {
        return new CodeableConcept().addCoding(new Coding(system, code, null));
    }

    private CodeableConcept role(String code) {
        return new CodeableConcept().addCoding(new Coding(ROLE_SYSTEM, code, null));
    }

    private Provenance.ProvenanceAgentComponent agent(String roleCode) {
        final Provenance.ProvenanceAgentComponent agent = new Provenance.ProvenanceAgentComponent();
        agent.setWho(new Reference("Organization/test"));
        agent.setOnBehalfOf(new Reference("Practitioner/test"));
        agent.setRole(List.of(role(roleCode)));
        return agent;
    }
}
