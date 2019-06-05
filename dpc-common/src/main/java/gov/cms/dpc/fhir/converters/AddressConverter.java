package gov.cms.dpc.fhir.converters;

import gov.cms.dpc.common.entities.AddressEntity;
import org.hl7.fhir.dstu3.model.Address;
import org.hl7.fhir.dstu3.model.StringType;

import java.util.List;
import java.util.stream.Collectors;

public class AddressConverter {

    private AddressConverter() {
        // Not used
    }

    public static AddressEntity convert(Address datatype) {
        final AddressEntity entity = new AddressEntity();
        entity.setType(datatype.getType());
        entity.setUse(datatype.getUse());

        // Get the first line and then concat any additional lines
        final List<StringType> lines = datatype.getLine();
        final StringType stringType = lines.get(0);

        entity.setLine1(stringType.getValue());
        if (lines.size() > 1) {
            final String line2 = lines.subList(1, lines.size()).stream().map(StringType::getValue).collect(Collectors.joining(" "));
            entity.setLine2(line2);
        }
        entity.setCity(datatype.getCity());
        entity.setCountry(datatype.getCountry());
        entity.setDistrict(datatype.getDistrict());
        entity.setState(datatype.getState());
        entity.setPostalCode(datatype.getPostalCode());
        entity.setCountry(datatype.getCountry());
        return entity;
    }
}
