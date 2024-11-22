package gov.cms.dpc.fhir.validations;

import ca.uhn.fhir.context.FhirContext;
import gov.cms.dpc.testing.BufferedLoggerHandler;
import org.hl7.fhir.dstu3.model.StructureDefinition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.DisplayName;

@ExtendWith(BufferedLoggerHandler.class)
@DisplayName("Profile validation")
class ProfileValidatorTests {

    private final FhirContext ctx = FhirContext.forDstu3();

    @Test
    @DisplayName("Load DPC profile 🥳")
    void testBasicLoading() {
        final DPCProfileSupport support = new DPCProfileSupport(ctx);

        final List<StructureDefinition> definitions = support.fetchAllStructureDefinitions();

        assertEquals(7, definitions.size(), "Should not have malformed or invalid resources");
    }
}
