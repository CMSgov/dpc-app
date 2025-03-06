package gov.cms.dpc.aggregation.engine;

import java.time.YearMonth;
import java.util.List;

/**
 * Holds configuration information for the operations of {@link gov.cms.dpc.aggregation.engine.AggregationEngine}.
 */
public class OperationsConfig {

    private final int retryCount;
    private final int resourcesPerFileCount;
    private final String exportPath;
    private final int pollingFrequency;
    private int lookBackMonths;
    private final YearMonth lookBackDate;
    private List<String> lookBackExemptOrgs;
    private int fetchWarnThresholdSeconds;

    public OperationsConfig(
            int resourcesPerFileCount,
            String exportPath,
            int retryCount,
            int pollingFrequency,
            int lookBackMonths,
            YearMonth lookBackDate,
            List<String> lookBackExemptOrgs,
            int fetchWarnThresholdSeconds
    ) {
        this.retryCount = retryCount;
        this.resourcesPerFileCount = resourcesPerFileCount;
        this.exportPath = exportPath;
        this.pollingFrequency = pollingFrequency;
        this.lookBackMonths = lookBackMonths;
        this.lookBackDate = lookBackDate;
        this.lookBackExemptOrgs = lookBackExemptOrgs;
        this.fetchWarnThresholdSeconds = fetchWarnThresholdSeconds;
    }

    public OperationsConfig(
            int resourcesPerFileCount,
            String exportPath,
            int pollingFrequency,
            YearMonth lookBackDate
    ) {
        this.retryCount = 3;
        this.resourcesPerFileCount = resourcesPerFileCount;
        this.exportPath = exportPath;
        this.pollingFrequency = pollingFrequency;
        this.lookBackDate = lookBackDate;
        this.fetchWarnThresholdSeconds = 30;
    }

    @SuppressWarnings("unused")
    public int getRetryCount() {
        return retryCount;
    }

    public int getResourcesPerFileCount() {
        return resourcesPerFileCount;
    }

    public String getExportPath() {
        return exportPath;
    }

    public int getPollingFrequency() {
        return pollingFrequency;
    }

    public int getLookBackMonths() {
        return lookBackMonths;
    }

    public YearMonth getLookBackDate() {
        return lookBackDate;
    }

    public List<String> getLookBackExemptOrgs() { return lookBackExemptOrgs; }

    public int getFetchWarnThresholdSeconds() { return fetchWarnThresholdSeconds; }
}
