package gov.cms.dpc.common.entities;

import gov.cms.dpc.common.annotations.NoHtml;
import org.hl7.fhir.dstu3.model.HumanName;
import org.hl7.fhir.dstu3.model.StringType;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

@Embeddable
public class NameEntity implements Serializable {
    public static final long serialVersionUID = 42L;

    @NotNull
    @Column(name = "name_use")
    private HumanName.NameUse use;
    @NoHtml
    @NotEmpty
    private String given;
    @NoHtml
    @NotEmpty
    private String family;
    @NoHtml
    private String prefix;
    @NoHtml
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

    public HumanName toFHIR() {
        final HumanName name = new HumanName();

        name.setFamily(this.family);
        name.setPrefix(List.of(new StringType(this.prefix)));
        name.setSuffix(List.of(new StringType(this.suffix)));
        name.setGiven(List.of(new StringType(this.given)));
        name.setUse(this.use);

        return name;
    }
}
