package gov.cms.dpc.api.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import gov.cms.dpc.api.converters.CertificateByesConverter;
import gov.cms.dpc.api.converters.CertificateSerializer;
import gov.cms.dpc.common.converters.jackson.OffsetDateTimeToStringConverter;
import gov.cms.dpc.common.entities.OrganizationEntity;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity(name = "auth.organization_certificates")
public class CertificateEntity implements Serializable {
    public static final long serialVersionUID = 42L;

    @Id
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    @JsonIgnore
    private OrganizationEntity managingOrganization;

    @NotNull
    @Convert(converter = CertificateByesConverter.class)
    @JsonSerialize(converter = CertificateSerializer.class)
    private SubjectPublicKeyInfo certificate;

    @Column(name = "created_at", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    @CreationTimestamp
    @JsonSerialize(converter = OffsetDateTimeToStringConverter.class)
    @JsonDeserialize(converter = OffsetDateTimeToStringConverter.class)
    private OffsetDateTime createdAt;

    public CertificateEntity() {
        // Hibernate required
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public OrganizationEntity getManagingOrganization() {
        return managingOrganization;
    }

    public void setManagingOrganization(OrganizationEntity managingOrganization) {
        this.managingOrganization = managingOrganization;
    }

    public SubjectPublicKeyInfo getCertificate() {
        return certificate;
    }

    public void setCertificate(SubjectPublicKeyInfo certificate) {
        this.certificate = certificate;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
