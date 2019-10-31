package gov.cms.dpc.api.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import gov.cms.dpc.api.entities.TokenEntity;
import gov.cms.dpc.common.converters.jackson.OffsetDateTimeToStringConverter;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Objects;

public class TokenResponse implements Serializable {

    public static final long serialVersionUID = 42L;

    private String id;
    private TokenEntity.TokenType type;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String token;
    @JsonSerialize(converter = OffsetDateTimeToStringConverter.class)
    private OffsetDateTime created;
    @JsonSerialize(converter = OffsetDateTimeToStringConverter.class)
    private OffsetDateTime expires;

    TokenResponse() {
        // Jackson required
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public TokenEntity.TokenType getType() {
        return type;
    }

    public void setType(TokenEntity.TokenType type) {
        this.type = type;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public OffsetDateTime getCreated() {
        return created;
    }

    public void setCreated(OffsetDateTime created) {
        this.created = created;
    }

    public OffsetDateTime getExpires() {
        return expires;
    }

    public void setExpires(OffsetDateTime expires) {
        this.expires = expires;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TokenResponse)) return false;
        TokenResponse that = (TokenResponse) o;
        return Objects.equals(id, that.id) &&
                type == that.type &&
                Objects.equals(expires, that.expires);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, type, expires);
    }
}
