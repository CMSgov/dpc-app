package gov.cms.dpc.fhir.converters.entities;

import gov.cms.dpc.common.entities.AttributionRelationship;
import gov.cms.dpc.common.entities.PatientEntity;
import gov.cms.dpc.common.entities.ProviderEntity;
import gov.cms.dpc.common.entities.RosterEntity;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import gov.cms.dpc.fhir.converters.FHIREntityConverter;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Group;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Date;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class RosterEntityConverterTest {
    RosterEntityConverter rosterEntityConverter = new RosterEntityConverter();
    FHIREntityConverter fhirEntityConverter = FHIREntityConverter.initialize();

    RosterEntity rosterEntity;

    UUID uuid = UUID.randomUUID();
    UUID patientId = UUID.randomUUID();
    String npi = UUID.randomUUID().toString();
    OffsetDateTime begin;
    OffsetDateTime end;

    @BeforeEach
    void buildEntities() {
        ProviderEntity providerEntity = new ProviderEntity();
        providerEntity.setProviderNPI(npi);
        PatientEntity patientEntity = new PatientEntity();
        patientEntity.setID(patientId);
        RosterEntity roster = new RosterEntity();
        AttributionRelationship attributionRelationship = new AttributionRelationship(roster, patientEntity);
        attributionRelationship.setInactive(false);
        begin = OffsetDateTime.of(2020, 2, 2, 2, 2, 2,2, ZoneOffset.UTC);
        end = OffsetDateTime.now();
        attributionRelationship.setPeriodBegin(begin);
        attributionRelationship.setPeriodEnd(end);
        rosterEntity = new RosterEntity();
        rosterEntity.setId(uuid);
        rosterEntity.setAttributedProvider(providerEntity);
        rosterEntity.setAttributions(List.of(attributionRelationship));
    }

    @Test
    void fromFHIR() {
        String expected = "Entity cannot be converted from FHIR, using this class";
        Exception exception = assertThrows(UnsupportedOperationException.class, () -> {
            rosterEntityConverter.fromFHIR(fhirEntityConverter, null);
        });
        assertEquals(expected, exception.getMessage());
    }

    @Test
    void toFHIR() {
        Group convertedResource = rosterEntityConverter.toFHIR(fhirEntityConverter, rosterEntity);
        assertEquals(Group.GroupType.PERSON, convertedResource.getType());
        assertTrue(convertedResource.getActual());
        assertEquals(uuid.toString(), convertedResource.getId());
        Group.GroupCharacteristicComponent characteristic = convertedResource.getCharacteristic().get(0);
        assertEquals("attributed-to", characteristic.getCode().getCoding().get(0).getCode());
        CodeableConcept codeableConcept = characteristic.getValueCodeableConcept();
        assertEquals(DPCIdentifierSystem.NPPES.getSystem(), codeableConcept.getCoding().get(0).getSystem());
        assertEquals(npi, codeableConcept.getCoding().get(0).getCode());
        Group.GroupMemberComponent member = convertedResource.getMember().get(0);
        assertEquals("Patient/" + patientId.toString(), member.getEntity().getReference());
        assertFalse(member.getInactive());
        assertEquals(Date.from(begin.toInstant()), member.getPeriod().getStart());
        assertEquals(Date.from(end.toInstant()), member.getPeriod().getEnd());
    }

    @Test
    void getFHIRResource() {
        assertEquals(Group.class, rosterEntityConverter.getFHIRResource());
    }

    @Test
    void getJavaClass() {
        assertEquals(RosterEntity.class, rosterEntityConverter.getJavaClass());
    }}
