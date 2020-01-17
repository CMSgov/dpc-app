package gov.cms.dpc.fhir.converters.entities;

import gov.cms.dpc.common.entities.ContactPointEntity;
import gov.cms.dpc.fhir.converters.FHIRConverter;
import gov.cms.dpc.fhir.converters.FHIREntityConverter;
import org.hl7.fhir.dstu3.model.ContactPoint;

public class ContactPointConverter implements FHIRConverter<ContactPoint, ContactPointEntity> {

    public ContactPointConverter() {
        // Not used
    }


    @Override
    public ContactPointEntity fromFHIR(FHIREntityConverter converter, ContactPoint resource) {
        final ContactPointEntity entity = new ContactPointEntity();

        entity.setSystem(resource.getSystem());
        entity.setUse(resource.getUse());
        entity.setValue(resource.getValue());

        // Optional values
        if (resource.hasRank()) {
            entity.setRank(resource.getRank());
        }
        return entity;
    }

    @Override
    public ContactPoint toFHIR(FHIREntityConverter converter, ContactPointEntity entity) {
        final ContactPoint cp = new ContactPoint();
        cp.setSystem(entity.getSystem());
        cp.setUse(entity.getUse());
        cp.setValue(entity.getValue());

        if (entity.getRank() != null) {
            cp.setRank(entity.getRank());
        }
        return cp;
    }

    @Override
    public Class<ContactPoint> getFHIRResource() {
        return ContactPoint.class;
    }

    @Override
    public Class<ContactPointEntity> getJavaClass() {
        return ContactPointEntity.class;
    }
}
