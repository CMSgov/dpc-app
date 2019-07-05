package gov.cms.dpc.common.entities;

import gov.cms.dpc.fhir.FHIRExtractors;
import gov.cms.dpc.fhir.converters.entities.ProviderEntityConverter;
import org.hibernate.annotations.SQLInsert;
import org.hl7.fhir.dstu3.model.HumanName;
import org.hl7.fhir.dstu3.model.Practitioner;

import javax.persistence.*;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity(name = "providers")
@NamedQueries(value = {
        @NamedQuery(name = "getProvider", query = "select 1 from providers a where a.providerNPI = :provID"),
        @NamedQuery(name = "findByProvider", query = "from providers a where a.providerNPI = :id"),
        @NamedQuery(name = "getAllProviders", query = "from providers p")
})
@SQLInsert(sql = "INSERT INTO providers(first_name, last_name, provider_id, id) VALUES(?, ?, ?, ?)" +
        " ON CONFLICT (provider_id) DO UPDATE SET last_name = EXCLUDED.last_name," +
        " first_name = EXCLUDED.first_name")
public class ProviderEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID providerID;

    @Column(name = "provider_id", unique = true)
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

    public Practitioner toFHIR() {
        return ProviderEntityConverter.convert(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProviderEntity)) return false;
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
        return fromFHIR(resource, null);
    }

    public static ProviderEntity fromFHIR(Practitioner resource, UUID resourceID) {
        final ProviderEntity provider = new ProviderEntity();

        if (resource.getId() == null) {
            provider.setProviderID(UUID.randomUUID());
        } else {
            provider.setProviderID(FHIRExtractors.getEntityUUID(resource.getId()));
        }

        provider.setProviderNPI(FHIRExtractors.getProviderNPI(resource));
        final HumanName name = resource.getNameFirstRep();
        provider.setProviderFirstName(name.getGivenAsSingleString());
        provider.setProviderLastName(name.getFamily());

        return provider;
    }
}
