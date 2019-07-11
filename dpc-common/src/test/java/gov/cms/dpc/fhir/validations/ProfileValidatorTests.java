package gov.cms.dpc.fhir.validations;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.dstu3.model.StructureDefinition;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.MissingResourceException;

import static org.junit.jupiter.api.Assertions.*;

class ProfileValidatorTests {

    private final FhirContext ctx = FhirContext.forDstu3();

    @Test
    void testBasicLoading() {
        final DPCProfileSupport DPCProfileSupport = new DPCProfileSupport(ctx);

        final List<StructureDefinition> definitions = DPCProfileSupport.fetchAllStructureDefinitions(ctx);

        assertEquals(1, definitions.size(), "Should not have malformed or invalid resources");
    }

    @Test
    @Disabled
    void testLoadingBadResources() {
        final DPCProfileSupport DPCProfileSupport = new DPCProfileSupport(ctx);

        final List<StructureDefinition> definitions = DPCProfileSupport.fetchAllStructureDefinitions(ctx);
        assertTrue(definitions.isEmpty(), "Should not have parsed anything");
    }

    @Test
    @Disabled
    void testBadPrefix() {
        assertThrows(MissingResourceException.class, () -> new DPCProfileSupport(ctx));
    }
}
