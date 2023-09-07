package gov.cms.dpc.fhir;

import gov.cms.dpc.testing.BufferedLoggerHandler;
import org.apache.commons.lang3.tuple.Pair;
import org.hl7.fhir.dstu3.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.UUID;

import static gov.cms.dpc.fhir.FHIRExtractors.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(BufferedLoggerHandler.class)
public class FHIRExtractorTests {

    private static final String MISSING_ID_FMT = "Cannot find identifier for system: %s";
    private static final String MISSING_MBI_FMT = "Patient: %s doesn't have an MBI";
    private static final String PERFORMER = "Cannot find Provenance performer";

    @Test
    void testGetMBI_MultipleIDs() {
        final Patient patient = new Patient();
        // This double nesting verifies that the fromString method works correctly. Makes PiTest happy.
        patient.addIdentifier().setSystem(DPCIdentifierSystem.fromString(DPCIdentifierSystem.DPC.getSystem()).getSystem()).setValue("test-dpc-one");
        patient.addIdentifier().setSystem(DPCIdentifierSystem.MBI.getSystem()).setValue("0A00A00AA01");

        assertEquals("0A00A00AA01", getPatientMBI(patient), "Should have MBI");
    }

    @Test
    void testGetMBI_NoID() {
        final Patient patient = new Patient();
        patient.setId("id");

        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> getPatientMBI(patient), "Should not have patient MBI");
        assertEquals(String.format(MISSING_MBI_FMT, "id"), exception.getMessage(), "Should have correct exception message");
    }

    @Test
    void testGetMBI_MultipleMBIs() {
        final Patient patient = new Patient();
        patient
            .addIdentifier()
            .setSystem(DPCIdentifierSystem.MBI.getSystem())
            .setValue("0A00A00AA01")
            .addExtension()
                .setUrl(DPCExtensionSystem.IDENTIFIER_CURRENCY.getSystem())
                .setValue(new Coding().setCode(DPCExtensionSystem.CURRENT));

        patient
            .addIdentifier()
            .setSystem(DPCIdentifierSystem.MBI.getSystem())
            .setValue("0A00A00AA01")
            .addExtension()
                .setUrl(DPCExtensionSystem.IDENTIFIER_CURRENCY.getSystem())
                .setValue(new Coding().setCode(DPCExtensionSystem.HISTORIC));

        assertEquals("0A00A00AA01", getPatientMBI(patient), "Should have MBI");
    }

    @Test
    void testGetMBI_MultipleMBIs_NoneCurrent() {
        final Patient patient = new Patient();
        patient.setId("id");
        patient
                .addIdentifier()
                .setSystem(DPCIdentifierSystem.MBI.getSystem())
                .setValue("0A00A00AA01")
                .addExtension()
                .setUrl(DPCExtensionSystem.IDENTIFIER_CURRENCY.getSystem())
                .setValue(new Coding().setCode(DPCExtensionSystem.HISTORIC));

        patient
                .addIdentifier()
                .setSystem(DPCIdentifierSystem.MBI.getSystem())
                .setValue("0A00A00AA01")
                .addExtension()
                .setUrl(DPCExtensionSystem.IDENTIFIER_CURRENCY.getSystem())
                .setValue(new Coding().setCode(DPCExtensionSystem.HISTORIC));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> getPatientMBI(patient));
        assertEquals("Cannot find current MBI for patient: id", exception.getMessage());
    }

    @Test
    void testGetMBI_MultipleMBIs_NoneWithCurrency() {
        final Patient patient = new Patient();
        patient.setId("id");
        patient
                .addIdentifier()
                .setSystem(DPCIdentifierSystem.MBI.getSystem())
                .setValue("0A00A00AA01");

        patient
                .addIdentifier()
                .setSystem(DPCIdentifierSystem.MBI.getSystem())
                .setValue("0A00A00AA01");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> getPatientMBI(patient));
        assertEquals("Cannot find an MBI with identifier currency for patient: id", exception.getMessage());
    }

    @Test
    void testGetMBI_BadFormat() {
        final Patient patient = new Patient();
        patient.addIdentifier().setSystem(DPCIdentifierSystem.MBI.getSystem()).setValue("bad_mbi");

        Exception exception = assertThrows(IllegalArgumentException.class, () -> getPatientMBI(patient));
    }

    @Test
    void testGetMBIs_OneFound() {
        final Patient patient = new Patient();
        Identifier validMBI = new Identifier().setSystem(DPCIdentifierSystem.MBI.getSystem()).setValue("0A00A00AA01");
        Identifier invalidMBI = new Identifier().setSystem(DPCIdentifierSystem.MBI.getSystem()).setValue("mbi2");
        Identifier bene_id = new Identifier().setSystem(DPCIdentifierSystem.MBI.getSystem()).setValue("bene_id");
        patient.addIdentifier(validMBI);
        patient.addIdentifier(invalidMBI);
        patient.addIdentifier(bene_id);

        assertEquals(List.of(validMBI.getValue()), FHIRExtractors.getPatientMBIs(patient));
    }

    @Test
    void testGetMBIs_MultipleFound_SomeWithBadFormat() {
        final Patient patient = new Patient();
        Identifier validMBI1 = new Identifier().setSystem(DPCIdentifierSystem.MBI.getSystem()).setValue("0A00A00AA01");
        Identifier validMBI2 = new Identifier().setSystem(DPCIdentifierSystem.MBI.getSystem()).setValue("0A00A00AA02");
        Identifier bene_id = new Identifier().setSystem(DPCIdentifierSystem.MBI.getSystem()).setValue("bene_id");
        patient.addIdentifier(validMBI1);
        patient.addIdentifier(validMBI2);
        patient.addIdentifier(bene_id);

        assertEquals(List.of(validMBI1.getValue(), validMBI2.getValue()), FHIRExtractors.getPatientMBIs(patient));
    }

    @Test
    void testGetMBIs_NoneFound() {
        final Patient patient = new Patient();
        Identifier invalidMBI = new Identifier().setSystem(DPCIdentifierSystem.MBI.getSystem()).setValue("mbi");
        patient.addIdentifier(invalidMBI);

        assertEquals(List.of(), FHIRExtractors.getPatientMBIs(patient));
    }

    @Test
    void testPractitionerMultipleIDs() {
        final Practitioner practitioner = new Practitioner();
        practitioner.addIdentifier().setSystem(DPCIdentifierSystem.DPC.getSystem()).setValue("test-dpc-one");
        practitioner.addIdentifier().setSystem(DPCIdentifierSystem.fromString(DPCIdentifierSystem.NPPES.getSystem()).getSystem()).setValue("test-npi-one");

        assertEquals("test-npi-one", getProviderNPI(practitioner), "Should have NPI");
    }

    @Test
    void testPractitionerNoID() {
        final Practitioner practitioner = new Practitioner();
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> getProviderNPI(practitioner), "Should not have NPI");
        assertEquals(String.format(MISSING_ID_FMT, DPCIdentifierSystem.NPPES.getSystem()), exception.getMessage(), "Should have correct error message");
    }

    @Test
    void testEntityIDExtraction() {
        final UUID uuid1 = UUID.randomUUID();
        final IdType id1 = new IdType("Organization", uuid1.toString());
        assertEquals(uuid1, getEntityUUID(id1.toString()), "Should have Org ID");
        assertEquals(uuid1, getEntityUUID(uuid1.toString()), "Should parse UUID correctly");
        assertEquals(uuid1, getEntityUUID(String.format("/%s", uuid1.toString())), "Should ignore leading slash");


        assertEquals(uuid1, getEntityUUID(uuid1.toString()), "Should have org id");
        final IdType idType = new IdType("Organization/not-a-uuid");
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> getEntityUUID(idType.toString()), "Should not parse non-UUID");
        assertEquals(String.format(ENTITY_ID_ERROR, idType.toString()), exception.getMessage(), "Should have correct error message");

        final IllegalArgumentException exception1 = assertThrows(IllegalArgumentException.class, () -> getEntityUUID("not-a-uuid"), "Should throw with non-uuid");
        assertEquals("Invalid UUID string: not-a-uuid", exception1.getMessage(), "Should have correct exception message");
    }

    @Test
    void testProvenanceMissingPerformer() {
        final Provenance noAgent = new Provenance();
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> getProvenancePerformer(noAgent), "Should fail with missing agent");
        assertEquals(PERFORMER, exception.getMessage(), "Should have correct message");

        final Provenance wrongAgent = new Provenance();
        wrongAgent.addAgent().addRole().addCoding().setCode("SECRETAGENT");

        final IllegalArgumentException agentException = assertThrows(IllegalArgumentException.class, () -> getProvenancePerformer(wrongAgent), "Should fail with incorrect agent");
        assertEquals(PERFORMER, agentException.getMessage(), "Should have correct exception message");
    }

    @Test
    void testProvenanceExtraction() {
        final Provenance provenance = new Provenance();
        provenance.addAgent().addRole().addCoding().setCode("AGNT");
        final Provenance.ProvenanceAgentComponent performer = getProvenancePerformer(provenance);
        assertEquals("AGNT", performer.getRoleFirstRep().getCodingFirstRep().getCode(), "Should have agent");
    }

    @Test
    void testTagParsing() {
        final Pair<String, String> codeTag = parseTag("a tag");
        assertAll(() -> assertEquals("", codeTag.getLeft()),
                () -> assertEquals("a tag", codeTag.getRight()));

        final Pair<String, String> systemCodeTag = parseTag("This|is a tag");
        assertAll(() -> assertEquals("This", systemCodeTag.getLeft()),
                () -> assertEquals("is a tag", systemCodeTag.getRight()));

        final Pair<String, String> danglingTag = parseTag("Dangling tag|");
        assertAll(() -> assertEquals("Dangling tag", danglingTag.getLeft()),
                () -> assertEquals("", danglingTag.getRight()));

        final Pair<String, String> noSystem = parseTag("|Dangling tag");
        assertAll(() -> assertEquals("", noSystem.getLeft()),
                () -> assertEquals("Dangling tag", noSystem.getRight()));
    }

    @Test
    void testIDExtraction() {
        final Identifier identifier = parseIDFromQueryParam(String.format("%s|%s", DPCIdentifierSystem.DPC.getSystem(), "hello"));
        assertAll(() -> assertEquals(DPCIdentifierSystem.DPC.getSystem(), identifier.getSystem(), "Should have correct system"),
                () -> assertEquals("hello", identifier.getValue(), "Should have correct value"));

        final Identifier emptyValue = parseIDFromQueryParam(String.format("%s|%s", DPCIdentifierSystem.HICN.getSystem(), ""));
        assertAll(() -> assertEquals(DPCIdentifierSystem.HICN.getSystem(), emptyValue.getSystem(), "Should have correct system"),
                () -> assertNull(emptyValue.getValue(), "Should have empty value"));

        final Identifier trailingSystem = parseIDFromQueryParam(String.format("%s\\|%s", DPCIdentifierSystem.HICN.getSystem(), "nada9"));
        assertAll(() -> assertEquals(DPCIdentifierSystem.HICN.getSystem(), trailingSystem.getSystem(), "Should have correct system"),
                () -> assertEquals("nada9", trailingSystem.getValue(), "Should have empty value"));
    }

    @Test
    void testResourceNoMeta() {
        final Group group = new Group();
        assertThrows(IllegalArgumentException.class, () -> getOrganizationID(group), "Should fail with missing meta");
    }

    @Test
    void testResourceWithMeta() {
        final Group group = new Group();
        final Meta meta = new Meta();
        group.setMeta(meta);
        assertThrows(IllegalArgumentException.class, () -> getOrganizationID(group), "Should fail with empty meta");

        meta.addTag().setSystem(DPCIdentifierSystem.MBI_HASH.getSystem()).setCode("not-an-org");
        assertThrows(IllegalArgumentException.class, () -> getOrganizationID(group), "Should fail with incorrect identifier");

        meta.addTag().setSystem(DPCIdentifierSystem.DPC.getSystem()).setCode("test-org");
        assertEquals("test-org", getOrganizationID(group), "Should have correct org ID");
    }

    @Test
    void testGroupAttributedExtraction() {
        final Group group = new Group();
        final IllegalArgumentException e1 = assertThrows(IllegalArgumentException.class, () -> getAttributedNPI(group), "Should throw with no concepts");
        assertEquals("Must have 'attributed-to' concept", e1.getMessage(), "Should have correct error message");

        final CodeableConcept linkedTo = new CodeableConcept();
        linkedTo.addCoding().setCode("linked-to").setSystem("http://local.test");

        group.addCharacteristic().setCode(linkedTo);
        final IllegalArgumentException e2 = assertThrows(IllegalArgumentException.class, () -> getAttributedNPI(group), "Should throw with incorrect characteristic");
        assertEquals("Must have 'attributed-to' concept", e2.getMessage(), "Should have correct error message");

        final CodeableConcept attributedTo = new CodeableConcept();
        attributedTo.addCoding().setCode("attributed-to").setSystem("http://local.test");
        final Group.GroupCharacteristicComponent gc = new Group.GroupCharacteristicComponent();
        gc.setCode(attributedTo);
        group.addCharacteristic(gc);
        final IllegalArgumentException e3 = assertThrows(IllegalArgumentException.class, () -> getAttributedNPI(group), "Should throw with missing value");
        assertEquals("Roster MUST have attributed Provider", e3.getMessage(), "Should have correct error message");

        final CodeableConcept c = new CodeableConcept();
        c.addCoding().setSystem(DPCIdentifierSystem.PECOS.getSystem()).setCode("1");
        gc.setValue(c);
        final IllegalArgumentException e4 = assertThrows(IllegalArgumentException.class, () -> getAttributedNPI(group), "Should throw with missing value");
        assertEquals("Roster MUST have attributed Provider", e4.getMessage(), "Should have correct error message");

        final CodeableConcept cc = new CodeableConcept();
        cc.addCoding().setSystem(DPCIdentifierSystem.NPPES.getSystem()).setCode("123345");
        gc.setValue(cc);
        assertEquals("123345", getAttributedNPI(group), "Should have correct NPI");
    }

    @Test
    void testFindMatchingIdentifiersMultipleFound() {
        Identifier idMbi1 = new Identifier().setSystem(DPCIdentifierSystem.MBI.getSystem()).setValue("mbi1");
        Identifier idMbi2 = new Identifier().setSystem(DPCIdentifierSystem.MBI.getSystem()).setValue("mbi1");
        Identifier idBeneId = new Identifier().setSystem(DPCIdentifierSystem.BENE_ID.getSystem()).setValue("bene_id");

        assertEquals(List.of(idMbi1, idMbi2), FHIRExtractors.findMatchingIdentifiers(List.of(idMbi1, idMbi2, idBeneId), DPCIdentifierSystem.MBI));
    }

    @Test
    void testFindMatchingIdentifiersNoneFound() {
        Identifier idMbi1 = new Identifier().setSystem(DPCIdentifierSystem.MBI.getSystem()).setValue("mbi1");
        Identifier idMbi2 = new Identifier().setSystem(DPCIdentifierSystem.MBI.getSystem()).setValue("mbi1");
        Identifier idBeneId = new Identifier().setSystem(DPCIdentifierSystem.BENE_ID.getSystem()).setValue("bene_id");

        assertTrue(FHIRExtractors.findMatchingIdentifiers(List.of(idMbi1, idMbi2, idBeneId), DPCIdentifierSystem.HICN).isEmpty());
    }
}
