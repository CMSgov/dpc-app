package gov.cms.dpc.common.entities;

import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.OffsetDateTime;
import java.util.Objects;

@Entity(name = "attributions")
@NamedQueries({
        @NamedQuery(name = "findRelationship", query = "select a from attributions a " +
                "inner join providers prov on a.provider = prov.providerID " +
                "inner join patients as pat on a.patient = pat.patientID " +
                "where prov.providerNPI = :provID and pat.beneficiaryID = :patID")
})
public class AttributionRelationship {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Long attributionID;

    @ManyToOne(cascade = CascadeType.PERSIST)
    private ProviderEntity provider;

    @ManyToOne(cascade = CascadeType.PERSIST)
    private PatientEntity patient;

    @Column(name = "created_at")
    @CreationTimestamp
    private OffsetDateTime created;

    public AttributionRelationship() {
        this.created = OffsetDateTime.now();
    }

    public AttributionRelationship(ProviderEntity provider, PatientEntity patient) {
        this.provider = provider;
        this.patient = patient;
        this.created = OffsetDateTime.now();
    }

    public AttributionRelationship(ProviderEntity provider, PatientEntity patient, OffsetDateTime created) {
        this.provider = provider;
        this.patient = patient;
        this.created = created;
    }

    public Long getAttributionID() {
        return attributionID;
    }

    public void setAttributionID(Long attributionID) {
        this.attributionID = attributionID;
    }

    public ProviderEntity getProvider() {
        return provider;
    }

    public void setProvider(ProviderEntity provider) {
        this.provider = provider;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AttributionRelationship that = (AttributionRelationship) o;
        return Objects.equals(attributionID, that.attributionID) &&
                Objects.equals(provider, that.provider) &&
                Objects.equals(patient, that.patient) &&
                Objects.equals(created, that.created);
    }

    @Override
    public int hashCode() {
        return Objects.hash(attributionID, provider, patient, created);
    }

    @Override
    public String toString() {
        return "AttributionRelationship{" +
                "attributionID=" + attributionID +
                ", provider=" + provider +
                ", patient=" + patient +
                ", created=" + created +
                '}';
    }
}
