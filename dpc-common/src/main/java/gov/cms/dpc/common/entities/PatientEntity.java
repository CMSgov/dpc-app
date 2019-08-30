package gov.cms.dpc.common.entities;

import gov.cms.dpc.fhir.FHIRExtractors;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.validator.constraints.NotEmpty;
import org.hl7.fhir.dstu3.model.HumanName;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Reference;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity(name = "patients")
public class PatientEntity implements Serializable {

    public static final long serialVersionUID = 42L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "uuid")
    @Access(AccessType.PROPERTY)
    private UUID patientID;

    @NotEmpty
    @Column(name = "beneficiary_id", unique = true)
    private String beneficiaryID;

    @Column(name = "first_name")
    private String patientFirstName;
    @Column(name = "last_name")
    private String patientLastName;

    @NotNull
    @Column(name = "dob")
    private LocalDate dob;

    @Column(name = "created_at", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime updatedAt;

    @NotNull
    @ManyToOne
    private OrganizationEntity organization;

    @OneToMany(mappedBy = "patient")
    private List<AttributionRelationship> attributions;

    public PatientEntity() {
//        Hibernate Required
    }

    public UUID getPatientID() {
        return patientID;
    }

    public void setPatientID(UUID patientID) {
        this.patientID = patientID;
    }

    public String getBeneficiaryID() {
        return beneficiaryID;
    }

    public void setBeneficiaryID(String beneficiaryID) {
        this.beneficiaryID = beneficiaryID;
    }

    public String getPatientFirstName() {
        return patientFirstName;
    }

    public void setPatientFirstName(String patientFirstName) {
        this.patientFirstName = patientFirstName;
    }

    public String getPatientLastName() {
        return patientLastName;
    }

    public void setPatientLastName(String patientLastName) {
        this.patientLastName = patientLastName;
    }

    public LocalDate getDob() {
        return dob;
    }

    public void setDob(LocalDate dob) {
        this.dob = dob;
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

    public OrganizationEntity getOrganization() {
        return organization;
    }

    public void setOrganization(OrganizationEntity organization) {
        this.organization = organization;
    }

    public List<AttributionRelationship> getAttributions() {
        return attributions;
    }

    public void setAttributions(List<AttributionRelationship> attributions) {
        this.attributions = attributions;
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

    /**
     * Update {@link Patient} fields.
     * Only first/last name and DOB is supported at this point.
     *
     * @param updated - {@link PatientEntity} with new values
     * @return - {@link PatientEntity} existing record with updated fields.
     */
    public PatientEntity update(PatientEntity updated) {
        this.setPatientFirstName(updated.getPatientFirstName());
        this.setPatientLastName(updated.getPatientLastName());
        this.setDob(updated.getDob());
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PatientEntity)) return false;
        PatientEntity that = (PatientEntity) o;
        return patientID.equals(that.patientID) &&
                beneficiaryID.equals(that.beneficiaryID) &&
                Objects.equals(patientFirstName, that.patientFirstName) &&
                Objects.equals(patientLastName, that.patientLastName) &&
                dob.equals(that.dob);
    }

    @Override
    public int hashCode() {
        return Objects.hash(patientID, beneficiaryID, patientFirstName, patientLastName, dob);
    }

    public static PatientEntity fromFHIR(Patient resource) {

        final PatientEntity patient = new PatientEntity();
        patient.setDob(PatientEntity.toLocalDate(resource.getBirthDate()));
        patient.setBeneficiaryID(FHIRExtractors.getPatientMPI(resource));
        final HumanName name = resource.getNameFirstRep();
        patient.setPatientFirstName(name.getGivenAsSingleString());
        patient.setPatientLastName(name.getFamily());

        // Set the managing organization

        final Reference managingOrganization = resource.getManagingOrganization();
        if (managingOrganization.getReference() != null) {
            final OrganizationEntity organizationEntity = new OrganizationEntity();
            organizationEntity.setId(FHIRExtractors.getEntityUUID(managingOrganization.getReference()));
            patient.setOrganization(organizationEntity);
        }
        return patient;
    }

    public static LocalDate toLocalDate(Date date) {
        return date.toInstant()
                .atZone(ZoneOffset.UTC)
                .toLocalDate();
    }

    public static Date fromLocalDate(LocalDate date) {
        return Date.from(date.atStartOfDay().toInstant(ZoneOffset.UTC));
    }
}
