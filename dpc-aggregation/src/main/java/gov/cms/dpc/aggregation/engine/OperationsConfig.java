package gov.cms.dpc.aggregation.engine;

/**
 * Holds configuration information for the operations of {@link gov.cms.dpc.aggregation.engine.AggregationEngine}.
 */
public class OperationsConfig {
    private int retryCount;
    private int resourcesPerFileCount;
    private String exportPath;
    private boolean encryptionEnabled;
    private boolean parallelEnabled;
    private float writeThreadFactor;
    private float fetchThreadFactor;


    public OperationsConfig(int resourcesPerFileCount,
                            String exportPath,
                            int retryCount,
                            boolean encryptionEnabled,
                            boolean parallelEnabled,
                            float writeThreadFactor,
                            float fetchThreadFactor) {
        this.retryCount = retryCount;
        this.resourcesPerFileCount = resourcesPerFileCount;
        this.parallelEnabled = parallelEnabled;
        this.exportPath = exportPath;
        this.encryptionEnabled = encryptionEnabled;
        this.writeThreadFactor = writeThreadFactor;
        this.fetchThreadFactor = fetchThreadFactor;
    }

    public OperationsConfig(int resourcesPerFileCount,
                            String exportPath) {
        this.retryCount = 3;
        this.resourcesPerFileCount = resourcesPerFileCount;
        this.parallelEnabled = true;
        this.exportPath = exportPath;
        this.encryptionEnabled = false;
        this.writeThreadFactor = 0.5f;
        this.fetchThreadFactor = 2.5f;
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

    public boolean isEncryptionEnabled() {
        return encryptionEnabled;
    }

    public boolean isParallelEnabled() {
        return parallelEnabled;
    }

    public float getWriteThreadFactor() {
        return writeThreadFactor;
    }

    public float getFetchThreadFactor() {
        return fetchThreadFactor;
    }
}
