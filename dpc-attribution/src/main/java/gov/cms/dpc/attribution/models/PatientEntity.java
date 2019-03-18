package gov.cms.dpc.attribution.models;

import org.hibernate.validator.constraints.NotEmpty;
import org.hl7.fhir.dstu3.model.HumanName;
import org.hl7.fhir.dstu3.model.Patient;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;

@Entity(name = "patients")
public class PatientEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID patientID;

    @NotEmpty
    @Column(name = "beneficiary_id")
    private String beneficiaryID;

    @Column(name = "first_name")
    private String patientFirstName;
    @Column(name = "last_name")
    private String patientLastName;

    @NotNull
    @Column(name = "dob")
    private LocalDate dob;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
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
        patient.setBeneficiaryID(resource.getIdentifierFirstRep().getValue());
        final HumanName name = resource.getNameFirstRep();
        patient.setPatientFirstName(name.getGivenAsSingleString());
        patient.setPatientLastName(name.getFamily());

        return patient;
    }

    public static LocalDate toLocalDate(Date date) {
        return date.toInstant()
                .atZone(ZoneOffset.UTC)
                .toLocalDate();
    }
}
