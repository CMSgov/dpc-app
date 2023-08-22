package gov.cms.dpc.common.entities;

import gov.cms.dpc.fhir.FHIRExtractors;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hl7.fhir.dstu3.model.Group;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Reference;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity(name = "rosters")
public class RosterEntity implements Serializable {
    public static final long serialVersionUID = 42L;

    @Id
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "uuid")
    @Access(AccessType.PROPERTY)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id")
    private ProviderEntity attributedProvider;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private OrganizationEntity managingOrganization;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "roster", fetch = FetchType.EAGER)
    private List<AttributionRelationship> attributions;

    @Column(name = "created_at", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    @CreationTimestamp
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    @UpdateTimestamp
    private OffsetDateTime updatedAt;

    public RosterEntity() {
        // Hibernate required
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public ProviderEntity getAttributedProvider() {
        return attributedProvider;
    }

    public void setAttributedProvider(ProviderEntity attributedProvider) {
        this.attributedProvider = attributedProvider;
    }

    public OrganizationEntity getManagingOrganization() {
        return managingOrganization;
    }

    public void setManagingOrganization(OrganizationEntity managingOrganization) {
        this.managingOrganization = managingOrganization;
    }

    public List<AttributionRelationship> getAttributions() {
        return attributions;
    }

    public void setAttributions(List<AttributionRelationship> attributions) {
        this.attributions = attributions;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RosterEntity)) return false;
        RosterEntity that = (RosterEntity) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(attributedProvider, that.attributedProvider) &&
                Objects.equals(managingOrganization, that.managingOrganization) &&
                Objects.equals(attributions, that.attributions) &&
                Objects.equals(createdAt, that.createdAt) &&
                Objects.equals(updatedAt, that.updatedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, attributedProvider, managingOrganization, createdAt, updatedAt);
    }

    public static RosterEntity fromFHIR(Group attributionRoster, ProviderEntity providerEntity, OffsetDateTime expiration) {
        final RosterEntity rosterEntity = new RosterEntity();

        final UUID rosterID;
        if (attributionRoster.getId() == null) {
            rosterID = UUID.randomUUID();
        } else {
            rosterID = UUID.fromString(attributionRoster.getId());
        }
        rosterEntity.setId(rosterID);

        // Set the managing organization
        final OrganizationEntity organizationEntity = new OrganizationEntity();
        final UUID organization = UUID.fromString(FHIRExtractors.getOrganizationID(attributionRoster));
        organizationEntity.setId(organization);
        rosterEntity
                .setManagingOrganization(organizationEntity);

        // Add patients, but only those which are active
        rosterEntity.setAttributions(getAttributedPatients(attributionRoster, rosterEntity, expiration));

        // Add the provider
        rosterEntity.setAttributedProvider(providerEntity);

        return rosterEntity;
    }

    private static List<AttributionRelationship> getAttributedPatients(Group attributionRoster, RosterEntity roster, OffsetDateTime expires) {
        final OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        return attributionRoster
                .getMember()
                .stream()
                .filter(member -> !member.getInactive())
                .map(Group.GroupMemberComponent::getEntity)
                .map(Reference::getReference)
                .map(IdType::new)
                .map(id -> {
                    final PatientEntity patientEntity = new PatientEntity();
                    patientEntity.setID(UUID.fromString(id.getIdPart()));
                    final AttributionRelationship relationship = new AttributionRelationship(roster, patientEntity, now);
                    relationship.setPeriodEnd(expires);
                    return relationship;
                })
                .collect(Collectors.toList());
    }
}
