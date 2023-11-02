package gov.cms.dpc.fhir.converters.entities;

import gov.cms.dpc.common.entities.OrganizationEntity;
import gov.cms.dpc.common.entities.PatientEntity;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import gov.cms.dpc.fhir.converters.FHIREntityConverter;
import gov.cms.dpc.fhir.validations.profiles.PatientProfile;
import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.r4.model.codesystems.GenderIdentity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class PatientEntityConverterTest {
    PatientEntityConverter converter = new PatientEntityConverter();
    FHIREntityConverter fhirEntityConverter = FHIREntityConverter.initialize();
    PatientEntity patientEntity;
    Patient patient;

    UUID uuid = UUID.randomUUID();
    UUID orgUuid = UUID.randomUUID();
    String given = "Bob";
    String family = "Jones";
    String beneficiaryId = "1aa2aa3aa44";
    Date birthDay = new GregorianCalendar(1950, Calendar.FEBRUARY, 11).getTime();

    @BeforeEach
    void buildEntities() {
        HumanName name = new HumanName();
        name.setGiven(List.of(new StringType(given)));
        name.setFamily(family);
        patient = new Patient();
        patient.setId(uuid.toString());
        patient.setName(List.of(name));
        patient.setGender(Enumerations.AdministrativeGender.MALE);
        patient.setBirthDate(birthDay);
        patient.setManagingOrganization(new Reference(new IdType("Organization", orgUuid.toString())));
        Identifier identifier = new Identifier().setSystem(DPCIdentifierSystem.MBI.getSystem()).setValue(beneficiaryId);
        patient.setIdentifier(List.of(identifier));

        OrganizationEntity organizationEntity = new OrganizationEntity();
        organizationEntity.setId(orgUuid);
        patientEntity = new PatientEntity();
        patientEntity.setID(uuid);
        patientEntity.setFirstName(given);
        patientEntity.setLastName(family);
        patientEntity.setGender(Enumerations.AdministrativeGender.MALE);
        patientEntity.setDob(PatientEntity.toLocalDate(birthDay));
        patientEntity.setOrganization(organizationEntity);
        patientEntity.setBeneficiaryID(beneficiaryId);
    }

    @Test
    void fromFHIR() {
        PatientEntity convertedEntity = converter.fromFHIR(fhirEntityConverter, patient);
        assertEquals(uuid, convertedEntity.getID());
        assertEquals(given, convertedEntity.getFirstName());
        assertEquals(family, convertedEntity.getLastName());
        assertEquals(GenderIdentity.MALE.toString(), convertedEntity.getGender().toString());
        assertEquals(beneficiaryId, convertedEntity.getBeneficiaryID());
        assertEquals(orgUuid, convertedEntity.getOrganization().getId());
    }

    @Test
    void fromFHIR_NoOrg() {
        patient.setManagingOrganization(null);
        PatientEntity convertedEntity = converter.fromFHIR(fhirEntityConverter, patient);
        assertNull(convertedEntity.getOrganization());
    }

    @Test
    void fromFHIR_NoId() {
        patient.setId("");
        PatientEntity convertedEntity = converter.fromFHIR(fhirEntityConverter, patient);
        assertEquals(uuid.toString().length(), convertedEntity.getID().toString().length());
    }

    @Test
    void toFHIR() {
        Patient convertedResource = converter.toFHIR(fhirEntityConverter, patientEntity);
        assertEquals(PatientProfile.PROFILE_URI, convertedResource.getMeta().getProfile().get(0).getValueAsString());
        assertEquals(uuid.toString(), convertedResource.getId());
        assertEquals(given, convertedResource.getName().get(0).getGiven().get(0).toString());
        assertEquals(family, convertedResource.getName().get(0).getFamily());
        assertEquals(GenderIdentity.MALE.toString(), convertedResource.getGender().toString());
        assertEquals(DPCIdentifierSystem.MBI.getSystem(), convertedResource.getIdentifier().get(0).getSystem());
        assertEquals(beneficiaryId, convertedResource.getIdentifier().get(0).getValue());
        assertEquals("Organization/" + orgUuid, convertedResource.getManagingOrganization().getReference());
        assertNull(convertedResource.getMeta().getLastUpdatedElement().getValue());
    }

    @Test
    void toFHIR_Updated() {
        OffsetDateTime ost = OffsetDateTime.now();
        patientEntity.setUpdatedAt(ost);
        Patient convertedResource = converter.toFHIR(fhirEntityConverter, patientEntity);
        assertEquals(ost.toLocalDate(), PatientEntity.toLocalDate(convertedResource.getMeta().getLastUpdatedElement().getValue()));
    }

    @Test
    void getFHIRResource() {
        assertEquals(Patient.class, converter.getFHIRResource());
    }

    @Test
    void getJavaClass() {
        assertEquals(PatientEntity.class, converter.getJavaClass());
    }
}
