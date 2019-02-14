package gov.cms.dpc.attribution.models;

import org.hibernate.validator.constraints.NotEmpty;

import javax.persistence.*;
import java.util.Objects;

@Entity
@NamedQueries({
        @NamedQuery(name = "findByProvider", query = "select * from attributions where provider_id = :id"),
        @NamedQuery(name = "findRelationshipID", query = "select id from attributions where provider_id = :provID and patient_id = :patID")
})
public class AttributionRelationship {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private Long attributionID;

    @NotEmpty
    @Column(name = "provider_id")
    private String providerID;

    @NotEmpty
    @Column(name = "patient_id")
    private String attributedPatient;

    public AttributionRelationship() {
    }

    public AttributionRelationship(String providerID, String attributedPatients) {
        this.providerID = providerID;
        this.attributedPatient = attributedPatients;
    }

    public Long getAttributionID() {
        return attributionID;
    }

    public void setAttributionID(Long attributionID) {
        this.attributionID = attributionID;
    }

    public String getProviderID() {
        return providerID;
    }

    public void setProviderID(String providerID) {
        this.providerID = providerID;
    }

    public String getAttributedPatient() {
        return attributedPatient;
    }

    public void setAttributedPatient(String attributedPatient) {
        this.attributedPatient = attributedPatient;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AttributionRelationship that = (AttributionRelationship) o;
        return Objects.equals(attributionID, that.attributionID) &&
                Objects.equals(providerID, that.providerID) &&
                Objects.equals(attributedPatient, that.attributedPatient);
    }

    @Override
    public int hashCode() {
        return Objects.hash(attributionID, providerID, attributedPatient);
    }
}
