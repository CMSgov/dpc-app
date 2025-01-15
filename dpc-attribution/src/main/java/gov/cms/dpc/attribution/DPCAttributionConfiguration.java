package gov.cms.dpc.attribution;

import com.fasterxml.jackson.annotation.JsonProperty;
import gov.cms.dpc.common.hibernate.attribution.IDPCDatabase;
import gov.cms.dpc.fhir.configuration.DPCFHIRConfiguration;
import gov.cms.dpc.fhir.configuration.IDPCFHIRConfiguration;
import io.dropwizard.core.server.DefaultServerFactory;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.jetty.HttpConnectorFactory;
import io.dropwizard.jobs.JobConfiguration;
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.time.Duration;
import java.util.List;
import java.util.Map;

public class DPCAttributionConfiguration extends JobConfiguration implements IDPCDatabase, IDPCFHIRConfiguration {

    @Valid
    private Duration expirationThreshold;

    private Boolean migrationEnabled;

    @Valid
    @NotNull
    @JsonProperty("database")
    private DataSourceFactory database = new DataSourceFactory();

    @NotEmpty
    private String publicServerURL;

    @Valid
    @NotNull
    @JsonProperty("fhir")
    private DPCFHIRConfiguration fhirConfig;

    @JsonProperty("swagger")
    private SwaggerBundleConfiguration swaggerBundleConfiguration;

    @Min(-1)
    private Integer providerLimit;

    @Min(-1)
    private Integer patientLimit;

    private List<String> lookBackExemptOrgs;

    @Override
    public DataSourceFactory getDatabase() {
        return database;
    }

    public Duration getExpirationThreshold() {
        return expirationThreshold;
    }

    public void setExpirationThreshold(int expirationThreshold) {
        this.expirationThreshold = Duration.ofDays(expirationThreshold);
    }

    public String getPublicServerURL() {
        return publicServerURL;
    }

    public void setPublicServerURL(String publicServerURL) {
        this.publicServerURL = publicServerURL;
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

    public Boolean getMigrationEnabled() {
        return migrationEnabled;
    }

    public void setMigrationEnabled(Boolean migrationEnabled) {
        this.migrationEnabled = migrationEnabled;
    }

    public Integer getProviderLimit() {
        return providerLimit;
    }

    public void setProviderLimit(Integer providerLimit) {
        this.providerLimit = providerLimit;
    }

    public Integer getPatientLimit() {
        return patientLimit;
    }

    public void setPatientLimit(Integer patientLimit) {
        this.patientLimit = patientLimit;
    }

    public List<String> getLookBackExemptOrgs() { return lookBackExemptOrgs; }

    public void setLookBackExemptOrgs(List<String> lookBackExemptOrgs) {
        this.lookBackExemptOrgs = lookBackExemptOrgs;
    }

    public int getServicePort() {
        DefaultServerFactory serverFactory = (DefaultServerFactory) this.getServerFactory();
        HttpConnectorFactory connection = (HttpConnectorFactory) serverFactory.getApplicationConnectors().get(0);
        return connection.getPort();
    }

    public int getQueryChunkSize() {
        Map<String,String> properties = database.getProperties();
        return Integer.parseInt(properties.get("queryChunkSize"));
    }
}
