package gov.cms.dpc.fhir.converters.entities;

import gov.cms.dpc.common.entities.AddressEntity;
import gov.cms.dpc.fhir.converters.FHIRConverter;
import gov.cms.dpc.fhir.converters.FHIREntityConverter;
import gov.cms.dpc.fhir.converters.exceptions.DataTranslationException;
import org.hl7.fhir.dstu3.model.Address;
import org.hl7.fhir.dstu3.model.StringType;

import java.util.List;
import java.util.stream.Collectors;

public class AddressEntityConverter implements FHIRConverter<Address, AddressEntity> {
    @Override
    public AddressEntity fromFHIR(FHIREntityConverter converter, Address resource) {
        final AddressEntity entity = new AddressEntity();
        entity.setType(resource.getType());
        entity.setUse(resource.getUse());

        // Get the first line and then concat any additional lines
        final List<StringType> lines = resource.getLine();
        if (lines.isEmpty()) {
            throw new DataTranslationException(Address.class, "Address line", "Must have at least one address line");
        }
        final StringType stringType = lines.get(0);

        entity.setLine1(stringType.getValue());
        if (lines.size() > 1) {
            final String line2 = lines.subList(1, lines.size()).stream().map(StringType::getValue).collect(Collectors.joining(" "));
            entity.setLine2(line2);
        }
        entity.setCity(resource.getCity());
        entity.setCountry(resource.getCountry());
        entity.setDistrict(resource.getDistrict());
        entity.setState(resource.getState());
        entity.setPostalCode(resource.getPostalCode());
        entity.setCountry(resource.getCountry());
        return entity;
    }

    @Override
    public Address toFHIR(FHIREntityConverter converter, AddressEntity entity) {
        final Address address = new Address();

        address.setCity(entity.getCity());
        address.setCountry(entity.getCountry());
        address.setDistrict(entity.getDistrict());
        address.setPostalCode(entity.getPostalCode());
        address.setState(entity.getState());
        address.setUse(entity.getUse());
        address.setType(entity.getType());
        address.setLine(List.of(new StringType(entity.getLine1()), new StringType(entity.getLine2())));

        return address;
    }

    @Override
    public Class<Address> getFHIRResource() {
        return Address.class;
    }

    @Override
    public Class<AddressEntity> getJavaClass() {
        return AddressEntity.class;
    }
}
