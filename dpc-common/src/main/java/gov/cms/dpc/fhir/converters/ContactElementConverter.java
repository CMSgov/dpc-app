package gov.cms.dpc.fhir.converters;

import gov.cms.dpc.common.entities.ContactEntity;
import gov.cms.dpc.common.entities.ContactPointEntity;
import org.hl7.fhir.dstu3.model.Organization;

import java.util.List;
import java.util.stream.Collectors;

public class ContactElementConverter {

    private ContactElementConverter() {
        // Not used
    }

    public static ContactEntity convert(Organization.OrganizationContactComponent element) {
        final ContactEntity entity = new ContactEntity();
//        entity.setPurpose(element.getPurpose());
        entity.setName(HumanNameConverter.convert(element.getName()));

        final List<ContactPointEntity> collect = element
                .getTelecom()
                .stream()
                .map(ContactPointConverter::convert)
                .collect(Collectors.toList());
        entity.setTelecom(collect);
        entity.setAddress(AddressConverter.convert(element.getAddress()));
        return entity;
    }
}
