package gov.cms.dpc.aggregation.engine;

import java.util.Date;

/**
 * Holds configuration information for the operations of {@link gov.cms.dpc.aggregation.engine.AggregationEngine}.
 */
public class OperationsConfig {

    private int retryCount;
    private int resourcesPerFileCount;
    private String exportPath;
    private int pollingFrequency;
    private int lookBackMonths;
    private Date lookBackDate;

    public OperationsConfig(
            int resourcesPerFileCount,
            String exportPath,
            int retryCount,
            int pollingFrequency,
            int lookBackMonths,
            Date lookBackDate
    ) {
        this.retryCount = retryCount;
        this.resourcesPerFileCount = resourcesPerFileCount;
        this.exportPath = exportPath;
        this.pollingFrequency = pollingFrequency;
        this.lookBackMonths = lookBackMonths;
        this.lookBackDate = lookBackDate;
    }

    public OperationsConfig(
            int resourcesPerFileCount,
            String exportPath,
            int pollingFrequency,
            Date lookBackDate
    ) {
        this.retryCount = 3;
        this.resourcesPerFileCount = resourcesPerFileCount;
        this.exportPath = exportPath;
        this.pollingFrequency = pollingFrequency;
        this.lookBackDate = lookBackDate;
    }

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

    public Date getLookBackDate() {
        return lookBackDate;
    }
}
