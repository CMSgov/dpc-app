package gov.cms.dpc.api.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import gov.cms.dpc.api.converters.PublicKeyBytesConverter;
import gov.cms.dpc.api.converters.PublicKeyDeserializer;
import gov.cms.dpc.api.converters.PublicKeySerializer;
import gov.cms.dpc.common.converters.jackson.OffsetDateTimeToStringConverter;
import gov.cms.dpc.common.converters.jackson.StringToOffsetDateTimeConverter;
import io.swagger.annotations.ApiModelProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.validator.constraints.Length;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity(name = "organization_keys")
public class PublicKeyEntity implements Serializable {
    private static final long serialVersionUID = 42L;

    @Id
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID id;

    @NotNull
    @JsonIgnore
    private UUID organization_id;

    @NotNull
    @Convert(converter = PublicKeyBytesConverter.class)
    @JsonSerialize(converter = PublicKeySerializer.class)
    @JsonDeserialize(converter = PublicKeyDeserializer.class)
    @Column(name = "public_key")
    @ApiModelProperty(value = "PEM encoded public key", dataType = "String", example = "---PUBLIC KEY---......---END PUBLIC KEY---")
    private SubjectPublicKeyInfo publicKey;

    @Column(name = "created_at", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    @CreationTimestamp
    @JsonSerialize(converter = OffsetDateTimeToStringConverter.class)
    @JsonDeserialize(converter = StringToOffsetDateTimeConverter.class)
    private OffsetDateTime createdAt;

    @NotEmpty
    @Length(max = 25)
    @ApiModelProperty(value = "Public key label", dataType = "String", example = "example public key")
    private String label;

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

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}
