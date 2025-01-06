package gov.cms.dpc.fhir.converters.entities;

import gov.cms.dpc.common.entities.AddressEntity;
import gov.cms.dpc.common.entities.ContactEntity;
import gov.cms.dpc.common.entities.ContactPointEntity;
import gov.cms.dpc.common.entities.NameEntity;
import gov.cms.dpc.fhir.converters.FHIRConverter;
import gov.cms.dpc.fhir.converters.FHIREntityConverter;
import org.hl7.fhir.dstu3.model.Address;
import org.hl7.fhir.dstu3.model.ContactPoint;
import org.hl7.fhir.dstu3.model.HumanName;
import org.hl7.fhir.dstu3.model.Organization;

import java.util.List;

public class OrganizationContactEntityConverter implements FHIRConverter<Organization.OrganizationContactComponent, ContactEntity> {
    @Override
    public ContactEntity fromFHIR(FHIREntityConverter converter, Organization.OrganizationContactComponent resource) {
        final ContactEntity entity = new ContactEntity();
        entity.setName(converter.fromFHIR(NameEntity.class, resource.getName()));

        final List<ContactPointEntity> collect = resource
                .getTelecom()
                .stream()
                .map(c -> converter.fromFHIR(ContactPointEntity.class, c))
                .toList();

        // Set the entity reference
        collect.forEach(contact -> contact.setContactEntity(entity));
        entity.setTelecom(collect);
        entity.setAddress(converter.fromFHIR(AddressEntity.class, resource.getAddress()));
        return entity;
    }

    @Override
    public Organization.OrganizationContactComponent toFHIR(FHIREntityConverter converter, ContactEntity entity) {
        final Organization.OrganizationContactComponent contactComponent = new Organization.OrganizationContactComponent();

        contactComponent.setName(converter.toFHIR(HumanName.class, entity.getName()));

        final List<ContactPoint> cps = entity.getTelecom()
                .stream()
                .map(ContactPointEntity::toFHIR)
                .toList();

        contactComponent.setTelecom(cps);
        contactComponent.setAddress(converter.toFHIR(Address.class, entity.getAddress()));

        return contactComponent;
    }

    @Override
    public Class<Organization.OrganizationContactComponent> getFHIRResource() {
        return Organization.OrganizationContactComponent.class;
    }

    @Override
    public Class<ContactEntity> getJavaClass() {
        return ContactEntity.class;
    }
}
