package gov.cms.dpc.fhir.converters.rewrite;

import gov.cms.dpc.common.entities.AddressEntity;
import gov.cms.dpc.fhir.converters.FHIRConverter;
import gov.cms.dpc.fhir.converters.FHIREntityConverter;
import gov.cms.dpc.fhir.exceptions.DataTranslationException;
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
    public Address toFHIR(FHIREntityConverter converter, AddressEntity javaClass) {
        final Address address = new Address();

        address.setCity(javaClass.getCity());
        address.setCountry(javaClass.getCountry());
        address.setDistrict(javaClass.getDistrict());
        address.setPostalCode(javaClass.getPostalCode());
        address.setState(javaClass.getState());
        address.setUse(javaClass.getUse());
        address.setType(javaClass.getType());
        address.setLine(List.of(new StringType(javaClass.getLine1()), new StringType(javaClass.getLine2())));

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
