package gov.cms.dpc.fhir.converters;

import gov.cms.dpc.common.entities.ContactPointEntity;
import org.hl7.fhir.dstu3.model.ContactPoint;

public class ContactPointConverter {

    private ContactPointConverter() {
        // Not used
    }

    public static ContactPointEntity convert(ContactPoint datatype) {
        final ContactPointEntity entity = new ContactPointEntity();

        entity.setSystem(datatype.getSystem());
        entity.setUse(datatype.getUse());
        entity.setValue(datatype.getValue());

        // Optional values
        if (datatype.hasRank()) {
            entity.setRank(datatype.getRank());
        }
        return entity;
    }
}
