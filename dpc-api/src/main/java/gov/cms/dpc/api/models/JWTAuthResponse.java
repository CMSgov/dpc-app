package gov.cms.dpc.api.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.github.nitram509.jmacaroons.Macaroon;
import gov.cms.dpc.api.converters.MacaroonListToJSONConverter;
import gov.cms.dpc.common.converters.jackson.DurationToSecondsConverter;

import java.io.Serializable;
import java.time.Duration;
import java.util.List;

public class JWTAuthResponse implements Serializable {

    public static final long serialVersionUID = 42L;

    @JsonProperty(value = "access_token")
    @JsonSerialize(converter = MacaroonListToJSONConverter.class)
    private List<Macaroon> dischargedMacaroons;
    @JsonProperty(value = "token_type")
    private String tokenType = "bearer";
    @JsonProperty(value = "expires_in")
    @JsonSerialize(converter = DurationToSecondsConverter.class)
    private Duration expiresIn;
    private String scope;

    public JWTAuthResponse() {
        // Jackson required
    }

    public List<Macaroon> getDischargedMacaroons() {
        return dischargedMacaroons;
    }

    public void setDischargedMacaroons(List<Macaroon> dischargedMacaroons) {
        this.dischargedMacaroons = dischargedMacaroons;
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
