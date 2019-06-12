package gov.cms.dpc.common.entities;

import org.hibernate.validator.constraints.NotEmpty;
import org.hl7.fhir.dstu3.model.HumanName;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Embeddable
public class NameEntity implements Serializable {
    public static final long serialVersionUID = 42L;

    @NotNull
    @Column(name = "name_use")
    private HumanName.NameUse use;
    @NotEmpty
    private String given;
    @NotEmpty
    private String family;
    private String prefix;
    private String suffix;

    public NameEntity() {
        // Not used
    }

    public HumanName.NameUse getUse() {
        return use;
    }

    public void setUse(HumanName.NameUse use) {
        this.use = use;
    }

    public String getGiven() {
        return given;
    }

    public void setGiven(String given) {
        this.given = given;
    }

    public String getFamily() {
        return family;
    }

    public void setFamily(String family) {
        this.family = family;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getSuffix() {
        return suffix;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }
}
