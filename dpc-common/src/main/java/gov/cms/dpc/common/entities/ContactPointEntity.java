package gov.cms.dpc.common.entities;

import org.hibernate.validator.constraints.NotEmpty;
import org.hl7.fhir.dstu3.model.ContactPoint;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Embeddable
public class ContactPointEntity implements Serializable {
    public static final long serialVersionUID = 42L;

    @NotNull
    @Column(name = "contact_system")
    private ContactPoint.ContactPointSystem system;
    @NotNull
    @Column(name = "contact_use")
    private ContactPoint.ContactPointUse use;
    @NotEmpty
    @Column(name = "contact_value")
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
}
