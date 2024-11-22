package gov.cms.dpc.fhir.converters.entities;

import gov.cms.dpc.common.entities.PatientEntity;
import gov.cms.dpc.common.entities.ProviderEntity;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import gov.cms.dpc.fhir.converters.FHIREntityConverter;
import gov.cms.dpc.fhir.validations.profiles.PractitionerProfile;
import org.hl7.fhir.dstu3.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.DisplayName;

@DisplayName("Provider entity conversion")
public class ProviderEntityConverterTest {
    ProviderEntityConverter converter = new ProviderEntityConverter();
    FHIREntityConverter fhirEntityConverter = FHIREntityConverter.initialize();
    ProviderEntity providerEntity;
    Practitioner practitioner;

    UUID uuid = UUID.randomUUID();
    UUID orgUuid = UUID.randomUUID();
    String npi = UUID.randomUUID().toString();
    String given = "Bob";
    String family = "Jones";

    @BeforeEach
    void buildEntities() {
        HumanName name = new HumanName();
        name.setGiven(List.of(new StringType(given)));
        name.setFamily(family);
        Meta meta = new Meta();
        meta.addProfile(PractitionerProfile.PROFILE_URI);
        meta.setTag(List.of(new Coding().setCode(orgUuid.toString()).setSystem(DPCIdentifierSystem.DPC.getSystem())));
        practitioner = new Practitioner();
        practitioner.setMeta(meta);
        practitioner.setId(uuid.toString());
        practitioner.setName(List.of(name));
        Identifier identifier = new Identifier().setSystem(DPCIdentifierSystem.NPPES.getSystem()).setValue(npi);
        practitioner.setIdentifier(List.of(identifier));

        providerEntity = new ProviderEntity();
        providerEntity.setID(uuid);
        providerEntity.setProviderNPI(npi);
        providerEntity.setFirstName(given);
        providerEntity.setLastName(family);
    }

    @Test
    @DisplayName("Convert practitioner entity with attributes from FHIR 🥳")
    void fromFHIR() {
        ProviderEntity convertedEntity = converter.fromFHIR(fhirEntityConverter, practitioner);
        assertEquals(uuid, convertedEntity.getID());
        assertEquals(orgUuid, convertedEntity.getOrganization().getId());
        assertEquals(npi, convertedEntity.getProviderNPI());
        assertEquals(given, convertedEntity.getFirstName());
        assertEquals(family, convertedEntity.getLastName());
    }

    @Test
    @DisplayName("Convert practitioner entity with no ID from FHIR 🥳")
    void fromFHIR_NoId() {
        practitioner.setId("");
        ProviderEntity convertedEntity = converter.fromFHIR(fhirEntityConverter, practitioner);
        assertEquals(uuid.toString().length(), convertedEntity.getID().toString().length());
    }
    @Test
    @DisplayName("Convert practitioner entity with attributes to FHIR 🥳")
    void toFHIR() {
        Practitioner convertedResource = converter.toFHIR(fhirEntityConverter, providerEntity);
        assertEquals(uuid.toString(), convertedResource.getId());
        assertEquals(npi, convertedResource.getIdentifier().get(0).getValue());
        assertEquals(DPCIdentifierSystem.NPPES.getSystem(), convertedResource.getIdentifier().get(0).getSystem());
        assertEquals(given, convertedResource.getName().get(0).getGiven().get(0).toString());
        assertEquals(family, convertedResource.getName().get(0).getFamily());
    }

    @Test
    @DisplayName("Convert practitioner update date attribute to FHIR 🥳")
    void toFHIR_Updated() {
        OffsetDateTime ost = OffsetDateTime.now();
        providerEntity.setUpdatedAt(ost);
        Practitioner convertedResource = converter.toFHIR(fhirEntityConverter, providerEntity);
        assertEquals(ost.toLocalDate(), PatientEntity.toLocalDate(convertedResource.getMeta().getLastUpdatedElement().getValue()));
    }

    @Test
    @DisplayName("Convert Practitioner class to FHIR resource 🥳")
    void getFHIRResource() {
        assertEquals(Practitioner.class, converter.getFHIRResource());
    }

    @Test
    @DisplayName("Convert Provider Entity to Java class 🥳")
    void getJavaClass() {
        assertEquals(ProviderEntity.class, converter.getJavaClass());
    }
}
