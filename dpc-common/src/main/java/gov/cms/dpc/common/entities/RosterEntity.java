package gov.cms.dpc.common.entities;

import gov.cms.dpc.fhir.FHIRExtractors;
import gov.cms.dpc.fhir.converters.entities.RosterEntityConverter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hl7.fhir.dstu3.model.Element;
import org.hl7.fhir.dstu3.model.Group;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity(name = "roster")
public class RosterEntity implements Serializable {
    public static final long serialVersionUID = 42L;

    @Id
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID id;

    @NotNull
    @OneToOne
    private ProviderEntity providerID;

    @NotNull
    @OneToOne
    private OrganizationEntity managingOrganization;

    @OneToMany(cascade = CascadeType.ALL)
    private List<AttributionRelationship> patients;

    @Column(name = "created_at", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    @CreationTimestamp
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    @UpdateTimestamp
    private OffsetDateTime updatedAt;

    RosterEntity() {
        // Hibernate required
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public ProviderEntity getProviderID() {
        return providerID;
    }

    public void setProviderID(ProviderEntity providerID) {
        this.providerID = providerID;
    }

    public OrganizationEntity getManagingOrganization() {
        return managingOrganization;
    }

    public void setManagingOrganization(OrganizationEntity managingOrganization) {
        this.managingOrganization = managingOrganization;
    }

    public List<AttributionRelationship> getPatients() {
        return patients;
    }

    public void setPatients(List<AttributionRelationship> patients) {
        this.patients = patients;
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

    public Group toFHIR() {
        return RosterEntityConverter.convert(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RosterEntity that = (RosterEntity) o;
        return id.equals(that.id) &&
                providerID.equals(that.providerID) &&
                managingOrganization.equals(that.managingOrganization) &&
                patients.equals(that.patients) &&
                createdAt.equals(that.createdAt) &&
                updatedAt.equals(that.updatedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, providerID, managingOrganization, patients, createdAt, updatedAt);
    }

    public static RosterEntity fromFHIR(Group attributionRoster, ProviderEntity providerEntity) {
        final RosterEntity rosterEntity = new RosterEntity();

        // Set the managing organization
        final OrganizationEntity organizationEntity = new OrganizationEntity();
        final UUID organization = UUID.fromString(FHIRExtractors.getOrganizationID(attributionRoster));
        organizationEntity.setId(organization);
        rosterEntity
                .setManagingOrganization(organizationEntity);

        // Add patients, but only those which are active
        rosterEntity.setPatients(getAttributedPatients(attributionRoster, providerEntity));

        return rosterEntity;
    }

    private static List<AttributionRelationship> getAttributedPatients(Group attributionRoster, ProviderEntity providerEntity) {
        return attributionRoster
                .getMember()
                .stream()
                .filter(member -> !member.getInactive())
                .map(Group.GroupMemberComponent::getEntity)
                .map(Element::getId)
                .map(id -> {
                    final PatientEntity patientEntity = new PatientEntity();
                    patientEntity.setPatientID(UUID.fromString(id));
                    return patientEntity;
                })
                .map(patient -> {
                    final AttributionRelationship relationship = new AttributionRelationship();
                    relationship.setProvider(providerEntity);
                    relationship.setPatient(patient);

                    return relationship;
                })
                .collect(Collectors.toList());
    }
}
