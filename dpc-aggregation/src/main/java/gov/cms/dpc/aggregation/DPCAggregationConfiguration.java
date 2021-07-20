package gov.cms.dpc.aggregation;

import ca.mestevens.java.configuration.TypesafeConfiguration;
import com.fasterxml.jackson.annotation.JsonProperty;
import gov.cms.dpc.bluebutton.config.BBClientConfiguration;
import gov.cms.dpc.bluebutton.config.BlueButtonBundleConfiguration;
import gov.cms.dpc.common.hibernate.attribution.IDPCDatabase;
import gov.cms.dpc.common.hibernate.queue.IDPCQueueDatabase;
import gov.cms.dpc.queue.DPCQueueConfig;
import io.dropwizard.db.DataSourceFactory;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;

public class DPCAggregationConfiguration extends TypesafeConfiguration implements BlueButtonBundleConfiguration, IDPCDatabase, IDPCQueueDatabase, DPCQueueConfig {

    @Valid
    @NotNull
    @JsonProperty("database")
    private final DataSourceFactory database = new DataSourceFactory();

    @Valid
    @NotNull
    @JsonProperty("queuedb")
    private final DataSourceFactory queueDatabase = new DataSourceFactory();

    @Valid
    @NotNull
    @JsonProperty("bbclient")
    private final BBClientConfiguration clientConfiguration = new BBClientConfiguration();

    @Valid
    @NotNull
    @JsonProperty("bbclientR4")
    private final BBClientConfiguration clientConfigurationV2 = new BBClientConfiguration();

    @Valid
    @NotNull
    @JsonProperty("consentServiceUrl")
    private String consentServiceUrl;

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
    private final int resourcesPerFileCount = 10000;

    // How often in milliseconds to check the queue for new batches
    @Min(50)
    private final int pollingFrequency = 500;

    @Min(1)
    private final int jobTimeoutInSeconds = 5;

    @Min(-1)
    @SuppressWarnings("unused")
    private int lookBackMonths;

    private List<String> lookBackExemptOrgs;

    @NotNull
    private final YearMonth lookBackDate = YearMonth.now(ZoneId.systemDefault());

    @Override
    public DataSourceFactory getDatabase() {
        return this.database;
    }

    @Override
    public DataSourceFactory getQueueDatabase() {
        return queueDatabase;
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

    @SuppressWarnings("unused")
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

    @Override
    public int getPollingFrequency() {
        return pollingFrequency;
    }

    public int getJobTimeoutInSeconds() {
        return jobTimeoutInSeconds;
    }

    public int getLookBackMonths() {
        return lookBackMonths;
    }

    public YearMonth getLookBackDate() {
        return lookBackDate;
    }

    public List<String> getLookBackExemptOrgs() {
        return lookBackExemptOrgs;
    }

    public String getConsentServiceUrl() {
        return consentServiceUrl;
    }

    @SuppressWarnings("unused")
    public void setLookBackExemptOrgs(List<String> lookBackExemptOrgs) {
        this.lookBackExemptOrgs = lookBackExemptOrgs;
    }
}
