package gov.cms.dpc.attribution.models;

import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.OffsetDateTime;
import java.util.Objects;

@Entity(name = "attributions")
@NamedQueries({
        @NamedQuery(name = "findRelationship", query = "select 1 " +
                "from providers prov " +
                "left join attributions a on a.provider = prov.providerID " +
                "left join patients as pat on a.patient = pat.patientID " +
                "where prov.providerNPI = :provID and pat.beneficiaryID = :patID")
})
public class AttributionRelationship {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Long attributionID;

    @ManyToOne(cascade = CascadeType.ALL)
    private ProviderEntity provider;

//    @Column(name = "patient_id")
    @ManyToOne(cascade = CascadeType.ALL)
    private PatientEntity patient;

    @Column(name = "created_at")
    @CreationTimestamp
    private OffsetDateTime created;

    public AttributionRelationship() {
        // Not used
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
}
