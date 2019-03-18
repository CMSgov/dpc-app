package gov.cms.dpc.attribution.models;

import org.hl7.fhir.dstu3.model.HumanName;
import org.hl7.fhir.dstu3.model.Practitioner;

import javax.persistence.*;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity(name = "providers")
@NamedQueries({
        @NamedQuery(name = "getProvider", query = "select 1 from providers a where a.providerNPPES = :provID"),
        @NamedQuery(name = "findByProvider", query = "from providers a where a.providerNPPES = :id"),
//        @NamedQuery(name = "findRelationship", query = "from providers a where a.providerNPPES = :provID and a.patient = :patID")
})
public class ProviderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID providerID;
    //    @NotEmpty
    @Column(name = "provider_id")
    private String providerNPPES;
    @Column(name = "first_name")
    private String providerFirstName;
    @Column(name = "last_name")
    private String providerLastName;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinTable(name = "attributions",
            joinColumns = {
                    @JoinColumn(name = "provider_id", referencedColumnName = "id")
            },
            inverseJoinColumns = {
                    @JoinColumn(name = "patient_id", referencedColumnName = "id")
            })
    private List<PatientEntity> attributedPatients;

    public ProviderEntity() {
        // Hibernate required
    }

    public UUID getProviderID() {
        return providerID;
    }

    public void setProviderID(UUID providerID) {
        this.providerID = providerID;
    }

    public String getProviderNPPES() {
        return providerNPPES;
    }

    public void setProviderNPPES(String providerNPPES) {
        this.providerNPPES = providerNPPES;
    }

    public String getProviderFirstName() {
        return providerFirstName;
    }

    public void setProviderFirstName(String providerFirstName) {
        this.providerFirstName = providerFirstName;
    }

    public String getProviderLastName() {
        return providerLastName;
    }

    public void setProviderLastName(String providerLastName) {
        this.providerLastName = providerLastName;
    }

    public List<PatientEntity> getAttributedPatients() {
        return attributedPatients;
    }

    public void setAttributedPatients(List<PatientEntity> attributedPatients) {
        this.attributedPatients = attributedPatients;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProviderEntity that = (ProviderEntity) o;
        return providerID.equals(that.providerID) &&
                providerNPPES.equals(that.providerNPPES) &&
                Objects.equals(providerFirstName, that.providerFirstName) &&
                Objects.equals(providerLastName, that.providerLastName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(providerID, providerNPPES, providerFirstName, providerLastName);
    }

    public static ProviderEntity fromFHIR(Practitioner resource) {

        final ProviderEntity provider = new ProviderEntity();

        provider.setProviderNPPES(resource.getIdentifierFirstRep().getValue());
        final HumanName name = resource.getNameFirstRep();
        provider.setProviderFirstName(name.getGivenAsSingleString());
        provider.setProviderLastName(name.getFamily());

        return provider;
    }
}
