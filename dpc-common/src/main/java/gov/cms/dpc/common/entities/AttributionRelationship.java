package gov.cms.dpc.common.entities;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;

@Entity(name = "attributions")
public class AttributionRelationship implements Serializable {

    public static final long serialVersionUID = 42L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    @Access(AccessType.PROPERTY)
    private Long attributionID;

    @ManyToOne
    private RosterEntity roster;

    @ManyToOne(fetch = FetchType.LAZY)
    private PatientEntity patient;

    private boolean inactive = false;

    @NotNull
    @Column(name = "period_begin", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime periodBegin;

    @NotNull
    @Column(name = "period_end", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime periodEnd;

    public AttributionRelationship() {
        // Hibernate required
    }

    public AttributionRelationship(RosterEntity roster, PatientEntity patient) {
        this.roster = roster;
        this.patient = patient;
    }

    public AttributionRelationship(RosterEntity roster, PatientEntity patient, OffsetDateTime created) {
        this.roster = roster;
        this.patient = patient;
        this.periodBegin = created;
    }

    public AttributionRelationship(RosterEntity roster, PatientEntity patient, Timestamp created) {
        this.roster = roster;
        this.patient = patient;
        this.periodBegin = OffsetDateTime.ofInstant(created.toInstant(), ZoneOffset.UTC);
    }

    public Long getAttributionID() {
        return attributionID;
    }

    public void setAttributionID(Long attributionID) {
        this.attributionID = attributionID;
    }

    public RosterEntity getRoster() {
        return roster;
    }

    public void setRoster(RosterEntity roster) {
        this.roster = roster;
    }

    public PatientEntity getPatient() {
        return patient;
    }

    public void setPatient(PatientEntity patient) {
        this.patient = patient;
    }

    public boolean isInactive() {
        return inactive;
    }

    public void setInactive(boolean inactive) {
        this.inactive = inactive;
    }

    public OffsetDateTime getPeriodBegin() {
        return periodBegin;
    }

    public void setPeriodBegin(OffsetDateTime periodBegin) {
        this.periodBegin = periodBegin;
    }

    public OffsetDateTime getPeriodEnd() {
        return periodEnd;
    }

    public void setPeriodEnd(OffsetDateTime periodEnd) {
        this.periodEnd = periodEnd;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AttributionRelationship)) return false;
        AttributionRelationship that = (AttributionRelationship) o;
        return inactive == that.inactive &&
                Objects.equals(attributionID, that.attributionID) &&
                Objects.equals(roster, that.roster) &&
                Objects.equals(patient, that.patient) &&
                Objects.equals(periodBegin, that.periodBegin) &&
                Objects.equals(periodEnd, that.periodEnd);
    }

    @Override
    public int hashCode() {
        return Objects.hash(attributionID, roster, patient, inactive, periodBegin, periodEnd);
    }

    @Override
    public String toString() {
        return "AttributionRelationship{" +
                "attributionID=" + attributionID +
                ", roster=" + roster +
                ", patient=" + patient +
                ", inactive=" + inactive +
                ", begin=" + periodBegin +
                ", end=" + periodEnd +
                '}';
    }
}
