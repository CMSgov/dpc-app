package gov.cms.dpc.fhir.converters.entities;

import ca.uhn.fhir.parser.DataFormatException;
import gov.cms.dpc.common.entities.*;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import gov.cms.dpc.fhir.converters.FHIREntityConverter;
import org.hl7.fhir.dstu3.model.Address;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Organization;
import org.hl7.fhir.dstu3.model.StringType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class OrganizationEntityConverterTest {
    FHIREntityConverter fhirEntityConverter = FHIREntityConverter.initialize();
    OrganizationEntityConverter organizationEntityConverter = new OrganizationEntityConverter();
    OrganizationEntity organizationEntity;
    Organization organization;

    UUID uuid = UUID.randomUUID();
    UUID endpointEntityUuid = UUID.randomUUID();
    String orgIdValue = "5";
    String line1 = "222 Baker ST";

    String family = "Jones";

    String orgName = "Health Hut";

    @BeforeEach
    void buildEntities() {
        AddressEntity addressEntity = new AddressEntity();
        addressEntity.setLine1(line1);
        NameEntity nameEntity = new NameEntity();
        nameEntity.setFamily(family);
        ContactEntity contactEntity = new ContactEntity();
        contactEntity.setName(nameEntity);
        contactEntity.setAddress(addressEntity);
        contactEntity.setTelecom(List.of());
        EndpointEntity endpointEntity = new EndpointEntity();
        endpointEntity.setId(endpointEntityUuid);
        OrganizationEntity.OrganizationID orgId = new OrganizationEntity.OrganizationID(DPCIdentifierSystem.NPPES, orgIdValue);
        organizationEntity = new OrganizationEntity();
        organizationEntity.setId(uuid);
        organizationEntity.setOrganizationID(orgId);
        organizationEntity.setOrganizationName(orgName);
        organizationEntity.setOrganizationAddress(addressEntity);
        organizationEntity.setContacts(List.of(contactEntity));
        organizationEntity.setEndpoints(List.of(endpointEntity));

        Address address = new Address().setLine(List.of(new StringType(line1)));
        organization = new Organization();
        organization.setId(uuid.toString());
        organization.setName(orgName);
        organization.setIdentifier(List.of(orgId.toFHIR()));
        organization.setAddress(List.of(address));
        organization.setContact(List.of(contactEntity.toFHIR()));
    }

    @Test
    void fromFHIR() {
        OrganizationEntity convertedEntity = organizationEntityConverter.fromFHIR(fhirEntityConverter, organization);
        assertEquals(uuid, convertedEntity.getId());
        assertEquals(DPCIdentifierSystem.NPPES, convertedEntity.getOrganizationID().getSystem());
        assertEquals(orgIdValue, convertedEntity.getOrganizationID().getValue());
        assertEquals(orgName, convertedEntity.getOrganizationName());
        assertEquals(line1, convertedEntity.getOrganizationAddress().getLine1());
        assertEquals(family, convertedEntity.getContacts().get(0).getName().getFamily());
    }

    @Test
    void fromFHIR_noID() {
        organization.setId("");
        OrganizationEntity convertedEntity = organizationEntityConverter.fromFHIR(fhirEntityConverter, organization);
        assertNotEquals(uuid.toString(), convertedEntity.getId().toString());
        assertEquals(uuid.toString().length(), convertedEntity.getId().toString().length());
    }

    @Test
    void fromFHIR_DifferentIdentifierSystem() {
        OrganizationEntity.OrganizationID orgId = new OrganizationEntity.OrganizationID(DPCIdentifierSystem.MBI, orgIdValue);

        organization.setIdentifier(List.of(orgId.toFHIR()));
        Exception exception = assertThrows(DataFormatException.class, () -> {
            organizationEntityConverter.fromFHIR(fhirEntityConverter, organization);
        });
        assertEquals("Identifier must be NPPES or PECOS", exception.getMessage());
    }

    @Test
    void fromFHIR_BadIdentifierSystem() {
        Identifier identifier = new Identifier().setSystem("bad system");
        organization.setIdentifier(List.of(identifier));
        Exception exception = assertThrows(DataFormatException.class, () -> {
            organizationEntityConverter.fromFHIR(fhirEntityConverter, organization);
        });
        assertEquals("Identifier must be NPPES or PECOS", exception.getMessage());
    }


    @Test
    void toFHIR() {
        Organization convertedResource = organizationEntityConverter.toFHIR(fhirEntityConverter, organizationEntity);
        assertEquals(uuid.toString(), convertedResource.getId());
        assertEquals(DPCIdentifierSystem.NPPES.getSystem(), convertedResource.getIdentifier().get(0).getSystem());
        assertEquals(orgIdValue, convertedResource.getIdentifier().get(0).getValue());
        assertEquals(orgName, convertedResource.getName());
        assertEquals(line1, convertedResource.getAddress().get(0).getLine().get(0).toString());
        assertEquals(family, convertedResource.getContact().get(0).getName().getFamily());
        assertEquals("Endpoint/" + endpointEntityUuid.toString(), convertedResource.getEndpoint().get(0).getReference());
    }

    @Test
    void getFHIRResource() {
        assertEquals(Organization.class, organizationEntityConverter.getFHIRResource());
    }

    @Test
    void getJavaClass() {
        assertEquals(OrganizationEntity.class, organizationEntityConverter.getJavaClass());
    }}
