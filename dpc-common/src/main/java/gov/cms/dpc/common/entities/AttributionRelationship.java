package gov.cms.dpc.common.entities;

import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;

@Entity(name = "attributions")
public class AttributionRelationship {

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

    @Column(name = "created_at", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    @CreationTimestamp
    private OffsetDateTime created;

    @Column(name = "expires_at", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    @NotNull
    private OffsetDateTime expires;

    @Column(name = "removed_at", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime removed;

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
        this.created = created;
    }

    public AttributionRelationship(RosterEntity roster, PatientEntity patient, Timestamp created) {
        this.roster = roster;
        this.patient = patient;
        this.created = OffsetDateTime.ofInstant(created.toInstant(), ZoneOffset.UTC);
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

    public OffsetDateTime getCreated() {
        return created;
    }

    public void setCreated(OffsetDateTime created) {
        this.created = created;
    }

    public boolean isInactive() {
        return inactive;
    }

    public void setInactive(boolean inactive) {
        this.inactive = inactive;
    }

    public OffsetDateTime getExpires() {
        return expires;
    }

    public void setExpires(OffsetDateTime expires) {
        this.expires = expires;
    }

    public OffsetDateTime getRemoved() {
        return removed;
    }

    public void setRemoved(OffsetDateTime removed) {
        this.removed = removed;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AttributionRelationship)) return false;
        AttributionRelationship that = (AttributionRelationship) o;
        return Objects.equals(roster, that.roster) &&
                Objects.equals(patient, that.patient);
    }

    @Override
    public int hashCode() {
        return Objects.hash(roster, patient);
    }

    @Override
    public String toString() {
        return "AttributionRelationship{" +
                "attributionID=" + attributionID +
                ", roster=" + roster +
                ", patient=" + patient +
                ", created=" + created +
                '}';
    }
}
