package gov.cms.dpc.common.models;

import java.util.Objects;
import java.util.Set;

public class JobModel {

    public static final long serializationUID = 42L;

    private final String providerID;
    private final Set<String> beneficiaries;

    public JobModel(String providerID, Set<String> beneficiaries) {
        this.providerID = providerID;
        this.beneficiaries = beneficiaries;
    }

    public String getProviderID() {
        return providerID;
    }

    public Set<String> getBeneficiaries() {
        return beneficiaries;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JobModel jobModel = (JobModel) o;
        return providerID.equals(jobModel.providerID) &&
                beneficiaries.equals(jobModel.beneficiaries);
    }

    @Override
    public int hashCode() {
        return Objects.hash(providerID, beneficiaries);
    }
}
