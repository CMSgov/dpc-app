package gov.cms.dpc.common.models;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class JobModel {

    /**
     * Enum which represents the resource types available for export from BlueButton
     */
    public enum ResourceType {
        PATIENT,
        EOB
    }

    public static final long serialVersionUID = 42L;

    private final UUID jobID;
    private final ResourceType type;
    private final String providerID;
    private final List<String> patients;

    public JobModel(UUID jobID, ResourceType type, String providerID, List<String> patients) {
        this.jobID = jobID;
        this.type = type;
        this.providerID = providerID;
        this.patients = patients;
    }

    public String getProviderID() {
        return providerID;
    }

    public List<String> getPatients() {
        return patients;
    }

    public UUID getJobID() {
        return jobID;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JobModel jobModel = (JobModel) o;
        return Objects.equals(jobID, jobModel.jobID) &&
                Objects.equals(providerID, jobModel.providerID) &&
                Objects.equals(patients, jobModel.patients);
    }

    @Override
    public int hashCode() {
        return Objects.hash(jobID, providerID, patients);
    }
}
