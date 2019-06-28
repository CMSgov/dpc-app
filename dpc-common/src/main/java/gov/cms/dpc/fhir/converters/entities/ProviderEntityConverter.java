package gov.cms.dpc.fhir.converters.entities;

import gov.cms.dpc.common.entities.ProviderEntity;
import org.hl7.fhir.dstu3.model.Practitioner;

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

        return practitioner;
    }
}
