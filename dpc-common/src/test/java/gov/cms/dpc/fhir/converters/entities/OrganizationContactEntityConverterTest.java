package gov.cms.dpc.fhir.converters.entities;

import gov.cms.dpc.common.entities.AddressEntity;
import gov.cms.dpc.common.entities.ContactEntity;
import gov.cms.dpc.common.entities.ContactPointEntity;
import gov.cms.dpc.common.entities.NameEntity;
import gov.cms.dpc.fhir.converters.FHIREntityConverter;
import org.hl7.fhir.dstu3.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class OrganizationContactEntityConverterTest {
    OrganizationContactEntityConverter entityConverter = new OrganizationContactEntityConverter();
    FHIREntityConverter fhirEntityConverter = FHIREntityConverter.initialize();

    ContactEntity organizationContactEntity;
    Organization.OrganizationContactComponent organizationContact;


    String family = "Jones";
    String email = "bob@example.com";
    String line1 = "222 Baker ST";

    @BeforeEach
    void buildEntities() {
        HumanName name = new HumanName();
        name.setFamily(family);
        Address address = new Address().setLine(List.of(new StringType(line1)));
        ContactPoint contactPoint = new ContactPoint();
        contactPoint.setValue(email);

        organizationContact = new Organization.OrganizationContactComponent();
        organizationContact.setName(name);
        organizationContact.setAddress(address);
        organizationContact.setTelecom(List.of(contactPoint));
        
        AddressEntity addressEntity = new AddressEntity();
        addressEntity.setLine1(line1);
        NameEntity nameEntity = new NameEntity();
        nameEntity.setFamily(family);
        ContactPointEntity contactPointEntity = new ContactPointEntity();
        contactPointEntity.setValue(email);
        organizationContactEntity = new ContactEntity();
        organizationContactEntity.setName(nameEntity);
        organizationContactEntity.setTelecom(List.of(contactPointEntity));
        organizationContactEntity.setAddress(addressEntity);
    }

    @Test
    void fromFHIR() {
        ContactEntity convertedEntity = entityConverter.fromFHIR(fhirEntityConverter, organizationContact);
        assertEquals(family, convertedEntity.getName().getFamily());
        assertEquals(line1, convertedEntity.getAddress().getLine1());
        assertEquals(email, convertedEntity.getTelecom().get(0).getValue());
    }

    @Test
    void toFHIR() {
        Organization.OrganizationContactComponent convertedResource = entityConverter.toFHIR(fhirEntityConverter, organizationContactEntity);
        assertEquals(family, convertedResource.getName().getFamily());
        assertEquals(line1, convertedResource.getAddress().getLine().get(0).toString());
        assertEquals(email, convertedResource.getTelecom().get(0).getValue());
    }

    @Test
    void getFHIRResource() {
        assertEquals(Organization.OrganizationContactComponent.class, entityConverter.getFHIRResource());
    }

    @Test
    void getJavaClass() {
        assertEquals(ContactEntity.class, entityConverter.getJavaClass());
    }}
