package gov.cms.dpc.attribution;

import com.fasterxml.jackson.annotation.JsonProperty;
import gov.cms.dpc.common.hibernate.attribution.IDPCDatabase;
import gov.cms.dpc.fhir.configuration.DPCFHIRConfiguration;
import gov.cms.dpc.fhir.configuration.IDPCFHIRConfiguration;
import io.dropwizard.db.DataSourceFactory;
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration;
import io.dropwizard.jobs.JobConfiguration;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.time.Duration;
import java.util.List;

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

    @NotEmpty
    private String fhirReferenceURL;

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

    public String getFhirReferenceURL() {
        return fhirReferenceURL;
    }
}
