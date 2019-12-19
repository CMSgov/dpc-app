package gov.cms.dpc.fhir.converters.rewrite;

import gov.cms.dpc.common.entities.ContactEntity;
import gov.cms.dpc.common.entities.ContactPointEntity;
import gov.cms.dpc.common.entities.NameEntity;
import gov.cms.dpc.fhir.converters.*;
import gov.cms.dpc.fhir.converters.ContactPointConverter;
import org.hl7.fhir.dstu3.model.Address;
import org.hl7.fhir.dstu3.model.ContactPoint;
import org.hl7.fhir.dstu3.model.HumanName;
import org.hl7.fhir.dstu3.model.Organization;

import java.util.List;
import java.util.stream.Collectors;

public class OrganizationContactEntityConverter implements FHIRConverter<Organization.OrganizationContactComponent, ContactEntity> {
    @Override
    public ContactEntity fromFHIR(FHIREntityConverter converter, Organization.OrganizationContactComponent resource) {
        final ContactEntity entity = new ContactEntity();
        entity.setName(converter.fromFHIR(NameEntity.class, resource.getName()));

        final List<ContactPointEntity> collect = resource
                .getTelecom()
                .stream()
                .map(ContactPointConverter::convert)
                .collect(Collectors.toList());

        // Set the entity reference
        collect.forEach(contact -> contact.setContactEntity(entity));
        entity.setTelecom(collect);
        entity.setAddress(AddressConverter.convert(resource.getAddress()));
        return entity;
    }

    @Override
    public Organization.OrganizationContactComponent toFHIR(FHIREntityConverter converter, ContactEntity javaClass) {
        final Organization.OrganizationContactComponent contactComponent = new Organization.OrganizationContactComponent();

        contactComponent.setName(converter.toFHIR(HumanName.class, javaClass.getName()));

        final List<ContactPoint> cps = javaClass.getTelecom()
                .stream()
                .map(ContactPointEntity::toFHIR)
                .collect(Collectors.toList());

        contactComponent.setTelecom(cps);
        contactComponent.setAddress(converter.toFHIR(Address.class, javaClass.getAddress()));

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
