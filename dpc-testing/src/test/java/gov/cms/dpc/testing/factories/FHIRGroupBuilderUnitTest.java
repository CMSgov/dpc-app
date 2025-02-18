package gov.cms.dpc.testing.factories;

import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Group;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class FHIRGroupBuilderUnitTest {

    @Test
    void testFullBuild() {
        UUID patient1Id = UUID.randomUUID();
        UUID patient2Id = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        String practitionerNpi  = "12345";

        Group group = FHIRGroupBuilder.newBuild()
                .withOrgTag(orgId)
                .withPatients(patient1Id,patient2Id)
                .attributedTo(practitionerNpi)
                .build();

        //Assert correct patients
        assertNotNull(group);
        assertEquals(2, group.getMember().size());
        assertEquals("Patient/"+patient1Id,group.getMember().get(0).getEntity().getReference());
        assertEquals("Patient/"+patient2Id,group.getMember().get(1).getEntity().getReference());

        //Assert correct practitioner
        assertTrue(group.getActive());
        assertEquals(Group.GroupType.PERSON, group.getType());
        assertEquals(1,group.getCharacteristic().size());

        Group.GroupCharacteristicComponent characteristic = group.getCharacteristicFirstRep();
        assertFalse(characteristic.getExclude());

        CodeableConcept npiConcept = (CodeableConcept) characteristic.getValue();
        CodeableConcept attrConcept = characteristic.getCode();
        assertEquals("attributed-to",attrConcept.getCodingFirstRep().getCode());
        assertEquals("12345", npiConcept.getCodingFirstRep().getCode());

        //Assert correct org tag
        assertEquals(1, group.getMeta().getTag().size());
        assertEquals(FHIRGroupBuilder.DPC_SYSTEM, group.getMeta().getTag().get(0).getSystem());
        assertEquals(orgId.toString(), group.getMeta().getTag().get(0).getCode());
    }

    @Test
    void testNewBuild() {
        FHIRGroupBuilder result = FHIRGroupBuilder.newBuild();
        assertNotNull(result);
    }

    @Test
    void testAttributedTo() {
        FHIRGroupBuilder builder = FHIRGroupBuilder.newBuild();
        builder.attributedTo("12345");
        Group group = builder.build();

        assertNotNull(group);
        assertTrue(group.getActive());
        assertEquals(Group.GroupType.PERSON, group.getType());
        assertEquals(1,group.getCharacteristic().size());

        Group.GroupCharacteristicComponent characteristic = group.getCharacteristicFirstRep();
        assertFalse(characteristic.getExclude());

        CodeableConcept npiConcept = (CodeableConcept) characteristic.getValue();
        CodeableConcept attrConcept = characteristic.getCode();
        assertEquals("attributed-to",attrConcept.getCodingFirstRep().getCode());
        assertEquals("12345", npiConcept.getCodingFirstRep().getCode());
    }


    @Test
    void testWithPatient() {
        UUID patientUUID = UUID.randomUUID();

        //With id as string
        Group group = FHIRGroupBuilder.newBuild().withPatients(patientUUID.toString()).build();
        assertEquals(1, group.getMember().size());
        assertEquals("Patient/"+patientUUID,group.getMemberFirstRep().getEntity().getReference());

        //With id as reference string
        group = FHIRGroupBuilder.newBuild().withPatients("Patient/"+patientUUID).build();
        assertEquals(1, group.getMember().size());
        assertEquals("Patient/"+patientUUID,group.getMemberFirstRep().getEntity().getReference());

        //With id as UUID
        group = FHIRGroupBuilder.newBuild().withPatients(patientUUID).build();
        assertEquals(1, group.getMember().size());
        assertEquals("Patient/"+patientUUID,group.getMemberFirstRep().getEntity().getReference());

        //With multiple
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        group = FHIRGroupBuilder.newBuild().withPatients(id1).withPatients(id2).build();
        assertEquals(2, group.getMember().size());
        assertEquals("Patient/"+id1,group.getMember().get(0).getEntity().getReference());
        assertEquals("Patient/"+id2,group.getMember().get(1).getEntity().getReference());
    }
    @Test
    void withOrgTag() {
        UUID orgId = UUID.randomUUID();

        //With id as UUID
        Group group = FHIRGroupBuilder.newBuild().withOrgTag(orgId).build();
        assertEquals(1, group.getMeta().getTag().size());
        assertEquals(FHIRGroupBuilder.DPC_SYSTEM, group.getMeta().getTag().get(0).getSystem());
        assertEquals(orgId.toString(), group.getMeta().getTag().get(0).getCode());
    }
}
