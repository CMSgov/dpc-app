package gov.cms.dpc.api.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import gov.cms.dpc.common.converters.jackson.DurationToSecondsConverter;

import java.io.Serializable;
import java.time.Duration;

public class JWTAuthResponse implements Serializable {

    public static final long serialVersionUID = 42L;

    @JsonProperty(value = "access_token")
    private byte[] accessToken;
    @JsonProperty(value = "token_type")
    private String tokenType = "bearer";
    @JsonProperty(value = "expires_in")
    @JsonSerialize(converter = DurationToSecondsConverter.class)
    private Duration expiresIn;
    private String scope;

    public JWTAuthResponse() {
        // Jackson required
    }

    public byte[] getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(byte[] accessToken) {
        this.accessToken = accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public Duration getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(Duration expiresIn) {
        this.expiresIn = expiresIn;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }
}
