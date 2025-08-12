package gov.cms.dpc.aggregation;

import com.fasterxml.jackson.annotation.JsonProperty;
import gov.cms.dpc.bluebutton.config.BBClientConfiguration;
import gov.cms.dpc.bluebutton.config.BlueButtonBundleConfiguration;
import gov.cms.dpc.common.hibernate.attribution.IDPCDatabase;
import gov.cms.dpc.common.hibernate.queue.IDPCQueueDatabase;
import gov.cms.dpc.fhir.configuration.FHIRClientConfiguration;
import gov.cms.dpc.queue.config.DPCAwsQueueConfiguration;
import gov.cms.dpc.queue.config.DPCQueueConfig;
import io.dropwizard.core.Configuration;
import io.dropwizard.db.DataSourceFactory;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;

public class DPCAggregationConfiguration extends Configuration implements BlueButtonBundleConfiguration, IDPCDatabase, IDPCQueueDatabase, DPCQueueConfig {

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
    @JsonProperty("consentdb")
    private DataSourceFactory consentDatabase = new DataSourceFactory();

    @Valid
    @NotNull
    @JsonProperty("bbclient")
    private final BBClientConfiguration clientConfiguration = new BBClientConfiguration();

    @Valid
    @NotNull
    @JsonProperty("consentClient")
    private final FHIRClientConfiguration consentClientConfiguration = new FHIRClientConfiguration();

    @NotNull
    @JsonProperty("awsQueue")
    private final DPCAwsQueueConfiguration dpcAwsQueueConfiguration = new DPCAwsQueueConfiguration();

    @NotEmpty
    @NotNull
    private String consentHealthCheckURL;

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

    // If a resource fetch takes longer than this, it'll get logged as a warning
    @Min(0)
    private int fetchWarnThresholdSeconds;

    @NotNull
    private final YearMonth lookBackDate = YearMonth.now(ZoneId.systemDefault());

    @NotEmpty
    private String fhirReferenceURL;

    @Override
    public DataSourceFactory getDatabase() {
        return this.database;
    }

    @Override
    public DataSourceFactory getQueueDatabase() {
        return queueDatabase;
    }

    public DataSourceFactory getConsentDatabase() {
        return consentDatabase;
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

    public FHIRClientConfiguration getConsentClientConfiguration() { return this.consentClientConfiguration; }

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

    public String getConsentHealthCheckURL() { return consentHealthCheckURL; }

    @SuppressWarnings("unused")
    public void setLookBackExemptOrgs(List<String> lookBackExemptOrgs) {
        this.lookBackExemptOrgs = lookBackExemptOrgs;
    }

    @Override
    public DPCAwsQueueConfiguration getDpcAwsQueueConfiguration() { return this.dpcAwsQueueConfiguration; }

    public int getFetchWarnThresholdSeconds() {
        return fetchWarnThresholdSeconds;
    }

    public String getFhirReferenceURL() {
        return fhirReferenceURL;
    }

    public void setFhirReferenceURL(String fhirReferenceURL) {
        this.fhirReferenceURL = fhirReferenceURL;
    }
}
