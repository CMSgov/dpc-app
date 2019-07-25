package gov.cms.dpc.fhir.validations;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.dstu3.model.StructureDefinition;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProfileValidatorTests {

    private final FhirContext ctx = FhirContext.forDstu3();

    @Test
    void testBasicLoading() {
        final DPCProfileSupport support = new DPCProfileSupport(ctx);

        final List<StructureDefinition> definitions = support.fetchAllStructureDefinitions(ctx);

        assertEquals(5, definitions.size(), "Should not have malformed or invalid resources");
    }
}
