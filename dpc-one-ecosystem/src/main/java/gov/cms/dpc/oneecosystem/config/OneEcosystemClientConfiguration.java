package gov.cms.dpc.oneecosystem.config;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OneEcosystemClientConfiguration {

    @JsonProperty
    private boolean useMock = true;
    @JsonProperty
    private String apiBaseUrl;
    @JsonProperty
    private String clientId;
    @JsonProperty
    private String clientSecret;

    public boolean isUseMock() {
        return useMock;
    }

    public String getApiBaseUrl() {
        return apiBaseUrl;
    }

    public String getClientId() {
        return clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }
}
