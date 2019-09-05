package gov.cms.dpc.aggregation.engine;

/**
 * Holds configuration information for the operations of {@link gov.cms.dpc.aggregation.engine.AggregationEngine}.
 */
public class OperationsConfig {
    private int retryCount;
    private int resourcesPerFileCount;
    private String exportPath;


    public OperationsConfig(int resourcesPerFileCount,
                            String exportPath,
                            int retryCount) {
        this.retryCount = retryCount;
        this.resourcesPerFileCount = resourcesPerFileCount;
        this.exportPath = exportPath;
    }

    public OperationsConfig(int resourcesPerFileCount,
                            String exportPath) {
        this.retryCount = 3;
        this.resourcesPerFileCount = resourcesPerFileCount;
        this.exportPath = exportPath;
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
}
