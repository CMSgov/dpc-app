package gov.cms.dpc.api.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import gov.cms.dpc.api.converters.PublicKeyBytesConverter;
import gov.cms.dpc.api.converters.PublicKeySerializer;
import gov.cms.dpc.common.converters.jackson.OffsetDateTimeToStringConverter;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity(name = "organization_keys")
public class PublicKeyEntity implements Serializable {
    public static final long serialVersionUID = 42L;

    @Id
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID id;

    @NotNull
    @JsonIgnore
    private UUID organization_id;

    @NotNull
    @Convert(converter = PublicKeyBytesConverter.class)
    @JsonSerialize(converter = PublicKeySerializer.class)
    @Column(name = "public_key")
    private SubjectPublicKeyInfo publicKey;

    @Column(name = "created_at", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    @CreationTimestamp
    @JsonSerialize(converter = OffsetDateTimeToStringConverter.class)
    @JsonDeserialize(converter = OffsetDateTimeToStringConverter.class)
    private OffsetDateTime createdAt;

    public PublicKeyEntity() {
        // Hibernate required
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getOrganization_id() {
        return organization_id;
    }

    public void setOrganization_id(UUID organization_id) {
        this.organization_id = organization_id;
    }

    public SubjectPublicKeyInfo getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(SubjectPublicKeyInfo publicKey) {
        this.publicKey = publicKey;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
