package gov.cms.dpc.oneecosystem.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.net.URI;

public class OneEcosystemClientConfiguration {

    @JsonProperty
    private boolean useMock = true;
    @JsonProperty
    private URI apiBaseUrl;
    @JsonProperty
    private String clientId;
    @JsonProperty
    private String clientSecret;

    public boolean isUseMock() {
        return useMock;
    }

    public URI getApiBaseUrl() {
        return apiBaseUrl;
    }

    public String getClientId() {
        return clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }
}
