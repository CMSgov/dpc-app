package gov.cms.dpc.attribution.models;

import gov.cms.dpc.fhir.FHIRExtractors;
import org.hl7.fhir.dstu3.model.HumanName;
import org.hl7.fhir.dstu3.model.Practitioner;

import javax.persistence.*;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity(name = "providers")
@NamedQueries({
        @NamedQuery(name = "getProvider", query = "select 1 from providers a where a.providerNPI = :provID"),
        @NamedQuery(name = "findByProvider", query = "from providers a where a.providerNPI = :id")
})
public class ProviderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID providerID;

    @Column(name = "provider_id")
    private String providerNPI;
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

    public String getProviderNPI() {
        return providerNPI;
    }

    public void setProviderNPI(String providerNPI) {
        this.providerNPI = providerNPI;
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
        return Objects.equals(providerID, that.providerID) &&
                Objects.equals(providerNPI, that.providerNPI) &&
                Objects.equals(providerFirstName, that.providerFirstName) &&
                Objects.equals(providerLastName, that.providerLastName) &&
                Objects.equals(attributedPatients, that.attributedPatients);
    }

    @Override
    public int hashCode() {
        return Objects.hash(providerID, providerNPI, providerFirstName, providerLastName, attributedPatients);
    }

    public static ProviderEntity fromFHIR(Practitioner resource) {

        final ProviderEntity provider = new ProviderEntity();

        provider.setProviderNPI(FHIRExtractors.getProviderNPI(resource));
        final HumanName name = resource.getNameFirstRep();
        provider.setProviderFirstName(name.getGivenAsSingleString());
        provider.setProviderLastName(name.getFamily());

        return provider;
    }
}
