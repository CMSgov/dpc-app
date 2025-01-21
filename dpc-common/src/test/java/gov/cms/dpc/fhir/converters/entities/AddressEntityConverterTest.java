package gov.cms.dpc.fhir.converters.entities;

import gov.cms.dpc.common.entities.AddressEntity;
import gov.cms.dpc.fhir.converters.FHIREntityConverter;
import gov.cms.dpc.fhir.converters.exceptions.DataTranslationException;
import org.hl7.fhir.dstu3.model.Address;
import org.hl7.fhir.dstu3.model.StringType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AddressEntityConverterTest {
    AddressEntityConverter converter = new AddressEntityConverter();
    FHIREntityConverter fhirEntityConverter = FHIREntityConverter.initialize();
    private Address fhirAddress;
    private AddressEntity entityAddress;
    String line1 = "222 Baker ST";
    String line2 = "APT 3";
    String city = "Bakersfield";
    String district = "West End";
    String state = "CA";
    String postalCode = "22222";
    String country = "US";
    StringType line1ST = new StringType(line1);
    StringType line2ST = new StringType(line2);

    @BeforeEach
    void buildAddress() {
        fhirAddress = new Address()
                .setType(Address.AddressType.PHYSICAL)
                .setUse(Address.AddressUse.HOME)
                .setLine(List.of(line1ST, line2ST))
                .setCity(city)
                .setDistrict(district)
                .setState(state)
                .setPostalCode(postalCode)
                .setCountry(country);
        entityAddress = new AddressEntity();
        entityAddress.setType(Address.AddressType.PHYSICAL);
        entityAddress.setUse(Address.AddressUse.HOME);
        entityAddress.setLine1(line1);
        entityAddress.setLine2(line2);
        entityAddress.setCity(city);
        entityAddress.setDistrict(district);
        entityAddress.setState(state);
        entityAddress.setPostalCode(postalCode);
        entityAddress.setCountry(country);
    }

    @Test
    void fromFHIR() {
        AddressEntity convertedAddress = converter.fromFHIR(fhirEntityConverter, fhirAddress);
        assertEquals(Address.AddressType.PHYSICAL, convertedAddress.getType());
        assertEquals(Address.AddressUse.HOME, convertedAddress.getUse());
        assertEquals(line1, convertedAddress.getLine1());
        assertEquals(line2, convertedAddress.getLine2());
        assertEquals(city, convertedAddress.getCity());
        assertEquals(district, convertedAddress.getDistrict());
        assertEquals(state, convertedAddress.getState());
        assertEquals(postalCode, convertedAddress.getPostalCode());
        assertEquals(country, convertedAddress.getCountry());
    }

    @Test
    void fromFHIROneLine() {
        fhirAddress.setLine(List.of(line1ST));
        AddressEntity convertedAddress = converter.fromFHIR(fhirEntityConverter, fhirAddress);
        assertEquals(line1, convertedAddress.getLine1());
        assertNull(convertedAddress.getLine2());
    }
    @Test
    void fromFHIRThreeLines() {

        StringType line3 = new StringType("Red Door");
        fhirAddress.setLine(List.of(line1ST,line2ST,line3));
        AddressEntity convertedAddress = converter.fromFHIR(fhirEntityConverter, fhirAddress);
        assertEquals(convertedAddress.getLine2(), "APT 3 Red Door");
    }

    @Test
    void fromFhirEmptyLines() {
        fhirAddress.setLine(new java.util.ArrayList<StringType>());
        assertThrows(DataTranslationException.class, () -> converter.fromFHIR(null, fhirAddress));
    }
    @Test
    void toFHIR() {
        Address convertedAddress = converter.toFHIR(fhirEntityConverter, entityAddress);
        assertEquals(Address.AddressType.PHYSICAL, convertedAddress.getType());
        assertEquals(Address.AddressUse.HOME, convertedAddress.getUse());
        assertEquals(line1, convertedAddress.getLine().get(0).toString());
        assertEquals(line2,convertedAddress.getLine().get(1).toString());
        assertEquals(city, convertedAddress.getCity());
        assertEquals(district, convertedAddress.getDistrict());
        assertEquals(state, convertedAddress.getState());
        assertEquals(postalCode, convertedAddress.getPostalCode());
        assertEquals(country, convertedAddress.getCountry());
    }

    @Test
    void getFHIRResource() {
        assertEquals(Address.class, converter.getFHIRResource());
    }

    @Test
    void getJavaClass() {
        assertEquals(AddressEntity.class, converter.getJavaClass());
    }
}
