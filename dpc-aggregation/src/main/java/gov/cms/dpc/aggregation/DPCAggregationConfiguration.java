package gov.cms.dpc.aggregation;

import ca.mestevens.java.configuration.TypesafeConfiguration;
import com.fasterxml.jackson.annotation.JsonProperty;
import gov.cms.dpc.bluebutton.config.BBClientConfiguration;
import gov.cms.dpc.bluebutton.config.BlueButtonBundleConfiguration;
import gov.cms.dpc.common.hibernate.IDPCDatabase;
import gov.cms.dpc.queue.DPCQueueConfig;
import io.dropwizard.db.DataSourceFactory;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

public class DPCAggregationConfiguration extends TypesafeConfiguration implements BlueButtonBundleConfiguration, IDPCDatabase, DPCQueueConfig {

    @Valid
    @NotNull
    @JsonProperty("database")
    private DataSourceFactory database = new DataSourceFactory();

    @Valid
    @NotNull
    @JsonProperty("bbclient")
    private BBClientConfiguration clientConfiguration = new BBClientConfiguration();

    // The path to the folder that will contain the output files
    @NotEmpty
    private String exportPath;

    // The number of retries per request to Blue Button
    @Min(1)
    @Max(5)
    private int retryCount = 3;

    // The max number of resources that we will place into a single file
    @Min(10)
    @Max(100000) // Keep files under a GB
    private int resourcesPerFileCount = 10000;

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

    public int getResourcesPerFileCount() {
        return resourcesPerFileCount;
    }

    @Override
    public BBClientConfiguration getBlueButtonConfiguration() {
        return this.clientConfiguration;
    }
}
