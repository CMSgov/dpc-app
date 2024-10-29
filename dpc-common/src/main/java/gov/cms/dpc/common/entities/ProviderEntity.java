package gov.cms.dpc.common.entities;

import gov.cms.dpc.common.annotations.NPI;
import gov.cms.dpc.common.annotations.NoHtml;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Objects;

@Entity(name = "providers")
public class ProviderEntity extends PersonEntity {

    public static final long serialVersionUID = 42L;

    @NoHtml
    @NPI
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

    public ProviderEntity update(ProviderEntity entity) {
        this.setFirstName(entity.getFirstName());
        this.setLastName(entity.getLastName());
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProviderEntity)) return false;
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
