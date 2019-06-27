package gov.cms.dpc.aggregation;

import ca.mestevens.java.configuration.TypesafeConfiguration;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.typesafe.config.ConfigRenderOptions;
import gov.cms.dpc.aggregation.engine.OperationsConfig;
import gov.cms.dpc.bluebutton.config.BBClientConfiguration;
import gov.cms.dpc.bluebutton.config.BlueButtonBundleConfiguration;
import gov.cms.dpc.common.hibernate.IDPCDatabase;
import gov.cms.dpc.queue.DPCQueueConfig;
import io.dropwizard.db.DataSourceFactory;
import org.hibernate.validator.constraints.NotEmpty;
import org.redisson.config.Config;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.io.IOException;

public class DPCAggregationConfiguration extends TypesafeConfiguration implements BlueButtonBundleConfiguration, IDPCDatabase, DPCQueueConfig {

    @Valid
    @NotNull
    @JsonProperty("database")
    private DataSourceFactory database = new DataSourceFactory();

    @Valid
    @NotNull
    @JsonProperty("bbclient")
    private BBClientConfiguration clientConfiguration = new BBClientConfiguration();

    @NotEmpty
    private String exportPath;

    @Min(1)
    @Max(5)
    private int retryCount = 3;

    @Min(10)
    private int resourcesPerFileCount = 5000;

    private boolean parallelRequestsEnabled = false;

    private boolean encryptionEnabled = false;

    @Override
    public DataSourceFactory getDatabase() {
        return this.database;
    }

    public String getExportPath() {
        return exportPath;
    }

    public void setExportPath(String exportPath) {
        this.exportPath = exportPath;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public boolean isEncryptionEnabled() {
        return encryptionEnabled;
    }

    public int getResourcesPerFileCount() {
        return resourcesPerFileCount;
    }

    public boolean isParallelRequestsEnabled() {
        return parallelRequestsEnabled;
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

    @Override
    public BBClientConfiguration getBlueButtonConfiguration() {
        return this.clientConfiguration;
    }

    public OperationsConfig getOperationsConfig() {
        return new OperationsConfig(retryCount, resourcesPerFileCount, parallelRequestsEnabled, exportPath, encryptionEnabled);
    }
}
