package gov.cms.dpc.common.entities;

import gov.cms.dpc.common.annotations.NoHtml;
import org.hibernate.validator.constraints.NotEmpty;
import org.hl7.fhir.dstu3.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.dstu3.model.Patient;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@SuppressWarnings("WeakerAccess")
@Entity(name = "patients")
public class PatientEntity extends PersonEntity {

    public static final long serialVersionUID = 42L;
    /* For details of the MBI format, see: https://www.cms.gov/Medicare/New-Medicare-Card/Understanding-the-MBI.pdf
    This pattern is similar to that format, but is less restrictive to accommodate testing. Synthetic MBIs should
    include letters and numbers not permitted in real MBIs. */
    public static final String MBI_FORMAT = "^\\d[a-zA-Z][a-zA-Z0-9]\\d[a-zA-Z][a-zA-Z0-9]\\d[a-zA-Z]{2}\\d{2}$";

    @NoHtml
    @NotEmpty
    @Column(name = "beneficiary_id", unique = true)
    @Pattern(regexp = MBI_FORMAT, message = "Must be a Medicare Beneficiary Identifier (MBI)")
    private String beneficiaryID;

    @NoHtml
    @Column(name = "mbi_hash")
    private String mbiHash;

    @NotNull
    @Column(name = "dob")
    private LocalDate dob;

    @NotNull
    private AdministrativeGender gender;

    @NotNull
    @ManyToOne
    private OrganizationEntity organization;

    @OneToMany(mappedBy = "patient")
    private List<AttributionRelationship> attributions;

    public PatientEntity() {
//        Hibernate Required
    }

    public String getBeneficiaryID() {
        return beneficiaryID;
    }

    public void setBeneficiaryID(String beneficiaryID) {
        this.beneficiaryID = beneficiaryID;
    }

    public String getMbiHash() {
        return mbiHash;
    }

    public void setMbiHash(String mbiHash) {
        this.mbiHash = mbiHash;
    }

    public LocalDate getDob() {
        return dob;
    }

    public void setDob(LocalDate dob) {
        this.dob = dob;
    }

    public AdministrativeGender getGender() {
        return gender;
    }

    public void setGender(AdministrativeGender gender) {
        this.gender = gender;
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

    /**
     * Update {@link Patient} fields.
     * Only first/last name and DOB is supported at this point.
     *
     * @param updated - {@link PatientEntity} with new values
     * @return - {@link PatientEntity} existing record with updated fields.
     */
    public PatientEntity update(PatientEntity updated) {
        this.setFirstName(updated.getFirstName());
        this.setLastName(updated.getLastName());
        this.setDob(updated.getDob());
        this.setGender(updated.getGender());
        return this;
    }

    @PrePersist
    @PreUpdate
    public void upperCaseBeneficiaryId() {
        if(this.getBeneficiaryID()!=null){
            this.setBeneficiaryID(this.getBeneficiaryID().toUpperCase());
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PatientEntity)) return false;
        PatientEntity that = (PatientEntity) o;
        return Objects.equals(getID(), that.getID()) &&
                Objects.equals(beneficiaryID, that.beneficiaryID) &&
                Objects.equals(mbiHash, that.mbiHash) &&
                Objects.equals(dob, that.dob) &&
                gender == that.gender &&
                Objects.equals(organization, that.organization) &&
                Objects.equals(attributions, that.attributions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getID(), beneficiaryID, mbiHash, dob, gender, organization, attributions);
    }

    @SuppressWarnings("JdkObsolete") // Date class is used by FHIR stu3 Patient model
    public static LocalDate toLocalDate(Date date) {
        return date.toInstant()
                .atZone(ZoneOffset.UTC)
                .toLocalDate();
    }

    @SuppressWarnings("JdkObsolete") // Date class is used by FHIR stu3 Patient model
    public static Date fromLocalDate(LocalDate date) {
        return Date.from(date.atStartOfDay().toInstant(ZoneOffset.UTC));
    }


}
