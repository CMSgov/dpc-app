package gov.cms.dpc.api;

import ca.mestevens.java.configuration.TypesafeConfiguration;
import com.fasterxml.jackson.annotation.JsonProperty;
import gov.cms.dpc.common.hibernate.IDPCAuthDatabase;
import gov.cms.dpc.common.hibernate.IDPCDatabase;
import gov.cms.dpc.common.hibernate.IDPCQueueDatabase;
import gov.cms.dpc.fhir.configuration.DPCFHIRConfiguration;
import gov.cms.dpc.fhir.configuration.IDPCFHIRConfiguration;
import io.dropwizard.client.JerseyClientConfiguration;
import io.dropwizard.db.DataSourceFactory;
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public class DPCAPIConfiguration extends TypesafeConfiguration implements IDPCDatabase, IDPCQueueDatabase, IDPCAuthDatabase, IDPCFHIRConfiguration {

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

    @NotEmpty
    @NotNull
    private String attributionURL;

    @Valid
    @NotNull
    @JsonProperty("fhir")
    private DPCFHIRConfiguration fhirConfig;

    private boolean authenticationDisabled;

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

    public boolean isAuthenticationDisabled() {
        return authenticationDisabled;
    }

    public void setAuthenticationDisabled(boolean authenticationDisabled) {
        this.authenticationDisabled = authenticationDisabled;
    }

    @Override
    public DPCFHIRConfiguration getFHIRConfiguration() {
        return this.fhirConfig;
    }

    @Override
    public void setFHIRConfiguration(DPCFHIRConfiguration config) {
        this.fhirConfig = config;
    }

    public SwaggerBundleConfiguration getSwaggerBundleConfiguration() {
        return swaggerBundleConfiguration;
    }

    public void setSwaggerBundleConfiguration(SwaggerBundleConfiguration swaggerBundleConfiguration) {
        this.swaggerBundleConfiguration = swaggerBundleConfiguration;
    }
}
