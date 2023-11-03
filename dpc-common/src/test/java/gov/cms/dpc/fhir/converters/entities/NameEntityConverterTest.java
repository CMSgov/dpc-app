package gov.cms.dpc.fhir.converters.entities;

import gov.cms.dpc.common.entities.NameEntity;
import gov.cms.dpc.fhir.converters.FHIREntityConverter;
import org.hl7.fhir.dstu3.model.HumanName;
import org.hl7.fhir.dstu3.model.StringType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class NameEntityConverterTest {
    NameEntityConverter converter = new NameEntityConverter();
    FHIREntityConverter fhirEntityConverter = FHIREntityConverter.initialize();
    NameEntity nameEntity;
    HumanName name;

    @BeforeEach
    void buildEntities() {
        String family = "Family";
        String given = "Given";
        String prefix = "Prefix";
        String suffix = "Suffix";
        List<StringType> givens = List.of(new StringType(given));
        List<StringType> prefixes = List.of(new StringType(prefix));
        List<StringType> suffixes = List.of(new StringType(suffix));

        name = new HumanName();
        name.setUse(HumanName.NameUse.OFFICIAL);
        name.setFamily(family);
        name.setGiven(givens);
        name.setPrefix(prefixes);
        name.setSuffix(suffixes);
        nameEntity = new NameEntity();
        nameEntity.setUse(HumanName.NameUse.OFFICIAL);
        nameEntity.setFamily(family);
        nameEntity.setGiven(given);
        nameEntity.setPrefix(prefix);
        nameEntity.setSuffix(suffix);
    }

    @Test
    void fromFHIR() {
        NameEntity convertedEntity = converter.fromFHIR(fhirEntityConverter, name);
        assertEquals(nameEntity.getUse(), convertedEntity.getUse());
        assertEquals(nameEntity.getFamily(), convertedEntity.getFamily());
        assertEquals(nameEntity.getGiven(), convertedEntity.getGiven());
        assertEquals(nameEntity.getPrefix(), convertedEntity.getPrefix());
        assertEquals(nameEntity.getSuffix(), convertedEntity.getSuffix());
    }

    @Test
    void toFHIR() {
        HumanName convertedResource = converter.toFHIR(fhirEntityConverter, nameEntity);
        assertEquals(name.getUse(), convertedResource.getUse());
        assertEquals(name.getFamily(), convertedResource.getFamily());
        assertEquals(name.getGiven().get(0).toString(), convertedResource.getGiven().get(0).toString());
        assertEquals(name.getPrefix().get(0).toString(), convertedResource.getPrefix().get(0).toString());
        assertEquals(name.getSuffix().get(0).toString(), convertedResource.getSuffix().get(0).toString());
    }

    @Test
    void getFHIRResource() {
        assertEquals(HumanName.class, converter.getFHIRResource());
    }

    @Test
    void getJavaClass() {
        assertEquals(NameEntity.class, converter.getJavaClass());
    }
}
