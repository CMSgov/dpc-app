package gov.cms.dpc.fhir.converters.entities;

import ca.uhn.fhir.parser.DataFormatException;
import gov.cms.dpc.common.entities.AddressEntity;
import gov.cms.dpc.common.entities.ContactEntity;
import gov.cms.dpc.common.entities.OrganizationEntity;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import gov.cms.dpc.fhir.FHIRExtractors;
import gov.cms.dpc.fhir.converters.FHIRConverter;
import gov.cms.dpc.fhir.converters.FHIREntityConverter;
import gov.cms.dpc.fhir.validations.profiles.OrganizationProfile;
import org.hl7.fhir.dstu3.model.*;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class OrganizationEntityConverter implements FHIRConverter<Organization, OrganizationEntity> {
    @Override
    public OrganizationEntity fromFHIR(FHIREntityConverter converter, Organization resource) {
        final OrganizationEntity entity = new OrganizationEntity();

        // Add the profile metadata
        final Meta meta = new Meta();
        meta.addProfile(OrganizationProfile.PROFILE_URI);

        // If we have an ID, and it parses, use it
        final String idString = resource.getId();
        UUID orgID;
        if (idString == null) {
            orgID = UUID.randomUUID();
        } else {
//             If we have an ID, we need to strip off the ID header, since we already know the resource type
            orgID = FHIRExtractors.getEntityUUID(idString);
        }

        entity.setId(orgID);

        // Find the first Organization ID that we can use
        final Optional<Identifier> identifier = resource
                .getIdentifier()
                .stream()
                // Don't support UNKNOWN systems for now, only things we can use
                .filter(resourceID -> {
                    final String system = resourceID.getSystem();
                    try {
                        final DPCIdentifierSystem idSys = DPCIdentifierSystem.fromString(system);
                        // MBI does not work, so filter it out
                        return idSys != DPCIdentifierSystem.MBI;
                    } catch (Exception e) {
                        return false;
                    }
                })
                .findFirst();

        if (identifier.isEmpty()) {
            throw new DataFormatException("Identifier must be NPPES or PECOS");
        }
        entity.setOrganizationID(new OrganizationEntity.OrganizationID(
                DPCIdentifierSystem.fromString(identifier.get().getSystem()),
                identifier.get().getValue()));

        entity.setOrganizationName(resource.getName());
        entity.setOrganizationAddress(converter.fromFHIR(AddressEntity.class, resource.getAddressFirstRep()));

        // Add all contact info
        final List<ContactEntity> contactEntities = resource
                .getContact()
                .stream()
                .map(r -> converter.fromFHIR(ContactEntity.class, r))
                .toList();
        // Add the entity reference
        contactEntities.forEach(contact -> contact.setOrganization(entity));
        entity.setContacts(contactEntities);

        return entity;
    }

    @Override
    public Organization toFHIR(FHIREntityConverter converter, OrganizationEntity entity) {
        final Organization org = new Organization();

        org.setId(entity.getId().toString());
        org.addIdentifier(entity.getOrganizationID().toFHIR());
        org.setName(entity.getOrganizationName() );

        org.setAddress(Collections.singletonList(converter.toFHIR(Address.class, entity.getOrganizationAddress())));

        final List<Organization.OrganizationContactComponent> contactComponents = entity.getContacts()
                .stream()
                .map(ContactEntity::toFHIR)
                .toList();
        org.setContact(contactComponents);

        final List<Reference> endpointReferences = entity
                .getEndpoints()
                .stream()
                .map(ep -> new Reference(new IdType("Endpoint", ep.getId().toString())))
                .toList();

        org.setEndpoint(endpointReferences);

        return org;
    }

    @Override
    public Class<Organization> getFHIRResource() {
        return Organization.class;
    }

    @Override
    public Class<OrganizationEntity> getJavaClass() {
        return OrganizationEntity.class;
    }
}
