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
    public HumanName toFHIR(FHIREntityConverter converter, NameEntity javaClass) {
        final HumanName name = new HumanName();

        name.setFamily(javaClass.getFamily());
        name.setPrefix(List.of(new StringType(javaClass.getPrefix())));
        name.setSuffix(List.of(new StringType(javaClass.getSuffix())));
        name.setGiven(List.of(new StringType(javaClass.getGiven())));
        name.setUse(javaClass.getUse());

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
