package gov.cms.dpc.common.entities;

import jakarta.persistence.*;
import jakarta.validation.Valid;
import org.hl7.fhir.dstu3.model.ContactPoint;
import org.hl7.fhir.dstu3.model.Organization;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity(name = "organization_contacts")
public class ContactEntity implements Serializable {
    @Serial
    private static final long serialVersionUID = 42L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID id;
    @ManyToOne
    private OrganizationEntity organization;
    @Valid
    @Embedded
    private NameEntity name;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "contactEntity", orphanRemoval = true)
    private List<ContactPointEntity> telecom;
    @Valid
    @Embedded
    private AddressEntity address;

    public ContactEntity() {
        // Not used
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public OrganizationEntity getOrganization() {
        return organization;
    }

    public void setOrganization(OrganizationEntity organization) {
        this.organization = organization;
    }

    public NameEntity getName() {
        return name;
    }

    public void setName(NameEntity name) {
        this.name = name;
    }

    public List<ContactPointEntity> getTelecom() {
        return telecom;
    }

    public void setTelecom(List<ContactPointEntity> telecom) {
        this.telecom = telecom;
    }

    public AddressEntity getAddress() {
        return address;
    }

    public void setAddress(AddressEntity address) {
        this.address = address;
    }

    public Organization.OrganizationContactComponent toFHIR() {
        final Organization.OrganizationContactComponent contactComponent = new Organization.OrganizationContactComponent();

        contactComponent.setName(this.name.toFHIR());

        final List<ContactPoint> cps = this.telecom
                .stream()
                .map(ContactPointEntity::toFHIR)
                .collect(Collectors.toList());

        contactComponent.setTelecom(cps);
        contactComponent.setAddress(this.address.toFHIR());

        return contactComponent;
    }
}
