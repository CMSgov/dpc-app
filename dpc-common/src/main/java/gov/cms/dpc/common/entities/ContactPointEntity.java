package gov.cms.dpc.common.entities;

import gov.cms.dpc.common.annotations.NoHtml;
import org.hl7.fhir.dstu3.model.ContactPoint;

import javax.persistence.*;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.UUID;

@Entity(name = "contact_telecoms")
public class ContactPointEntity implements Serializable {
    public static final long serialVersionUID = 42L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "contact_id", nullable = false)
    private ContactEntity contactEntity;

    @NotNull
    private ContactPoint.ContactPointSystem system;
    @NotNull
    private ContactPoint.ContactPointUse use;
    @NoHtml
    @NotEmpty
    private String value;
    private Integer rank;

    public ContactPointEntity() {
        // Not used
    }

    public ContactPoint.ContactPointSystem getSystem() {
        return system;
    }

    public void setSystem(ContactPoint.ContactPointSystem system) {
        this.system = system;
    }

    public ContactPoint.ContactPointUse getUse() {
        return use;
    }

    public void setUse(ContactPoint.ContactPointUse use) {
        this.use = use;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Integer getRank() {
        return rank;
    }

    public void setRank(Integer rank) {
        this.rank = rank;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public ContactEntity getContactEntity() {
        return contactEntity;
    }

    public void setContactEntity(ContactEntity contactEntity) {
        this.contactEntity = contactEntity;
    }

    public ContactPoint toFHIR() {
        final ContactPoint cp = new ContactPoint();
        cp.setSystem(this.system);
        cp.setUse(this.use);
        cp.setValue(this.value);

        if (this.rank != null) {
            cp.setRank(this.rank);
        }
        return cp;
    }
}
