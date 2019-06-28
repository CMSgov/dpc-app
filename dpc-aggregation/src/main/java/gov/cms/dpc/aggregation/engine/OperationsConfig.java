package gov.cms.dpc.aggregation.engine;

/**
 * Holds configuration information for the operations of {@link gov.cms.dpc.aggregation.engine.AggregationEngine}.
 */
public class OperationsConfig {
    private int retryCount;

    private int resourcesPerFileCount;

    private boolean parallelRequestsEnabled;

    private String exportPath;

    private boolean encryptionEnabled;

    public OperationsConfig(int retryCount, int resourcesPerFileCount, boolean parallelRequestsEnabled, String exportPath, boolean encryptionEnabled) {
        this.retryCount = retryCount;
        this.resourcesPerFileCount = resourcesPerFileCount;
        this.parallelRequestsEnabled = parallelRequestsEnabled;
        this.exportPath = exportPath;
        this.encryptionEnabled = encryptionEnabled;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public int getResourcesPerFileCount() {
        return resourcesPerFileCount;
    }

    public boolean isParallelRequestsEnabled() {
        return parallelRequestsEnabled;
    }

    public String getExportPath() {
        return exportPath;
    }

    public boolean isEncryptionEnabled() {
        return encryptionEnabled;
    }
}
