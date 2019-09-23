package gov.cms.dpc.aggregation.engine;

/**
 * Holds configuration information for the operations of {@link gov.cms.dpc.aggregation.engine.AggregationEngine}.
 */
public class OperationsConfig {

    private int retryCount;
    private int resourcesPerFileCount;
    private String exportPath;
    private int pollingFrequency;

    public OperationsConfig(
            int resourcesPerFileCount,
            String exportPath,
            int retryCount,
            int pollingFrequency
    ) {
        this.retryCount = retryCount;
        this.resourcesPerFileCount = resourcesPerFileCount;
        this.exportPath = exportPath;
        this.pollingFrequency = pollingFrequency;
    }

    public OperationsConfig(
            int resourcesPerFileCount,
            String exportPath,
            int pollingFrequency
    ) {
        this.retryCount = 3;
        this.resourcesPerFileCount = resourcesPerFileCount;
        this.exportPath = exportPath;
        this.pollingFrequency = pollingFrequency;
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
}
