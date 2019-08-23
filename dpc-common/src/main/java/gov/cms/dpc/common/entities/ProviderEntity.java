package gov.cms.dpc.common.entities;

import gov.cms.dpc.fhir.FHIRExtractors;
import gov.cms.dpc.fhir.converters.entities.ProviderEntityConverter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hl7.fhir.dstu3.model.HumanName;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Practitioner;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity(name = "providers")
@NamedQueries(value = {
        @NamedQuery(name = "getProvider", query = "select 1 from providers a where a.providerNPI = :provID"),
        @NamedQuery(name = "findByProvider", query = "from providers a where a.providerNPI = :id"),
        @NamedQuery(name = "getAllProviders", query = "from providers p")
})
public class ProviderEntity implements Serializable {

    public static final long serialVersionUID = 42L;

    @Id
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID providerID;

    @Column(name = "provider_id", unique = true)
    private String providerNPI;
    @Column(name = "first_name")
    private String providerFirstName;
    @Column(name = "last_name")
    private String providerLastName;

    @NotNull
    @ManyToOne
    private OrganizationEntity organization;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinTable(name = "attributions",
            joinColumns = {
                    @JoinColumn(name = "roster_id", referencedColumnName = "id")
            },
            inverseJoinColumns = {
                    @JoinColumn(name = "patient_id", referencedColumnName = "id")
            })
    private List<PatientEntity> attributedPatients;

    @Column(name = "created_at", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime updatedAt;

    public ProviderEntity() {
        // Hibernate required
    }

    public UUID getProviderID() {
        return providerID;
    }

    public void setProviderID(UUID providerID) {
        this.providerID = providerID;
    }

    public String getProviderNPI() {
        return providerNPI;
    }

    public void setProviderNPI(String providerNPI) {
        this.providerNPI = providerNPI;
    }

    public String getProviderFirstName() {
        return providerFirstName;
    }

    public void setProviderFirstName(String providerFirstName) {
        this.providerFirstName = providerFirstName;
    }

    public String getProviderLastName() {
        return providerLastName;
    }

    public void setProviderLastName(String providerLastName) {
        this.providerLastName = providerLastName;
    }

    public List<PatientEntity> getAttributedPatients() {
        return attributedPatients;
    }

    public void setAttributedPatients(List<PatientEntity> attributedPatients) {
        this.attributedPatients = attributedPatients;
    }

    public OrganizationEntity getOrganization() {
        return organization;
    }

    public void setOrganization(OrganizationEntity organization) {
        this.organization = organization;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @PrePersist
    public void setCreation() {
        final OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        this.setCreatedAt(now);
        this.setUpdatedAt(now);
    }

    @PreUpdate
    public void setUpdateTime() {
        this.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
    }

    public Practitioner toFHIR() {
        return ProviderEntityConverter.convert(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProviderEntity)) return false;
        ProviderEntity that = (ProviderEntity) o;
        return Objects.equals(providerID, that.providerID) &&
                Objects.equals(providerNPI, that.providerNPI) &&
                Objects.equals(providerFirstName, that.providerFirstName) &&
                Objects.equals(providerLastName, that.providerLastName) &&
                Objects.equals(attributedPatients, that.attributedPatients);
    }

    @Override
    public int hashCode() {
        return Objects.hash(providerID, providerNPI, providerFirstName, providerLastName, attributedPatients);
    }

    public static ProviderEntity fromFHIR(Practitioner resource) {
        return fromFHIR(resource, null);
    }

    public static ProviderEntity fromFHIR(Practitioner resource, UUID resourceID) {
        final ProviderEntity provider = new ProviderEntity();

        // Get the Organization, from the tag field
        final String organizationID = FHIRExtractors.getOrganizationID(resource);

        final OrganizationEntity organizationEntity = new OrganizationEntity();
        organizationEntity.setId(UUID.fromString(new IdType(organizationID).getIdPart()));

        provider.setOrganization(organizationEntity);
        provider.setProviderID(Objects.requireNonNullElseGet(resourceID, UUID::randomUUID));

        provider.setProviderNPI(FHIRExtractors.getProviderNPI(resource));
        final HumanName name = resource.getNameFirstRep();
        provider.setProviderFirstName(name.getGivenAsSingleString());
        provider.setProviderLastName(name.getFamily());

        return provider;
    }
}
