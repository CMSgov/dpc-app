package gov.cms.dpc.aggregation;

import ca.mestevens.java.configuration.TypesafeConfiguration;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.typesafe.config.ConfigRenderOptions;
import gov.cms.dpc.common.hibernate.IDPCDatabase;
import gov.cms.dpc.queue.DPCQueueConfig;
import io.dropwizard.db.DataSourceFactory;
import org.redisson.config.Config;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.io.IOException;

public class DPCAggregationConfiguration extends TypesafeConfiguration implements IDPCDatabase, DPCQueueConfig {

    @Valid
    @NotNull
    @JsonProperty("database")
    private DataSourceFactory database = new DataSourceFactory();


    @Override
    public DataSourceFactory getDatabase() {
        return this.database;
    }

    @Override
    public Config getQueueConfig() {
        final String configString = getConfig().getConfig("queue").root().render(ConfigRenderOptions.concise());
        try {
            return Config.fromJSON(configString);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read queue config.", e);
        }
    }
}
