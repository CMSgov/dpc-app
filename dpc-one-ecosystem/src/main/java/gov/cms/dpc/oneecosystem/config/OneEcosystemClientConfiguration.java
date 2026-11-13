package gov.cms.dpc.oneecosystem.config;

import com.fasterxml.jackson.annotation.JsonMerge;
import com.fasterxml.jackson.annotation.JsonProperty;
import gov.cms.dpc.fhir.configuration.ConnectionPoolConfiguration;
import gov.cms.dpc.fhir.configuration.FHIRClientConfiguration;

import java.net.URI;

import static org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager.DEFAULT_MAX_CONNECTIONS_PER_ROUTE;
import static org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager.DEFAULT_MAX_TOTAL_CONNECTIONS;

public class OneEcosystemClientConfiguration extends FHIRClientConfiguration {

    @JsonProperty
    private boolean useMock = true;
    @JsonProperty
    private String clientId;
    @JsonProperty
    private String clientSecret;
    @JsonProperty
    private long tokenRefreshBuffer;

    public OneEcosystemClientConfiguration() {
        setServerBaseUrl("dev.bcda.cms.gov");
        getConnectionPoolConfiguration().setPoolMaxTotal(DEFAULT_MAX_TOTAL_CONNECTIONS);
        getConnectionPoolConfiguration().setPoolMaxPerRoute(DEFAULT_MAX_CONNECTIONS_PER_ROUTE);
    }

    public URI apiBaseUri() {
        return URI.create("https://" + getServerBaseUrl());
    }

    @Override
    @JsonMerge
    public ConnectionPoolConfiguration getConnectionPoolConfiguration() {
        return super.getConnectionPoolConfiguration();
    }

    public boolean isUseMock() {
        return useMock;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public long getTokenRefreshBuffer() {
        return tokenRefreshBuffer;
    }

    public void setTokenRefreshBuffer(long tokenRefreshBuffer) {
        this.tokenRefreshBuffer = tokenRefreshBuffer;
    }
}
