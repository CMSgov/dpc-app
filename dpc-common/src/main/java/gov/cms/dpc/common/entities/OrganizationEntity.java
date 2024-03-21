package gov.cms.dpc.common.entities;

import gov.cms.dpc.common.annotations.NoHtml;
import gov.cms.dpc.common.annotations.OrganizationId;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Organization;

import javax.persistence.*;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@Entity(name = "organizations")
public class OrganizationEntity implements Serializable {
    public static final long serialVersionUID = 42L;

    @Id
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID id;

    @Valid
    @OrganizationId
    @Embedded
    private OrganizationID organizationID;

    @NoHtml
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

    @OneToMany(cascade = CascadeType.REMOVE, mappedBy = "managingOrganization")
    private List<RosterEntity> rosters;

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

    public void addEndpoint(EndpointEntity endpoint) {
        this.endpoints.add(endpoint);
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

    public List<RosterEntity> getRosters() {
        return rosters;
    }

    public void setRosters(List<RosterEntity> rosters) {
        this.rosters = rosters;
    }

    /**
     * Update {@link Organization} fields.
     *
     * @param updated - {@link OrganizationEntity} with new values
     * @return - {@link OrganizationEntity} existing record with updated fields.
     */
    public OrganizationEntity update(OrganizationEntity updated) {
        this.setOrganizationID(updated.getOrganizationID());
        this.setOrganizationName(updated.getOrganizationName());
        this.setOrganizationAddress(updated.getOrganizationAddress());
        this.setContacts(updated.getContacts());
        this.setEndpoints(updated.getEndpoints());
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
