package gov.cms.dpc.common.entities;

import gov.cms.dpc.fhir.FHIRExtractors;
import gov.cms.dpc.fhir.converters.entities.ProviderRoleEntityConverter;
import org.hibernate.annotations.CreationTimestamp;
import org.hl7.fhir.dstu3.model.PractitionerRole;

import javax.persistence.*;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity(name = "provider_roles")
@Table(name = "provider_roles",
        uniqueConstraints = {@UniqueConstraint(columnNames = {"organization_id", "provider_id"})})
public class ProviderRoleEntity implements Serializable {

    public static final long serialVersionUID = 42L;

    @Id
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID roleID;

    @OneToOne(fetch = FetchType.LAZY)
    private OrganizationEntity organization;

    @OneToOne(fetch = FetchType.LAZY)
    private ProviderEntity provider;

    @CreationTimestamp
    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    private ProviderRoleEntity() {
        // Hibernate required
    }

    public UUID getRoleID() {
        return roleID;
    }

    public void setRoleID(UUID roleID) {
        this.roleID = roleID;
    }

    public OrganizationEntity getOrganization() {
        return organization;
    }

    public void setOrganization(OrganizationEntity organization) {
        this.organization = organization;
    }

    public ProviderEntity getProvider() {
        return provider;
    }

    public void setProvider(ProviderEntity provider) {
        this.provider = provider;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public PractitionerRole toFHIR() {
        return ProviderRoleEntityConverter.convert(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProviderRoleEntity)) return false;
        ProviderRoleEntity that = (ProviderRoleEntity) o;
        return Objects.equals(roleID, that.roleID) &&
                Objects.equals(organization, that.organization) &&
                Objects.equals(provider, that.provider);
    }

    @Override
    public int hashCode() {
        return Objects.hash(roleID, organization, provider);
    }

    public static ProviderRoleEntity fromFHIR(PractitionerRole resource) {
        return fromFHIR(resource, null);
    }

    public static ProviderRoleEntity fromFHIR(PractitionerRole resource, UUID resourceID) {
        // Create the resource links
        final ProviderEntity pe = new ProviderEntity();
        pe.setProviderID(FHIRExtractors.getEntityUUID(resource.getPractitioner().getReference()));

        final OrganizationEntity oe = new OrganizationEntity();
        oe.setId(FHIRExtractors.getEntityUUID(resource.getOrganization().getReference()));

        final ProviderRoleEntity roleEntity = new ProviderRoleEntity();
        roleEntity.setRoleID(Objects.requireNonNullElseGet(resourceID, UUID::randomUUID));

        roleEntity.setOrganization(oe);
        roleEntity.setProvider(pe);

        return roleEntity;
    }
}
