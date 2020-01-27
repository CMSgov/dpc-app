package gov.cms.dpc.fhir.converters.entities;

import gov.cms.dpc.common.entities.OrganizationEntity;
import gov.cms.dpc.common.entities.ProviderEntity;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import gov.cms.dpc.fhir.FHIRExtractors;
import gov.cms.dpc.fhir.converters.FHIRConverter;
import gov.cms.dpc.fhir.converters.FHIREntityConverter;
import gov.cms.dpc.fhir.validations.profiles.PractitionerProfile;
import org.hl7.fhir.dstu3.model.*;

import java.util.UUID;

import static gov.cms.dpc.fhir.FHIRFormatters.INSTANT_FORMATTER;

public class ProviderEntityConverter implements FHIRConverter<Practitioner, ProviderEntity> {

    public ProviderEntityConverter() {
        // Not used
    }

    @Override
    public ProviderEntity fromFHIR(FHIREntityConverter converter, Practitioner resource) {
        final ProviderEntity provider = new ProviderEntity();

        // Get the Organization, from the tag field
        final String organizationID = FHIRExtractors.getOrganizationID(resource);

        final OrganizationEntity organizationEntity = new OrganizationEntity();
        organizationEntity.setId(UUID.fromString(new IdType(organizationID).getIdPart()));

        provider.setOrganization(organizationEntity);
        final UUID providerID;
        if (resource.getId() == null) {
            providerID = UUID.randomUUID();
        } else {
            providerID = UUID.fromString(resource.getIdElement().getIdPart());
        }

        provider.setID(providerID);

        provider.setProviderNPI(FHIRExtractors.getProviderNPI(resource));
        final HumanName name = resource.getNameFirstRep();
        provider.setFirstName(name.getGivenAsSingleString());
        provider.setLastName(name.getFamily());

        return provider;
    }

    @Override
    public Practitioner toFHIR(FHIREntityConverter converter, ProviderEntity entity) {
        final Practitioner practitioner = new Practitioner();

        practitioner.setId(entity.getID().toString());
        practitioner.addName().
                setFamily(entity.getFirstName())
                .addGiven(entity.getLastName());

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

    @Override
    public Class<Practitioner> getFHIRResource() {
        return Practitioner.class;
    }

    @Override
    public Class<ProviderEntity> getJavaClass() {
        return ProviderEntity.class;
    }
}
