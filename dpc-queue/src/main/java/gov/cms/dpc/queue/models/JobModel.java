package gov.cms.dpc.queue.models;

import gov.cms.dpc.common.converters.StringListConverter;
import gov.cms.dpc.queue.JobStatus;
import gov.cms.dpc.queue.converters.ResourceTypeListConverter;

import org.hl7.fhir.dstu3.model.ResourceType;

import javax.persistence.*;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity(name = "job_queue")
public class JobModel {
    public static final long serialVersionUID = 42L;

    public static final List<ResourceType> validResourceTypes = List.of(ResourceType.Patient, ResourceType.ExplanationOfBenefit);

    public static String outputFileName(UUID jobID, ResourceType resourceType) {
        return String.format("%s.%s", jobID.toString(), resourceType.getPath());
    }

    @Id
    private UUID jobID;

    @Convert(converter = ResourceTypeListConverter.class)
    @Column(name = "resourceTypes")
    private List<ResourceType> resourceTypes;

    @Column(name = "provider_id")
    private String providerID;

    @Convert(converter = StringListConverter.class)
    @Column(name = "patients", columnDefinition = "text")
    private List<String> patients;

    private JobStatus status;

    public JobModel() {
        // Hibernate required
    }

    public JobModel(UUID jobID, List<ResourceType> resourceTypes, String providerID, List<String> patients) {
        this.jobID = jobID;
        this.resourceTypes = resourceTypes;
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

    public List<ResourceType> getResourceTypes() {
        return resourceTypes;
    }

    public void setResourceTypes(List<ResourceType> types) {
            this.resourceTypes = resourceTypes;
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
                Objects.equals(resourceTypes, jobModel.resourceTypes) &&
                Objects.equals(providerID, jobModel.providerID) &&
                Objects.equals(patients, jobModel.patients) &&
                status == jobModel.status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(jobID, resourceTypes, providerID, patients, status);
    }
}
