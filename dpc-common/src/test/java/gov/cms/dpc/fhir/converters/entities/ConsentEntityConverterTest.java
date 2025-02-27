package gov.cms.dpc.fhir.converters.entities;

import ca.uhn.fhir.context.FhirContext;
import gov.cms.dpc.common.consent.entities.ConsentEntity;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Consent;
import org.hl7.fhir.dstu3.model.Reference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.ws.rs.WebApplicationException;
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
        consent.setPolicyRule(ConsentEntityConverter.OPT_IN_MAGIC);
    }

    private ConsentEntityConverterTest() { }

    @Test
    final void convert_correctlyConverts_fromDefaultEntity() {
        ConsentEntity ce = ConsentEntity.defaultConsentEntity(Optional.empty(), Optional.of(TEST_HICN), Optional.of(TEST_MBI));
        final Consent result = ConsentEntityConverter.toFhir(ce, TEST_FHIR_URL);

        assertNotNull(result);
        assertEquals(Consent.ConsentState.ACTIVE, result.getStatus());
        assertEquals(ConsentEntity.CATEGORY_LOINC_CODE, result.getCategoryFirstRep().getCodingFirstRep().getCode());
        assertEquals(ConsentEntity.CATEGORY_DISPLAY, result.getCategoryFirstRep().getCodingFirstRep().getDisplay());
        assertEquals(TEST_FHIR_URL + "/Patient?identity=|" + TEST_MBI, result.getPatient().getReference());
        assertEquals(ConsentEntityConverter.OPT_IN_MAGIC, result.getPolicyRule());
        assertTrue(result.getPolicy().isEmpty());
        assertDoesNotThrow(() -> FhirContext.forDstu3().newJsonParser().encodeResourceToString(result));
    }

    @Test
    final void convert_correctlyConverts_fromOptOutEntity() {
        ConsentEntity ce = ConsentEntity.defaultConsentEntity(Optional.empty(), Optional.of(TEST_HICN), Optional.of(TEST_MBI));
        ce.setPolicyCode(ConsentEntity.OPT_OUT);
        final Consent result = ConsentEntityConverter.toFhir(ce, TEST_FHIR_URL);

        assertEquals(ConsentEntityConverter.OPT_OUT_MAGIC, result.getPolicyRule());
        assertDoesNotThrow(() -> {
            FhirContext.forDstu3().newJsonParser().encodeResourceToString(result);
        });
    }

    @Test
    final void convert_correctlyThrows_whenNoMbi() {
        ConsentEntity ce = ConsentEntity.defaultConsentEntity(Optional.empty(), Optional.of(TEST_HICN), Optional.of(TEST_MBI));
        ce.setMbi(null);

        assertThrows(IllegalArgumentException.class, () -> ConsentEntityConverter.toFhir(ce, TEST_FHIR_URL), "should throw an error with invalid data");
    }

    @Test
    final void convert_correctlyThrows_whenEntityHasInvalidData() {
        ConsentEntity ce = ConsentEntity.defaultConsentEntity(Optional.empty(), Optional.of(TEST_HICN), Optional.of(TEST_MBI));
        ce.setPolicyCode("BANANA");

        assertThrows(IllegalArgumentException.class, () -> ConsentEntityConverter.toFhir(ce, TEST_FHIR_URL), "should throw an error with invalid data");
    }

    @Test
    void fromFhir() {

        ConsentEntity consentEntity = ConsentEntityConverter.fromFhir(consent);

        assertEquals(id, consentEntity.getId());
        assertEquals(validMbi, consentEntity.getMbi());
        assertEquals(loinCode, consentEntity.getLoincCode());
        assertEquals(ConsentEntity.OPT_IN, consentEntity.getPolicyCode());
    }

    @Test
    void fromFHIR_OPT_OUT() {
        consent.setPolicyRule(ConsentEntityConverter.OPT_OUT_MAGIC);
        ConsentEntity consentEntity = ConsentEntityConverter.fromFhir(consent);
        assertEquals(ConsentEntity.OPT_OUT, consentEntity.getPolicyCode());
    }

    @Test
    void fromFHIR_nullThrowsError() {
        Exception exception = assertThrows(WebApplicationException.class, () -> {
            ConsentEntityConverter.fromFhir(null);
        });
        assertEquals("No consent resource provided", exception.getMessage());
    }

    @Test
    void fromFHIR_InactiveThrowsError() {
        consent.setStatus(Consent.ConsentState.INACTIVE);
        Exception exception = assertThrows(WebApplicationException.class, () -> {
            ConsentEntityConverter.fromFhir(consent);
        });
        assertEquals("Only active consent records are accepted", exception.getMessage());
    }

    @Test
    void fromFHIR_NoPatientThrowsError() {
        consent.setPatient(null);
        Exception exception = assertThrows(WebApplicationException.class, () -> {
            ConsentEntityConverter.fromFhir(consent);
        });
        assertEquals("Consent resource must contain patient reference", exception.getMessage());
    }

    @Test
    void fromFHIR_BadMBIThrowsError() {
        Reference patient = new Reference().setReference(String.format("http://fhir.org/Patient?identity=|%s", TEST_MBI));
        consent.setPatient(patient);
        Exception exception = assertThrows(WebApplicationException.class, () -> {
            ConsentEntityConverter.fromFhir(consent);
        });
        assertEquals("Could not find MBI in patient reference", exception.getMessage());
    }

    @Test
    void fromFHIR_BadPolicyUriThrowsError() {
        consent.setPolicyRule("https://www.google.com");

        Exception exception = assertThrows(WebApplicationException.class, () -> {
            ConsentEntityConverter.fromFhir(consent);
        });
        assertEquals("Policy rule must be http://hl7.org/fhir/ConsentPolicy/opt-in or http://hl7.org/fhir/ConsentPolicy/opt-out.", exception.getMessage());
    }

    @Test
    void fromFHIR_NoCategoryThrowsError() {
        consent.setCategory(new ArrayList<>());
        Exception exception = assertThrows(WebApplicationException.class, () -> {
            ConsentEntityConverter.fromFhir(consent);
        });
        assertEquals("Must include one category", exception.getMessage());
    }

    @Test
    void fromFHIR_NoCodingThrowsError() {
        CodeableConcept category = new CodeableConcept();
        consent.setCategory(List.of(category));
        Exception exception = assertThrows(WebApplicationException.class, () -> {
            ConsentEntityConverter.fromFhir(consent);
        });
        assertEquals("Category must have one coding", exception.getMessage());
    }

    @Test
    void fromFHIR_BadCategoryLoincThrowsError() {
        CodeableConcept category = new CodeableConcept();
        category.addCoding().setSystem("https://www.google.com").setCode(loinCode).setDisplay(ConsentEntity.CATEGORY_DISPLAY);
        consent.setCategory(List.of(category));
        Exception exception = assertThrows(WebApplicationException.class, () -> {
            ConsentEntityConverter.fromFhir(consent);
        });
        assertEquals("Category coding must have system http://loinc.org and code 64292-6", exception.getMessage());
    }

    @Test
    void fromFHIR_BadCategoryCodeThrowsError() {
        CodeableConcept category = new CodeableConcept();
        category.addCoding().setSystem("https://www.google.com").setCode("bad code").setDisplay(ConsentEntity.CATEGORY_DISPLAY);
        consent.setCategory(List.of(category));
        Exception exception = assertThrows(WebApplicationException.class, () -> {
            ConsentEntityConverter.fromFhir(consent);
        });
        assertEquals("Category coding must have system http://loinc.org and code 64292-6", exception.getMessage());
    }
}
