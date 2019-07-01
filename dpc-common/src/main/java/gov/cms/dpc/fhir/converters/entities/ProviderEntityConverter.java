package gov.cms.dpc.fhir.converters.entities;

import gov.cms.dpc.common.entities.ProviderEntity;
import org.hl7.fhir.dstu3.model.Meta;
import org.hl7.fhir.dstu3.model.Practitioner;

import java.sql.Date;

public class ProviderEntityConverter {


    private ProviderEntityConverter() {
        // Not used
    }

    public static Practitioner convert(ProviderEntity entity) {
        final Practitioner practitioner = new Practitioner();

        practitioner.setId(entity.getProviderID().toString());
        practitioner.addName().
                setFamily(entity.getProviderLastName())
                .addGiven(entity.getProviderFirstName());

        practitioner.addIdentifier().setValue(entity.getProviderNPI());
        final Meta meta = new Meta();
        meta.setLastUpdated(Date.valueOf("1990-01-01"));
        practitioner.setMeta(meta);

        return practitioner;
    }
}
