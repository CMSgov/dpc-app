package gov.cms.dpc.api.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import gov.cms.dpc.common.converters.jackson.DurationToSecondsConverter;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.Duration;

public class JWTAuthResponse implements Serializable {

    public static final long serialVersionUID = 42L;

    @NotNull
    @JsonProperty(value = "access_token")
    private String dischargedMacaroons;
    @NotNull
    @JsonProperty(value = "token_type")
    private String tokenType = "bearer";
    @NotNull
    @JsonProperty(value = "expires_in")
    @JsonSerialize(converter = DurationToSecondsConverter.class)
    @ApiModelProperty(value = "Token expiration (in seconds)", dataType = "String")
    private Duration expiresIn;
    @NotEmpty
    private String scope;

    public JWTAuthResponse() {
        // Jackson required
    }

    public String getDischargedMacaroons() {
        return dischargedMacaroons;
    }

    public void setDischargedMacaroons(String dischargedMacaroons) {
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
