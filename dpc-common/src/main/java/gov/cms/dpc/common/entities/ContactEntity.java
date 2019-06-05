package gov.cms.dpc.common.entities;

import org.hl7.fhir.dstu3.model.CodeableConcept;

import javax.persistence.*;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@Entity(name = "organization_contacts")
public class ContactEntity implements Serializable {
    public static long serialVersionUID = 42L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID id;
    @ManyToOne
    private OrganizationEntity organization;
//    @NotNull
//    private CodeableConcept purpose;
    @Valid
    @Embedded
    private NameEntity name;
    @Valid
    @ElementCollection
    @Transient
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

//    public CodeableConcept getPurpose() {
//        return purpose;
//    }
//
//    public void setPurpose(CodeableConcept purpose) {
//        this.purpose = purpose;
//    }

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
}
