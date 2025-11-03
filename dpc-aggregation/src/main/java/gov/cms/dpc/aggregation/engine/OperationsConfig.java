package gov.cms.dpc.aggregation.engine;

import java.time.YearMonth;
import java.time.ZoneId;
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
    private List<String> lookBackExemptOrgs;
    private final int fetchWarnThresholdSeconds;

    public OperationsConfig(
            int resourcesPerFileCount,
            String exportPath,
            int retryCount,
            int pollingFrequency,
            int lookBackMonths,
            List<String> lookBackExemptOrgs,
            int fetchWarnThresholdSeconds
    ) {
        this.retryCount = retryCount;
        this.resourcesPerFileCount = resourcesPerFileCount;
        this.exportPath = exportPath;
        this.pollingFrequency = pollingFrequency;
        this.lookBackMonths = lookBackMonths;
        this.lookBackExemptOrgs = lookBackExemptOrgs;
        this.fetchWarnThresholdSeconds = fetchWarnThresholdSeconds;
    }

    // This constructor is only used in testing, so set sensible defaults for members that aren't provided.
    public OperationsConfig(
            int resourcesPerFileCount,
            String exportPath,
            int pollingFrequency
    ) {
        this.retryCount = 3;
        this.resourcesPerFileCount = resourcesPerFileCount;
        this.exportPath = exportPath;
        this.pollingFrequency = pollingFrequency;
        this.fetchWarnThresholdSeconds = 30;
        this.lookBackMonths = 18;
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
        return YearMonth.now(ZoneId.systemDefault());
    }

    public List<String> getLookBackExemptOrgs() { return lookBackExemptOrgs; }

    public int getFetchWarnThresholdSeconds() { return fetchWarnThresholdSeconds; }
}
