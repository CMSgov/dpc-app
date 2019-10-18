package gov.cms.dpc.consent;

import ca.mestevens.java.configuration.TypesafeConfiguration;
import com.fasterxml.jackson.annotation.JsonProperty;
import gov.cms.dpc.common.hibernate.attribution.IDPCDatabase;
import io.dropwizard.db.DataSourceFactory;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public class DPCConsentConfiguration extends TypesafeConfiguration implements IDPCDatabase {

    @Valid
    @NotNull
    @JsonProperty("database")
    private DataSourceFactory database = new DataSourceFactory();

    @Override
    public DataSourceFactory getDatabase() {
        return database;
    }
}
