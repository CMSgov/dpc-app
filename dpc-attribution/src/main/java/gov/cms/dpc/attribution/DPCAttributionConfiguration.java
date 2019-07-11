package gov.cms.dpc.attribution;

import ca.mestevens.java.configuration.TypesafeConfiguration;
import com.fasterxml.jackson.annotation.JsonProperty;
import gov.cms.dpc.attribution.config.TokenPolicy;
import gov.cms.dpc.common.hibernate.IDPCDatabase;
import gov.cms.dpc.fhir.configuration.DPCFHIRConfiguration;
import gov.cms.dpc.fhir.configuration.IDPCFHIRConfiguration;
import io.dropwizard.db.DataSourceFactory;
import org.hibernate.validator.constraints.NotEmpty;
import org.knowm.dropwizard.sundial.SundialConfiguration;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.time.Duration;

public class DPCAttributionConfiguration extends TypesafeConfiguration implements IDPCDatabase, IDPCFHIRConfiguration {

    @Valid
    private Duration expirationThreshold;

    private Boolean migrationEnabled;

    @Valid
    @NotNull
    @JsonProperty("database")
    private DataSourceFactory database = new DataSourceFactory();

    @Valid
    @NotNull
    @JsonProperty("sundial")
    private SundialConfiguration sundial = new SundialConfiguration();

    @Valid
    @NotNull
    @JsonProperty("tokens")
    private TokenPolicy tokenPolicy = new TokenPolicy();

    @NotEmpty
    private String publicServerURL;

    @Valid
    @NotNull
    @JsonProperty("fhir")
    private DPCFHIRConfiguration fhirConfig;

    @Override
    public DataSourceFactory getDatabase() {
        return database;
    }

    public SundialConfiguration getSundial() {
        return sundial;
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

    public TokenPolicy getTokenPolicy() {
        return tokenPolicy;
    }

    public void setTokenPolicy(TokenPolicy tokenPolicy) {
        this.tokenPolicy = tokenPolicy;
    }

    @Override
    public DPCFHIRConfiguration getFHIRConfiguration() {
        return this.fhirConfig;
    }

    @Override
    public void setFHIRConfiguration(DPCFHIRConfiguration config) {
        this.fhirConfig = config;
    }
    public Boolean getMigrationEnabled() {
        return migrationEnabled;
    }

    public void setMigrationEnabled(Boolean migrationEnabled) {
        this.migrationEnabled = migrationEnabled;
    }
}
