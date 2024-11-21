package gov.cms.dpc.fhir;

import gov.cms.dpc.testing.BufferedLoggerHandler;
import org.hl7.fhir.dstu3.model.Enumerations;
import org.hl7.fhir.dstu3.model.Meta;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Practitioner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;

@ExtendWith(BufferedLoggerHandler.class)
@DisplayName("FHIR data object construction")
public class FHIRBuilderTests {

    private FHIRBuilderTests() {
        // Not used
    }

    @Test
    @DisplayName("Build patient 🥳")
    void testPatientBuilder() {
        final Patient patient = FHIRBuilders.buildPatientFromMBI("12345");
        assertAll(() -> assertEquals("12345", patient.getIdentifierFirstRep().getValue(), "Should have the correct value"),
                () -> assertEquals(DPCIdentifierSystem.MBI.getSystem(), patient.getIdentifierFirstRep().getSystem(), "Should have correct system"));
    }

    @Test
    @DisplayName("Look back log JSON parsing 🥳")
    void testProviderBuilder() {
        final Practitioner practitioner = FHIRBuilders.buildPractitionerFromNPI("12345");
        assertAll(() -> assertEquals("12345", practitioner.getIdentifierFirstRep().getValue(), "Should have the correct value"),
                () -> assertEquals(DPCIdentifierSystem.NPPES.getSystem(), practitioner.getIdentifierFirstRep().getSystem(), "Should have correct system"));
    }

    @Test
    @DisplayName("Verify no additional metadata 🥳")
    void testTagAdditionNoMeta() {
        final UUID uuid = UUID.randomUUID();
        final Patient patient = new Patient();
        patient.setGender(Enumerations.AdministrativeGender.FEMALE);
        FHIRBuilders.addOrganizationTag(patient, uuid);

        assertAll(() -> assertNotNull(patient.getMeta(), "Should have meta"),
                () -> assertEquals(1, patient.getMeta().getTag().size(), "Should have a single tag"),
                () -> assertEquals(uuid.toString(), patient.getMeta().getTagFirstRep().getCode(), "Should have correct code"),
                () -> assertEquals(DPCIdentifierSystem.DPC.getSystem(), patient.getMeta().getTagFirstRep().getSystem(), "Should have correct system"));
    }

    @Test
    @DisplayName("Distinguish existing from new metadata 🥳")
    void testTagAdditionExistingMeta() {
        final Meta meta = new Meta();
        meta.addTag().setSystem("http://not.real").setCode("nothing");
        final UUID uuid = UUID.randomUUID();
        final Patient patient = new Patient();
        patient.setMeta(meta);
        patient.setGender(Enumerations.AdministrativeGender.FEMALE);
        FHIRBuilders.addOrganizationTag(patient, uuid);

        assertAll(() -> assertNotNull(patient.getMeta(), "Should have meta"),
                () -> assertEquals(2, patient.getMeta().getTag().size(), "Should have a single tag"));
    }
}
