package gov.cms.dpc.common.entities;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.validator.constraints.NotEmpty;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
public class ConsentEntity implements Serializable {

    private static final long serialVersionUID = 42L;

    @Id
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID id;

    @NotEmpty
    private String mbi;
    @NotEmpty
    private String hicn;

    @NotNull
    @Column(columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime effectiveDate;

    private String policyCode;
    private String purposeCode;
    private String loincCode;
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

    public OffsetDateTime getEffectiveDate() {
        return effectiveDate;
    }

    public void setEffectiveDate(OffsetDateTime effectiveDate) {
        this.effectiveDate = effectiveDate;
    }

    public String getPolicyCode() {
        return policyCode;
    }

    public void setPolicyCode(String policyCode) {
        this.policyCode = policyCode;
    }

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
}
