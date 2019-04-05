package gov.cms.dpc.queue.models;

import gov.cms.dpc.common.converters.StringListConverter;
import gov.cms.dpc.queue.JobStatus;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity(name = "job_queue")
public class JobModel {

    /**
     * Enum which represents the resource types available for export from BlueButton
     */
    public enum ResourceType {
        PATIENT,
        EOB
    }

    public static final long serialVersionUID = 42L;

    @Id
    private UUID jobID;
    private ResourceType type;
    @Column(name = "provider_id")
    private String providerID;
    @Convert(converter = StringListConverter.class)
    @Column(name = "patients", columnDefinition = "text")
    private List<String> patients;
    private JobStatus status;

    public JobModel() {
        // Hibernate required
    }

    public JobModel(UUID jobID, ResourceType type, String providerID, List<String> patients) {
        this.jobID = jobID;
        this.type = type;
        this.providerID = providerID;
        this.patients = patients;
        this.status = JobStatus.QUEUED;
    }

    public UUID getJobID() {
        return jobID;
    }

    public void setJobID(UUID jobID) {
        this.jobID = jobID;
    }

    public ResourceType getType() {
        return type;
    }

    public void setType(ResourceType type) {
        this.type = type;
    }

    public String getProviderID() {
        return providerID;
    }

    public void setProviderID(String providerID) {
        this.providerID = providerID;
    }

    public List<String> getPatients() {
        return patients;
    }

    public void setPatients(List<String> patients) {
        this.patients = patients;
    }

    public JobStatus getStatus() {
        return status;
    }

    public void setStatus(JobStatus status) {
        this.status = status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JobModel jobModel = (JobModel) o;
        return Objects.equals(jobID, jobModel.jobID) &&
                type == jobModel.type &&
                Objects.equals(providerID, jobModel.providerID) &&
                Objects.equals(patients, jobModel.patients) &&
                status == jobModel.status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(jobID, type, providerID, patients, status);
    }
}
