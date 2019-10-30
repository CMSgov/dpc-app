package gov.cms.dpc.consent;

import ca.mestevens.java.configuration.TypesafeConfiguration;
import com.fasterxml.jackson.annotation.JsonProperty;
import gov.cms.dpc.common.hibernate.attribution.IDPCDatabase;
import gov.cms.dpc.common.hibernate.consent.IDPCConsentDatabase;
import io.dropwizard.db.DataSourceFactory;
import org.hibernate.validator.constraints.NotEmpty;
import org.knowm.dropwizard.sundial.SundialConfiguration;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public class DPCConsentConfiguration extends TypesafeConfiguration implements IDPCDatabase, IDPCConsentDatabase {

    @Valid
    @NotNull
    @JsonProperty("database")
    private DataSourceFactory database = new DataSourceFactory();

    @Valid
    @NotNull
    @JsonProperty("consentdb")
    private DataSourceFactory consentDatabase = new DataSourceFactory();

    @Valid
    @NotNull
    @JsonProperty("sundial")
    private SundialConfiguration sundial = new SundialConfiguration();

    @NotEmpty
    private String suppressionFileDir;

    @Override
    public DataSourceFactory getDatabase() {
        return database;
    }

    @Override
    public DataSourceFactory getConsentDatabase() {
        return database;
    }

    public SundialConfiguration getSundial() {
        return sundial;
    }

    public String getSuppressionFileDir() { return suppressionFileDir; }

    public void setSuppressionFileDir(String suppressionFileDir) { this.suppressionFileDir = suppressionFileDir; }
}
