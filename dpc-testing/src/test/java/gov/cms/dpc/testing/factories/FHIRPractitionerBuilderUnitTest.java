package gov.cms.dpc.testing.factories;

import org.hl7.fhir.dstu3.model.Practitioner;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;

@DisplayName("FHIR-based Practitioner Factory tests")
public class FHIRPractitionerBuilderUnitTest {

    @Test
    @DisplayName("Set up new FHIR Practitioner builder 🥳")
    public void newBuilder() {
        assertNotNull(FHIRPractitionerBuilder.newBuilder());
    }

    @Test
    @DisplayName("Build new practitioner 🥳")
    public void build() {
        Practitioner practitioner = FHIRPractitionerBuilder.newBuilder()
                .withNpi("7127445550")
                .withName("Test", "Practitioner")
                .withOrgTag("123456")
                .build();
        assertEquals(1, practitioner.getIdentifier().size());
        assertEquals("7127445550", practitioner.getIdentifierFirstRep().getValue());
        assertEquals("Test Practitioner", practitioner.getNameFirstRep().getNameAsSingleString());
        assertEquals("123456", practitioner.getMeta().getTagFirstRep().getCode());
    }
}