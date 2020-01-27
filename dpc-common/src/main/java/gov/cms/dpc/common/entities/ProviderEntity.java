package gov.cms.dpc.common.entities;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity(name = "providers")
public class ProviderEntity extends PersonEntity {

    @Column(name = "provider_id", unique = true)
    private String providerNPI;

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

    @OneToMany(cascade = CascadeType.REMOVE, orphanRemoval = true, mappedBy = "attributedProvider")
    private List<RosterEntity> attributionRosters;

    public ProviderEntity() {
        // Hibernate required
    }

    public String getProviderNPI() {
        return providerNPI;
    }

    public void setProviderNPI(String providerNPI) {
        this.providerNPI = providerNPI;
    }

    public List<PatientEntity> getAttributedPatients() {
        return attributedPatients;
    }

    public void setAttributedPatients(List<PatientEntity> attributedPatients) {
        this.attributedPatients = attributedPatients;
    }

    public List<RosterEntity> getAttributionRosters() {
        return attributionRosters;
    }

    public void setAttributionRosters(List<RosterEntity> attributionRosters) {
        this.attributionRosters = attributionRosters;
    }

    public OrganizationEntity getOrganization() {
        return organization;
    }

    public void setOrganization(OrganizationEntity organization) {
        this.organization = organization;
    }

    // Temporal setters/getters need to be on the child class, in order for Jooq to find them
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

    public ProviderEntity update(ProviderEntity entity) {
        this.setFirstName(entity.getFirstName());
        this.setLastName(entity.getLastName());
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProviderEntity that = (ProviderEntity) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(providerNPI, that.providerNPI) &&
                Objects.equals(organization, that.organization) &&
                Objects.equals(attributedPatients, that.attributedPatients) &&
                Objects.equals(attributionRosters, that.attributionRosters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, providerNPI, organization, attributedPatients, attributionRosters);
    }
}
