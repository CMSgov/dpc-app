package gov.cms.dpc.attribution;

import ca.mestevens.java.configuration.TypesafeConfiguration;
import com.fasterxml.jackson.annotation.JsonProperty;
import gov.cms.dpc.common.hibernate.IDPCDatabase;
import io.dropwizard.db.DataSourceFactory;
import org.knowm.dropwizard.sundial.SundialConfiguration;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.time.Duration;

public class DPCAttributionConfiguration extends TypesafeConfiguration implements IDPCDatabase {

    @Valid
    private Duration expirationThreshold;

    @Valid
    @NotNull
    @JsonProperty("database")
    private DataSourceFactory database = new DataSourceFactory();

    @Valid
    @NotNull
    @JsonProperty("sundial")
    private SundialConfiguration sundial = new SundialConfiguration();

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
}
