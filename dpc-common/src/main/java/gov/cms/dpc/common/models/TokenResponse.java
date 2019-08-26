package gov.cms.dpc.common.models;

import gov.cms.dpc.common.entities.TokenEntity;

import java.io.Serializable;
import java.util.Objects;

public class TokenResponse implements Serializable {

    public static final long serialVersionUID = 42L;

    private String id;
    private TokenEntity.TokenType type;
    private String expires;

    TokenResponse() {
        // Jackson required
    }

    public TokenResponse(String id, TokenEntity.TokenType type, String expires) {
        this.id = id;
        this.type = type;
        this.expires = expires;
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

    public String getExpires() {
        return expires;
    }

    public void setExpires(String expires) {
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
