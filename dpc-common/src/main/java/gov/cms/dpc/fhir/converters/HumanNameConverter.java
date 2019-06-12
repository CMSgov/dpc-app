package gov.cms.dpc.fhir.converters;

import gov.cms.dpc.common.entities.NameEntity;
import org.hl7.fhir.dstu3.model.HumanName;

public class HumanNameConverter {

    private HumanNameConverter() {
        // Not used
    }

    public static NameEntity convert(HumanName datatype) {
        final NameEntity entity = new NameEntity();
        entity.setUse(datatype.getUse());
        entity.setFamily(datatype.getFamily());
        entity.setGiven(datatype.getGivenAsSingleString());
        entity.setPrefix(datatype.getPrefixAsSingleString());
        entity.setSuffix(datatype.getSuffixAsSingleString());
        return entity;
    }
}
