package gov.cms.dpc.api;

import ca.mestevens.java.configuration.TypesafeConfiguration;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.typesafe.config.ConfigRenderOptions;
import gov.cms.dpc.common.hibernate.IDPCDatabase;
import gov.cms.dpc.queue.DPCQueueConfig;
import io.dropwizard.client.JerseyClientConfiguration;
import io.dropwizard.db.DataSourceFactory;
import org.hibernate.validator.constraints.NotEmpty;
import org.redisson.config.Config;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.io.IOException;

public class DPCAPIConfiguration extends TypesafeConfiguration implements IDPCDatabase, DPCQueueConfig {

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

    @NotEmpty
    @NotNull
    private String attributionURL;

    public DataSourceFactory getDatabase() {
        return database;
    }

    DPCAPIConfiguration() {
//        Not used;
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
