package gov.cms.dpc.queue.models;

import gov.cms.dpc.common.converters.StringListConverter;
import gov.cms.dpc.queue.JobStatus;
import gov.cms.dpc.queue.converters.ResourceTypeListConverter;

import org.hl7.fhir.dstu3.model.ResourceType;

import javax.persistence.*;
import java.security.interfaces.RSAPublicKey;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity(name = "job_queue")
public class JobModel {
    public static final long serialVersionUID = 42L;

    /**
     * The list of resource type supported by DCP
     */
    public static final List<ResourceType> validResourceTypes = List.of(ResourceType.Patient, ResourceType.ExplanationOfBenefit);

    /**
     * Test if the resource type is in the list of resources supported by the DCP
     *
     * @param type
     * @return True iff the passed in type is valid f
     */
    public static Boolean isValidResourceType(ResourceType type) {
        return validResourceTypes.contains(type);
    }

    /**
     * Form a file name for passed in parameters.
     *
     * @param jobID - the jobs id
     * @param resourceType - the resource type
     * @return a file name
     */
    public static String outputFileName(UUID jobID, ResourceType resourceType) {
        return String.format("%s.%s", jobID.toString(), resourceType.getPath());
    }

    @Id
    private UUID jobID;

    @Convert(converter = ResourceTypeListConverter.class)
    @Column(name = "resource_types")
    private List<ResourceType> resourceTypes;

    @Column(name = "provider_id")
    private String providerID;

    @Convert(converter = StringListConverter.class)
    @Column(name = "patients", columnDefinition = "text")
    private List<String> patients;

    private JobStatus status;

    private RSAPublicKey rsaPublicKey;

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

    public JobModel(UUID jobID, List<ResourceType> resourceTypes, String providerID, List<String> patients, RSAPublicKey pubKey) {
        this.jobID  = jobID;
        this.resourceTypes = resourceTypes;
        this.providerID = providerID;
        this.patients  = patients;
        this.status = JobStatus.QUEUED;
        this.rsaPublicKey = pubKey;
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

    public void setResourceTypes(List<ResourceType> resourceTypes) {
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

    public RSAPublicKey getRsaPublicKey() {
        if (rsaPublicKey == null) {
            throw new NullPointerException("This Job was created without a public key!");
        } else {
            return rsaPublicKey;
        }
    }

    public void setRsaPublicKey(RSAPublicKey rsaPublicKey) {
        this.rsaPublicKey = rsaPublicKey;
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
