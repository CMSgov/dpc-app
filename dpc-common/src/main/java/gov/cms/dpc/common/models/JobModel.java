package gov.cms.dpc.common.models;

import java.util.List;
import java.util.Objects;

public class JobModel {

    public static final long serialVersionUID = 42L;

    private final String providerID;
    private final List<String> patients;

    public JobModel(String providerID, List<String> patients) {
        this.providerID = providerID;
        this.patients = patients;
    }

    public String getProviderID() {
        return providerID;
    }

    public List<String> getPatients() {
        return patients;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JobModel jobModel = (JobModel) o;
        return providerID.equals(jobModel.providerID) &&
                patients.equals(jobModel.patients);
    }

    @Override
    public int hashCode() {
        return Objects.hash(providerID, patients);
    }
}
