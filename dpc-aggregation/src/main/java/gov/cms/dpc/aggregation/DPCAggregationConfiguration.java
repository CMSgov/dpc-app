package gov.cms.dpc.aggregation;

import ca.mestevens.java.configuration.TypesafeConfiguration;
import com.fasterxml.jackson.annotation.JsonProperty;
import gov.cms.dpc.common.hibernate.IDPCDatabase;
import io.dropwizard.db.DataSourceFactory;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public class DPCAggregationConfiguration extends TypesafeConfiguration implements IDPCDatabase {

    @Valid
    @NotNull
    @JsonProperty("database")
    private DataSourceFactory database = new DataSourceFactory();


    @Override
    public DataSourceFactory getDatabase() {
        return this.database;
    }
}
