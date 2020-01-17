package gov.cms.dpc.fhir.converters.entities;

import gov.cms.dpc.common.entities.NameEntity;
import gov.cms.dpc.fhir.converters.FHIRConverter;
import gov.cms.dpc.fhir.converters.FHIREntityConverter;
import org.hl7.fhir.dstu3.model.HumanName;
import org.hl7.fhir.dstu3.model.StringType;

import java.util.List;

public class NameEntityConverter implements FHIRConverter<HumanName, NameEntity> {
    @Override
    public NameEntity fromFHIR(FHIREntityConverter converter, HumanName resource) {
        final NameEntity entity = new NameEntity();
        entity.setUse(resource.getUse());
        entity.setFamily(resource.getFamily());
        entity.setGiven(resource.getGivenAsSingleString());
        entity.setPrefix(resource.getPrefixAsSingleString());
        entity.setSuffix(resource.getSuffixAsSingleString());
        return entity;
    }

    @Override
    public HumanName toFHIR(FHIREntityConverter converter, NameEntity entity) {
        final HumanName name = new HumanName();

        name.setFamily(entity.getFamily());
        name.setPrefix(List.of(new StringType(entity.getPrefix())));
        name.setSuffix(List.of(new StringType(entity.getSuffix())));
        name.setGiven(List.of(new StringType(entity.getGiven())));
        name.setUse(entity.getUse());

        return name;
    }

    @Override
    public Class<HumanName> getFHIRResource() {
        return HumanName.class;
    }

    @Override
    public Class<NameEntity> getJavaClass() {
        return NameEntity.class;
    }
}
