package gov.cms.dpc.fhir.converters.entities.r4;

import ca.uhn.fhir.context.FhirContext;
import gov.cms.dpc.common.consent.entities.ConsentEntity;
import gov.cms.dpc.fhir.converters.FHIREntityConverter;
import jakarta.ws.rs.WebApplicationException;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Consent;
import org.hl7.fhir.r4.model.Reference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class ConsentEntityConverterTest {

    private static final String TEST_FHIR_URL = "http://test-fhir-url";
    private static final String TEST_HICN = "fake_hicn";
    private static final String TEST_MBI = "fake_mbi";

    Consent consent = new Consent();
    String validMbi = "1aa2aa3aa44";
    UUID id = UUID.randomUUID();
    String loinCode = "64292-6";
    @BeforeEach
    void buildConsent() {
        Reference patient = new Reference().setReference(String.format("http://fhir.org/Patient?identity=|%s", validMbi));

        consent.setId(id.toString());
        consent.setStatus(Consent.ConsentState.ACTIVE);
        consent.setPatient(patient);
        CodeableConcept category = new CodeableConcept();
        category.addCoding().setSystem(ConsentEntityConverter.SYSTEM_LOINC).setCode(loinCode).setDisplay(ConsentEntity.CATEGORY_DISPLAY);

        consent.setCategory(List.of(category));
        consent.setPolicyRule(policy(ConsentEntity.OPT_IN));
        consent.setScope(scope(ConsentEntityConverter.SYSTEM_SCOPE, ConsentEntity.SCOPE_CODE));
    }

    private ConsentEntityConverterTest() { }

    @Test
    final void convert_correctlyConverts_fromDefaultEntity() {
        ConsentEntity ce = ConsentEntity.defaultConsentEntity(Optional.empty(), Optional.of(TEST_HICN), Optional.of(TEST_MBI));
        final Consent result = ConsentEntityConverter.toFHIR(ce, TEST_FHIR_URL);

        assertNotNull(result);
        assertEquals(Consent.ConsentState.ACTIVE, result.getStatus());
        assertEquals(ConsentEntity.CATEGORY_LOINC_CODE, result.getCategoryFirstRep().getCodingFirstRep().getCode());
        assertEquals(ConsentEntity.CATEGORY_DISPLAY, result.getCategoryFirstRep().getCodingFirstRep().getDisplay());
        assertEquals(TEST_FHIR_URL + "/Patient?identity=|" + TEST_MBI, result.getPatient().getReference());
        assertEquals(ConsentEntity.OPT_IN, result.getPolicyRule().getCodingFirstRep().getCode());
        assertEquals(ConsentEntityConverter.SYSTEM_POLICY, result.getPolicyRule().getCodingFirstRep().getSystem());
        assertEquals(ConsentEntityConverter.SYSTEM_SCOPE, result.getScope().getCodingFirstRep().getSystem());
        assertEquals(ConsentEntity.SCOPE_CODE, result.getScope().getCodingFirstRep().getCode());
        assertTrue(result.getPolicy().isEmpty());
        assertDoesNotThrow(() -> FhirContext.forR4().newJsonParser().encodeResourceToString(result));
    }

    @Test
    final void convert_correctlyConverts_fromOptOutEntity() {
        ConsentEntity ce = ConsentEntity.defaultConsentEntity(Optional.empty(), Optional.of(TEST_HICN), Optional.of(TEST_MBI));
        ce.setPolicyCode(ConsentEntity.OPT_OUT);
        final Consent result = ConsentEntityConverter.toFHIR(ce, TEST_FHIR_URL);

        assertEquals(ConsentEntity.OPT_OUT, result.getPolicyRule().getCodingFirstRep().getCode());
        assertDoesNotThrow(() -> {
            FhirContext.forR4().newJsonParser().encodeResourceToString(result);
        });
    }

    @Test
    final void convert_correctlyThrows_whenNoMbi() {
        ConsentEntity ce = ConsentEntity.defaultConsentEntity(Optional.empty(), Optional.of(TEST_HICN), Optional.of(TEST_MBI));
        ce.setMbi(null);

        assertThrows(IllegalArgumentException.class, () -> ConsentEntityConverter.toFHIR(ce, TEST_FHIR_URL), "should throw an error with invalid data");
    }

    @Test
    final void convert_correctlyThrows_whenEntityHasInvalidData() {
        ConsentEntity ce = ConsentEntity.defaultConsentEntity(Optional.empty(), Optional.of(TEST_HICN), Optional.of(TEST_MBI));
        ce.setPolicyCode("BANANA");

        assertThrows(IllegalArgumentException.class, () -> ConsentEntityConverter.toFHIR(ce, TEST_FHIR_URL), "should throw an error with invalid data");
    }

    @Test
    void fromFhir() {

        ConsentEntity consentEntity = ConsentEntityConverter.fromFHIR(consent);

        assertEquals(id, consentEntity.getId());
        assertEquals(validMbi, consentEntity.getMbi());
        assertEquals(loinCode, consentEntity.getLoincCode());
        assertEquals(ConsentEntity.OPT_IN, consentEntity.getPolicyCode());
        assertEquals(ConsentEntity.SCOPE_CODE, consentEntity.getScopeCode());
    }

    @Test
    void fromFHIR_OPT_OUT() {
        consent.setPolicyRule(policy(ConsentEntity.OPT_OUT));
        ConsentEntity consentEntity = ConsentEntityConverter.fromFHIR(consent);
        assertEquals(ConsentEntity.OPT_OUT, consentEntity.getPolicyCode());
    }

    @Test
    void fromFHIR_nullThrowsError() {
        Exception exception = assertThrows(WebApplicationException.class, () -> ConsentEntityConverter.fromFHIR(null));
        assertEquals("No consent resource provided", exception.getMessage());
    }

    @Test
    void fromFHIR_InactiveThrowsError() {
        consent.setStatus(Consent.ConsentState.INACTIVE);
        Exception exception = assertThrows(WebApplicationException.class, () -> ConsentEntityConverter.fromFHIR(consent));
        assertEquals("Only active consent records are accepted", exception.getMessage());
    }

    @Test
    void fromFHIR_NoPatientThrowsError() {
        consent.setPatient(null);
        Exception exception = assertThrows(WebApplicationException.class, () -> ConsentEntityConverter.fromFHIR(consent));
        assertEquals("Consent resource must contain patient reference", exception.getMessage());
    }

    @Test
    void fromFHIR_BadMBIThrowsError() {
        Reference patient = new Reference().setReference(String.format("http://fhir.org/Patient?identity=|%s", TEST_MBI));
        consent.setPatient(patient);
        Exception exception = assertThrows(WebApplicationException.class, () -> ConsentEntityConverter.fromFHIR(consent));
        assertEquals("Could not find MBI in patient reference", exception.getMessage());
    }

    @Test
    void fromFHIR_BadPolicyCodeThrowsError() {
        consent.setPolicyRule(policy("INVALID"));

        Exception exception = assertThrows(WebApplicationException.class, () -> ConsentEntityConverter.fromFHIR(consent));
        assertEquals("Policy rule must have coding with system http://terminology.hl7.org/CodeSystem/v3-ActCode and code OPTIN or OPTOUT.", exception.getMessage());
    }

    @Test
    void fromFHIR_NoCategoryThrowsError() {
        consent.setCategory(new ArrayList<>());
        Exception exception = assertThrows(WebApplicationException.class, () -> ConsentEntityConverter.fromFHIR(consent));
        assertEquals("Must include one category", exception.getMessage());
    }

    @Test
    void fromFHIR_NoCodingThrowsError() {
        CodeableConcept category = new CodeableConcept();
        consent.setCategory(List.of(category));
        Exception exception = assertThrows(WebApplicationException.class, () -> ConsentEntityConverter.fromFHIR(consent));
        assertEquals("Category must have one coding", exception.getMessage());
    }

    @Test
    void fromFHIR_BadCategoryLoincThrowsError() {
        CodeableConcept category = new CodeableConcept();
        category.addCoding().setSystem("https://www.google.com").setCode(loinCode).setDisplay(ConsentEntity.CATEGORY_DISPLAY);
        consent.setCategory(List.of(category));
        Exception exception = assertThrows(WebApplicationException.class, () -> ConsentEntityConverter.fromFHIR(consent));
        assertEquals("Category coding must have system http://loinc.org and code 64292-6", exception.getMessage());
    }

    @Test
    void fromFHIR_BadCategoryCodeThrowsError() {
        CodeableConcept category = new CodeableConcept();
        category.addCoding().setSystem("https://www.google.com").setCode("bad code").setDisplay(ConsentEntity.CATEGORY_DISPLAY);
        consent.setCategory(List.of(category));
        Exception exception = assertThrows(WebApplicationException.class, () -> ConsentEntityConverter.fromFHIR(consent));
        assertEquals("Category coding must have system http://loinc.org and code 64292-6", exception.getMessage());
    }

    @Test
    void registryRoundTripPreservesConsentFields() {
        FHIREntityConverter registry = FHIREntityConverter.initialize();
        for (String policy : List.of(ConsentEntity.OPT_IN, ConsentEntity.OPT_OUT)) {
            ConsentEntity original = ConsentEntity.defaultConsentEntity(Optional.of(id), Optional.empty(), Optional.of(validMbi));
            original.setPolicyCode(policy);
            original.setEffectiveDate(java.time.LocalDate.of(2020, 2, 3));

            Consent resource = registry.toFHIR(Consent.class, original);
            assertEquals("Patient?identity=|" + validMbi, resource.getPatient().getReference());
            String json = FhirContext.forR4().newJsonParser().encodeResourceToString(resource);
            Consent parsed = FhirContext.forR4().newJsonParser().parseResource(Consent.class, json);
            ConsentEntity converted = registry.fromFHIR(ConsentEntity.class, parsed);
            assertEquals(original.getId(), converted.getId());
            assertEquals(original.getMbi(), converted.getMbi());
            assertEquals(original.getPolicyCode(), converted.getPolicyCode());
            assertEquals(original.getScopeCode(), converted.getScopeCode());
            assertEquals(original.getLoincCode(), converted.getLoincCode());
            assertEquals(original.getEffectiveDate(), converted.getEffectiveDate());
        }
    }

    @Test
    void fromFHIR_RejectsMissingOrUnsupportedScope() {
        for (CodeableConcept scope : List.of(new CodeableConcept(),
                scope("http://wrong-system", ConsentEntity.SCOPE_CODE),
                scope(ConsentEntityConverter.SYSTEM_SCOPE, "research"))) {
            consent.setScope(scope);
            WebApplicationException exception = assertThrows(WebApplicationException.class,
                    () -> ConsentEntityConverter.fromFHIR(consent));
            assertEquals(422, exception.getResponse().getStatus());
        }
    }

    @Test
    void fromFHIR_RejectsMissingWrongSystemOrAmbiguousPolicy() {
        CodeableConcept wrongSystem = policy(ConsentEntity.OPT_IN);
        wrongSystem.getCodingFirstRep().setSystem("http://wrong-system");
        CodeableConcept ambiguous = policy(ConsentEntity.OPT_IN);
        ambiguous.addCoding().setSystem(ConsentEntityConverter.SYSTEM_POLICY).setCode(ConsentEntity.OPT_OUT);
        for (CodeableConcept policy : List.of(new CodeableConcept(), wrongSystem, ambiguous)) {
            consent.setPolicyRule(policy);
            WebApplicationException exception = assertThrows(WebApplicationException.class,
                    () -> ConsentEntityConverter.fromFHIR(consent));
            assertEquals(400, exception.getResponse().getStatus());
        }
    }

    @Test
    void toFHIR_UsesStoredScope() {
        ConsentEntity entity = ConsentEntity.defaultConsentEntity(Optional.empty(), Optional.empty(), Optional.of(validMbi));
        entity.setScopeCode("research");

        Consent resource = ConsentEntityConverter.toFHIR(entity, TEST_FHIR_URL);

        assertEquals(ConsentEntityConverter.SYSTEM_SCOPE, resource.getScope().getCodingFirstRep().getSystem());
        assertEquals("research", resource.getScope().getCodingFirstRep().getCode());
    }

    private static CodeableConcept policy(String code) {
        CodeableConcept policy = new CodeableConcept();
        policy.addCoding().setSystem(ConsentEntityConverter.SYSTEM_POLICY).setCode(code);
        return policy;
    }

    private static CodeableConcept scope(String system, String code) {
        CodeableConcept scope = new CodeableConcept();
        scope.addCoding().setSystem(system).setCode(code);
        return scope;
    }
}
