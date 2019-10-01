package gov.cms.dpc.fhir.converters.entities;

import gov.cms.dpc.common.entities.ProviderEntity;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import gov.cms.dpc.fhir.validations.profiles.PractitionerProfile;
import org.hl7.fhir.dstu3.model.InstantType;
import org.hl7.fhir.dstu3.model.Meta;
import org.hl7.fhir.dstu3.model.Practitioner;

import static gov.cms.dpc.fhir.FHIRFormatters.INSTANT_FORMATTER;

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

        practitioner.addIdentifier()
                .setSystem(DPCIdentifierSystem.NPPES.getSystem())
                .setValue(entity.getProviderNPI());

        final Meta meta = new Meta();

        if (entity.getUpdatedAt() != null) {
            meta.setLastUpdatedElement(new InstantType(entity.getUpdatedAt().format(INSTANT_FORMATTER)));
        }
        meta.addProfile(PractitionerProfile.PROFILE_URI);
        practitioner.setMeta(meta);

        return practitioner;
    }
}
