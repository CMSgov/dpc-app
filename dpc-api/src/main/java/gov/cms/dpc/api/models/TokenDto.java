package gov.cms.dpc.api.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import gov.cms.dpc.common.annotations.NoHtml;
import gov.cms.dpc.common.converters.jackson.OffsetDateTimeToStringConverter;
import gov.cms.dpc.common.converters.jackson.StringToOffsetDateTimeConverter;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;


public class TokenDto implements Serializable {

    private static final long serialVersionUID = 4464861101748469238L;

    private String id;

    private String tokenType;

    @JsonIgnore
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private UUID organizationID;

    @NoHtml
    private String label;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String token;

    @JsonSerialize(converter = OffsetDateTimeToStringConverter.class)
    @JsonDeserialize(converter = StringToOffsetDateTimeConverter.class)
    private OffsetDateTime createdAt;

    @JsonSerialize(converter = OffsetDateTimeToStringConverter.class)
    @JsonDeserialize(converter = StringToOffsetDateTimeConverter.class)
    private OffsetDateTime expiresAt;

    public TokenDto() {
        // Jackson required
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTokenType() { return tokenType; }

    public void setTokenType(String tokenType) { this.tokenType = tokenType; }

    public UUID getOrganizationID() {
        return organizationID;
    }

    public void setOrganizationID(UUID organizationID) {
        this.organizationID = organizationID;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TokenDto)) return false;
        TokenDto that = (TokenDto) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(tokenType, that.tokenType) &&
                Objects.equals(organizationID, that.organizationID) &&
                Objects.equals(label, that.label) &&
                Objects.equals(token, that.token) &&
                Objects.equals(createdAt, that.createdAt) &&
                Objects.equals(expiresAt, that.expiresAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, tokenType, organizationID, label, token, createdAt, expiresAt);
    }
}
