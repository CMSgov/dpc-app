package gov.cms.dpc.api.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import gov.cms.dpc.common.converters.jackson.OffsetDateTimeToStringConverter;
import gov.cms.dpc.common.converters.jackson.StringToOffsetDateTimeConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Transient;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serial;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity(name = "organization_tokens")
public class TokenEntity implements Serializable {
    @Serial
    private static final long serialVersionUID = 42L;

    public enum TokenType {
        MACAROON,
        OAUTH
    }

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private String id;

    @JsonIgnore
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Column(name = "organization_id")
    private UUID organizationID;

    @Column(name = "type")
    private TokenType tokenType;

    @Column
    private String label;

    @Column(name = "created_at", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    @JsonSerialize(converter = OffsetDateTimeToStringConverter.class)
    @JsonDeserialize(converter = StringToOffsetDateTimeConverter.class)
    @CreationTimestamp
    private OffsetDateTime createdAt;

    @Column(name = "expires_at", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    @JsonSerialize(converter = OffsetDateTimeToStringConverter.class)
    @JsonDeserialize(converter = StringToOffsetDateTimeConverter.class)
    private OffsetDateTime expiresAt;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Transient
    private String token;


    public TokenEntity() {
        // Hibernate required
    }

    public TokenEntity(String tokenID, UUID organizationID, TokenType type) {
        this.id = tokenID;
        this.organizationID = organizationID;
        this.tokenType = type;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public UUID getOrganizationID() {
        return organizationID;
    }

    public void setOrganizationID(UUID organizationID) {
        this.organizationID = organizationID;
    }

    public TokenType getTokenType() {
        return tokenType;
    }

    public void setTokenType(TokenType tokenType) {
        this.tokenType = tokenType;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(OffsetDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    @JsonProperty("token")
    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TokenEntity that)) return false;
        return Objects.equals(id, that.id) &&
                Objects.equals(organizationID, that.organizationID) &&
                tokenType == that.tokenType &&
                Objects.equals(label, that.label) &&
                Objects.equals(createdAt, that.createdAt) &&
                Objects.equals(expiresAt, that.expiresAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, organizationID, tokenType, label, createdAt, expiresAt);
    }

    @Override
    public String toString() {
        return "TokenEntity{" +
                "id='" + id + '\'' +
                ", organization=" + organizationID +
                ", tokenType=" + tokenType +
                ", label='" + label + '\'' +
                ", createdAt=" + createdAt +
                ", expiresAt=" + expiresAt +
                '}';
    }
}
