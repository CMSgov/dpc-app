package gov.cms.dpc.common.consent.entities;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.validator.constraints.NotEmpty;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

@Entity(name = "consent")
public class ConsentEntity implements Serializable {
    public static final String LOINC_CATEGORY = "64292-6";
    public static final String OPT_IN = "OPTIN";
    public static final String OPT_OUT = "OPTOUT";
    public static final String TREATMENT = "TREAT";
    public static final String SCOPE_CODE = "patient-privacy";

    private static final long serialVersionUID = 8702499693412507926L;

    public ConsentEntity() { }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID id;

    @Column(name = "mbi")
    private String mbi;

    @NotEmpty
    @Column(name = "hicn")
    private String hicn;

    @NotNull
    @Column(name = "effective_date", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private LocalDate effectiveDate;

    @Column(name = "policy_code")
    private String policyCode;

    @Column(name = "purpose_code")
    private String purposeCode;

    @Column(name = "loinc_code")
    private String loincCode;

    @Column(name = "scope_code")
    private String scopeCode;

    @Column(name = "created_at", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    @CreationTimestamp
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    @UpdateTimestamp
    private OffsetDateTime updatedAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getMbi() {
        return mbi;
    }

    public void setMbi(String mbi) {
        this.mbi = mbi;
    }

    public String getHicn() {
        return hicn;
    }

    public void setHicn(String hicn) {
        this.hicn = hicn;
    }

    public LocalDate getEffectiveDate() {
        return effectiveDate;
    }

    public void setEffectiveDate(LocalDate effectiveDate) { this.effectiveDate = effectiveDate; }

    public String getPolicyCode() {
        return policyCode;
    }

    public void setPolicyCode(String policyCode) { this.policyCode = policyCode; }

    public String getPurposeCode() {
        return purposeCode;
    }

    public void setPurposeCode(String purposeCode) {
        this.purposeCode = purposeCode;
    }

    public String getLoincCode() {
        return loincCode;
    }

    public void setLoincCode(String loincCode) {
        this.loincCode = loincCode;
    }

    public String getScopeCode() {
        return scopeCode;
    }

    public void setScopeCode(String scopeCode) {
        this.scopeCode = scopeCode;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public static ConsentEntity defaultConsentEntity(Optional<UUID> id, Optional<String> hicn, Optional<String> mbi) {
        ConsentEntity ce = new ConsentEntity();

        ce.setId(UUID.randomUUID());
        id.ifPresent(ce::setId);

        ce.setCreatedAt(OffsetDateTime.now(ZoneId.of("UTC")));
        ce.setEffectiveDate(LocalDate.now(ZoneId.of("UTC")));
        ce.setUpdatedAt(OffsetDateTime.now(ZoneId.of("UTC")));

        hicn.ifPresent(ce::setHicn);
        mbi.ifPresent(ce::setMbi);

        ce.setLoincCode(LOINC_CATEGORY);
        ce.setPolicyCode(OPT_IN);
        ce.setPurposeCode(TREATMENT);
        ce.setScopeCode(SCOPE_CODE);

        return ce;
    }
}
