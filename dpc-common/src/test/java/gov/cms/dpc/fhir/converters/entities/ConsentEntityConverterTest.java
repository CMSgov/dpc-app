package gov.cms.dpc.fhir.converters.entities;

import ca.uhn.fhir.context.FhirContext;
import gov.cms.dpc.common.consent.entities.ConsentEntity;
import org.hl7.fhir.dstu3.model.Consent;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class ConsentEntityConverterTest {

    private static final String TEST_HICN = "fake_hicn";
    private static final String TEST_MBI = "fake_mbi";

    private ConsentEntityConverterTest() { }

    @Test
    final void convert_correctlyConverts_fromDefaultEntity() {
        ConsentEntity ce = ConsentEntity.defaultConsentEntity(Optional.empty(), Optional.of(TEST_HICN), Optional.of(TEST_MBI));
        final Consent result = ConsentEntityConverter.convert(ce);
        assertNotNull(result);
        assertEquals(Consent.ConsentState.ACTIVE, result.getStatus());
        assertEquals(ConsentEntity.LOINC_CATEGORY, result.getCategoryFirstRep().getCodingFirstRep().getCode());
        assertEquals("Patient/" + TEST_MBI, result.getPatient().getReference());
        assertEquals(ConsentEntity.OPT_IN, result.getPolicyRule());
        assertTrue(result.getPolicy().isEmpty());
        assertDoesNotThrow(() -> FhirContext.forDstu3().newJsonParser().encodeResourceToString(result));
    }

    @Test
    final void convert_correctlyConverts_fromOptOutfEntity() {
        ConsentEntity ce = ConsentEntity.defaultConsentEntity(Optional.empty(), Optional.of(TEST_HICN), Optional.of(TEST_MBI));
        ce.setPolicyCode(ConsentEntity.OPT_OUT);
        final Consent result = ConsentEntityConverter.convert(ce);

        assertEquals(ConsentEntity.OPT_OUT, result.getPolicyRule());
        assertDoesNotThrow(() -> {
            FhirContext.forDstu3().newJsonParser().encodeResourceToString(result);
        });
    }

    @Test
    final void convert_correctlyThrows_whenEntityHasInvalidData() {
        ConsentEntity ce = ConsentEntity.defaultConsentEntity(Optional.empty(), Optional.of(TEST_HICN), Optional.of(TEST_MBI));
        ce.setPolicyCode("BANANA");

        assertThrows(IllegalArgumentException.class, () -> ConsentEntityConverter.convert(ce), "should throw an error with invalid data");
    }
}
