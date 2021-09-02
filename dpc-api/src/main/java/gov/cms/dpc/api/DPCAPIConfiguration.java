package gov.cms.dpc.api;

import ca.mestevens.java.configuration.TypesafeConfiguration;
import com.fasterxml.jackson.annotation.JsonProperty;
import gov.cms.dpc.bluebutton.config.BBClientConfiguration;
import gov.cms.dpc.bluebutton.config.BlueButtonBundleConfiguration;
import gov.cms.dpc.common.hibernate.attribution.IDPCDatabase;
import gov.cms.dpc.common.hibernate.auth.IDPCAuthDatabase;
import gov.cms.dpc.common.hibernate.queue.IDPCQueueDatabase;
import gov.cms.dpc.fhir.configuration.DPCFHIRConfiguration;
import gov.cms.dpc.fhir.configuration.IDPCFHIRConfiguration;
import gov.cms.dpc.macaroons.config.TokenPolicy;
import io.dropwizard.client.JerseyClientConfiguration;
import io.dropwizard.db.DataSourceFactory;
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.LinkedList;
import java.util.List;

public class DPCAPIConfiguration extends TypesafeConfiguration implements IDPCDatabase, IDPCQueueDatabase, IDPCAuthDatabase, IDPCFHIRConfiguration, BlueButtonBundleConfiguration {

    @NotEmpty
    private String exportPath;
    @Valid
    @NotNull
    @JsonProperty
    private JerseyClientConfiguration httpClient = new JerseyClientConfiguration();

    @Valid
    @NotNull
    @JsonProperty("database")
    private DataSourceFactory database = new DataSourceFactory();

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
    @JsonProperty("bbclient")
    private BBClientConfiguration clientConfiguration = new BBClientConfiguration();

    @NotEmpty
    @NotNull
    private String attributionURL;

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

    @Valid
    @JsonProperty("swagger")
    private SwaggerBundleConfiguration swaggerBundleConfiguration;

    @Override
    public DataSourceFactory getDatabase() {
        return database;
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

    public String getAttributionURL() {
        return attributionURL;
    }

    public void setAttributionURL(String attributionURL) {
        this.attributionURL = attributionURL;
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

    public SwaggerBundleConfiguration getSwaggerBundleConfiguration() {
        return swaggerBundleConfiguration;
    }

    public void setSwaggerBundleConfiguration(SwaggerBundleConfiguration swaggerBundleConfiguration) {
        this.swaggerBundleConfiguration = swaggerBundleConfiguration;
    }

    public int getJobTimeoutInSeconds() {
        return jobTimeoutInSeconds;
    }

    public List<String> getLookBackExemptOrgs() {
        if(lookBackExemptOrgs == null){
            return new LinkedList<>();
        }
        return lookBackExemptOrgs; }

    public void setLookBackExemptOrgs(List<String> lookBackExemptOrgs) {
        this.lookBackExemptOrgs = lookBackExemptOrgs;
    }
}
