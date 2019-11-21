package gov.cms.dpc.common.entities;

import ca.uhn.fhir.parser.DataFormatException;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import gov.cms.dpc.fhir.FHIRConvertable;
import gov.cms.dpc.fhir.FHIRExtractors;
import gov.cms.dpc.fhir.converters.AddressConverter;
import gov.cms.dpc.fhir.converters.ContactElementConverter;
import gov.cms.dpc.fhir.validations.profiles.OrganizationProfile;
import org.hibernate.validator.constraints.NotEmpty;
import org.hl7.fhir.dstu3.model.*;

import javax.persistence.*;
import javax.validation.Valid;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity(name = "organizations")
public class OrganizationEntity implements Serializable, FHIRConvertable<OrganizationEntity, Organization> {
    public static final long serialVersionUID = 42L;

    @Id
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID id;

    @Embedded
    private OrganizationID organizationID;

    @NotEmpty
    @Column(name = "organization_name")
    private String organizationName;

    @Valid
    @Embedded
    private AddressEntity organizationAddress;

    @Valid
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "organization")
    private List<ContactEntity> contacts;

    @Valid
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "organization")
    private List<EndpointEntity> endpoints;

    @OneToMany(cascade = CascadeType.REMOVE, mappedBy = "organization")
    private List<ProviderEntity> providers;

    @OneToMany(cascade = CascadeType.REMOVE, mappedBy = "organization")
    private List<PatientEntity> patients;

    public OrganizationEntity() {
        // Not used
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public OrganizationID getOrganizationID() {
        return organizationID;
    }

    public void setOrganizationID(OrganizationID organizationID) {
        this.organizationID = organizationID;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }

    public AddressEntity getOrganizationAddress() {
        return organizationAddress;
    }

    public void setOrganizationAddress(AddressEntity organizationAddress) {
        this.organizationAddress = organizationAddress;
    }

    public List<ContactEntity> getContacts() {
        return contacts;
    }

    public void setContacts(List<ContactEntity> contacts) {
        this.contacts = contacts;
    }

    public List<EndpointEntity> getEndpoints() {
        return endpoints;
    }

    public void setEndpoints(List<EndpointEntity> endpoints) {
        this.endpoints = endpoints;
    }

    public List<ProviderEntity> getProviders() {
        return providers;
    }

    public void setProviders(List<ProviderEntity> providers) {
        this.providers = providers;
    }

    public List<PatientEntity> getPatients() {
        return patients;
    }

    public void setPatients(List<PatientEntity> patients) {
        this.patients = patients;
    }

    @Override
    public OrganizationEntity fromFHIR(Organization resource) {
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
        entity.setOrganizationID(new OrganizationID(
                DPCIdentifierSystem.fromString(identifier.get().getSystem()),
                identifier.get().getValue()));

        entity.setOrganizationName(resource.getName());
        entity.setOrganizationAddress(AddressConverter.convert(resource.getAddressFirstRep()));

        // Add all contact info
        final List<ContactEntity> contactEntities = resource
                .getContact()
                .stream()
                .map(ContactElementConverter::convert)
                .collect(Collectors.toList());
        // Add the entity reference
        contactEntities.forEach(contact -> contact.setOrganization(entity));
        entity.setContacts(contactEntities);

        return entity;
    }

    @Override
    public Organization toFHIR() {
        // TODO: This will be dramatically improved in the future. (DPC-276)

        final Organization org = new Organization();

        org.setId(this.id.toString());
        org.addIdentifier(this.organizationID.toFHIR());
        org.setName(this.organizationName);
        org.setAddress(Collections.singletonList(this.organizationAddress.toFHIR()));

        final List<Organization.OrganizationContactComponent> contactComponents = this.contacts
                .stream()
                .map(ContactEntity::toFHIR)
                .collect(Collectors.toList());
        org.setContact(contactComponents);

        final List<Reference> endpointReferences = this
                .endpoints
                .stream()
                .map(ep -> new Reference(new IdType("Endpoint", ep.getId().toString())))
                .collect(Collectors.toList());

        org.setEndpoint(endpointReferences);

        return org;
    }

    /**
     * Update {@link Organization} fields.
     *
     * @param updated - {@link OrganizationEntity} with new values
     * @return - {@link OrganizationEntity} existing record with updated fields.
     */
    public OrganizationEntity update(OrganizationEntity updated) {
        this.setOrganizationName(updated.getOrganizationName());
        this.setOrganizationAddress(updated.getOrganizationAddress());
        this.setContacts(updated.getContacts());
        this.setPatients(updated.getPatients());
        this.setProviders(updated.getProviders());
        return this;
    }

    @Embeddable
    public static class OrganizationID implements Serializable {
        public static final long serialVersionUID = 42L;

        @Column(name = "id_system")
        private DPCIdentifierSystem system;

        @NotEmpty
        @Column(name = "id_value")
        private String value;

        public OrganizationID() {
            // Hibernate Required
        }

        public OrganizationID(DPCIdentifierSystem system, String value) {
            this.system = system;
            this.value = value;
        }

        public DPCIdentifierSystem getSystem() {
            return system;
        }

        public void setSystem(DPCIdentifierSystem system) {
            this.system = system;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public Identifier toFHIR() {
            final Identifier id = new Identifier();
            id.setSystem(this.system.getSystem());
            id.setValue(this.value);

            return id;
        }
    }
}
