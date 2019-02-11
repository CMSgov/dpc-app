package gov.cms.dpc.common.models;

public class JobModel {

    private final String providerID;

    public JobModel(String providerID) {
        this.providerID = providerID;
    }

    public String getProviderID() {
        return providerID;
    }
}
