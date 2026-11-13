package gov.cms.dpc.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import gov.cms.dpc.attribution.client.config.AttributionBundleConfiguration;
import gov.cms.dpc.bluebutton.config.BBClientConfiguration;
import gov.cms.dpc.bluebutton.config.BlueButtonBundleConfiguration;
import gov.cms.dpc.common.hibernate.auth.IDPCAuthDatabase;
import gov.cms.dpc.common.hibernate.consent.IDPCConsentDatabase;
import gov.cms.dpc.common.hibernate.queue.IDPCQueueDatabase;
import gov.cms.dpc.fhir.configuration.DPCFHIRConfiguration;
import gov.cms.dpc.fhir.configuration.FHIRClientConfiguration;
import gov.cms.dpc.fhir.configuration.IDPCFHIRConfiguration;
import gov.cms.dpc.macaroons.config.TokenPolicy;
import gov.cms.dpc.oneecosystem.config.OneEcosystemBundleConfiguration;
import gov.cms.dpc.oneecosystem.config.OneEcosystemClientConfiguration;
import gov.cms.dpc.queue.config.DPCAwsQueueConfiguration;
import gov.cms.dpc.queue.config.DPCQueueConfig;
import io.dropwizard.client.JerseyClientConfiguration;
import io.dropwizard.core.Configuration;
import io.dropwizard.core.server.DefaultServerFactory;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.jetty.HttpConnectorFactory;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;

public class DPCAPIConfiguration extends Configuration implements IDPCQueueDatabase, DPCQueueConfig, IDPCAuthDatabase, IDPCConsentDatabase, IDPCFHIRConfiguration, BlueButtonBundleConfiguration, AttributionBundleConfiguration, OneEcosystemBundleConfiguration {

    @NotEmpty
    private String exportPath;
    @Valid
    @NotNull
    @JsonProperty
    private JerseyClientConfiguration httpClient = new JerseyClientConfiguration();

    @Valid
    @NotNull
    @JsonProperty("queuedb")
    private DataSourceFactory queueDatabase = new DataSourceFactory();

    @Valid
    @NotNull
    @JsonProperty("authdb")
    private DataSourceFactory authDatabase = new DataSourceFactory();

    @Valid
    @NotNull
    @JsonProperty("consentdb")
    private DataSourceFactory consentDatabase = new DataSourceFactory();

    @Override
    public DataSourceFactory getConsentDatabase() {
        return consentDatabase;
    }

    @Valid
    @NotNull
    @JsonProperty("bbclient")
    private BBClientConfiguration clientConfiguration = new BBClientConfiguration();

    @Valid
    @NotNull
    @JsonProperty("oneEcosystemClient")
    private OneEcosystemClientConfiguration oneEcosystemClientConfiguration = new OneEcosystemClientConfiguration();

    @Valid
    @NotNull
    @JsonProperty("attributionClient")
    private final FHIRClientConfiguration fhirClientConfiguration = new FHIRClientConfiguration();

    @Valid
    @NotNull
    @JsonProperty("attributionR4Client")
    private final FHIRClientConfiguration attributionClientConfiguration = new FHIRClientConfiguration();

    @NotEmpty
    @NotNull
    private String attributionHealthCheckURL;

    @Valid
    @NotNull
    @JsonProperty("fhir")
    private DPCFHIRConfiguration fhirConfig;

    private boolean authenticationDisabled;

    @NotEmpty
    private String publicURL;

    @Valid
    @NotNull
    @JsonProperty("tokens")
    private TokenPolicy tokenPolicy = new TokenPolicy();

    @NotEmpty
    private String keyPairLocation;

    @Min(0)
    private int jobTimeoutInSeconds;

    public TokenPolicy getTokenPolicy() {
        return tokenPolicy;
    }

    public void setTokenPolicy(TokenPolicy tokenPolicy) {
        this.tokenPolicy = tokenPolicy;
    }

    @Override
    public DataSourceFactory getQueueDatabase() {
        return this.queueDatabase;
    }

    @Override
    public DataSourceFactory getAuthDatabase() {
        return this.authDatabase;
    }

    private List<String> lookBackExemptOrgs;

    public DPCAPIConfiguration() {
        // Jackson required
    }

    public JerseyClientConfiguration getHttpClient() {
        return httpClient;
    }

    public void setHttpClient(JerseyClientConfiguration httpClient) {
        this.httpClient = httpClient;
    }

    public String getAttributionHealthCheckURL() {
        return attributionHealthCheckURL;
    }

    public String getExportPath() {
        return exportPath;
    }

    public void setExportPath(String exportPath) {
        this.exportPath = exportPath;
    }

    public String getKeyPairLocation() {
        return keyPairLocation;
    }

    public void setKeyPairLocation(String keyPairLocation) {
        this.keyPairLocation = keyPairLocation;
    }

    public boolean isAuthenticationDisabled() {
        return authenticationDisabled;
    }

    public void setAuthenticationDisabled(boolean authenticationDisabled) {
        this.authenticationDisabled = authenticationDisabled;
    }

    public String getPublicURL() {
        return publicURL;
    }
    public void setPublicURL(String publicURL) {
        this.publicURL = publicURL;
    }

    @Override
    public DPCFHIRConfiguration getFHIRConfiguration() {
        return this.fhirConfig;
    }

    @Override
    public void setFHIRConfiguration(DPCFHIRConfiguration config) {
        this.fhirConfig = config;
    }

    @Override
    public BBClientConfiguration getBlueButtonConfiguration() {
        return this.clientConfiguration;
    }

    @Override
    public OneEcosystemClientConfiguration getOneEcosystemClientConfiguration() {
        return this.oneEcosystemClientConfiguration;
    }

    public FHIRClientConfiguration getFhirClientConfiguration() {
        return this.fhirClientConfiguration;
    }

    @Override
    @JsonIgnore
    public FHIRClientConfiguration getAttributionClientConfiguration() {
        return this.attributionClientConfiguration;
    }

    public int getJobTimeoutInSeconds() {
        return jobTimeoutInSeconds;
    }

    public List<String> getLookBackExemptOrgs() {
        if(lookBackExemptOrgs == null){
            return new ArrayList<>();
        }
        return lookBackExemptOrgs; }

    public void setLookBackExemptOrgs(List<String> lookBackExemptOrgs) {
        this.lookBackExemptOrgs = lookBackExemptOrgs;
    }

    // Never used in dpc-api, but required for the queue service
    @JsonIgnore
    @Override
    public int getPollingFrequency() {
        throw new UnsupportedOperationException("getPollingFrequency() is not supported in dpc-api.");
    }

    // dpc-api isn't currently using an AWS queue.
    @Override
    public DPCAwsQueueConfiguration getDpcAwsQueueConfiguration() {
        return null;
    }

    public int getServicePort() {
        DefaultServerFactory serverFactory = (DefaultServerFactory) this.getServerFactory();
        HttpConnectorFactory connection = (HttpConnectorFactory) serverFactory.getApplicationConnectors().get(0);
        return connection.getPort();
    }

    public String getAppContextPath() {
        DefaultServerFactory serverFactory = (DefaultServerFactory) this.getServerFactory();
        return serverFactory.getApplicationContextPath();
    }
}
