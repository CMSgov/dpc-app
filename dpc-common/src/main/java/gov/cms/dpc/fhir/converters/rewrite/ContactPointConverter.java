package gov.cms.dpc.fhir.converters.rewrite;

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
    public ContactPoint toFHIR(FHIREntityConverter converter, ContactPointEntity javaClass) {
        final ContactPoint cp = new ContactPoint();
        cp.setSystem(javaClass.getSystem());
        cp.setUse(javaClass.getUse());
        cp.setValue(javaClass.getValue());

        if (javaClass.getRank() != null) {
            cp.setRank(javaClass.getRank());
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
