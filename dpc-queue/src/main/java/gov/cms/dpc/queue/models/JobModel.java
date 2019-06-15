package gov.cms.dpc.queue.models;

import gov.cms.dpc.common.converters.StringListConverter;
import gov.cms.dpc.queue.JobStatus;
import gov.cms.dpc.queue.converters.ResourceTypeListConverter;
import gov.cms.dpc.queue.exceptions.JobQueueFailure;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.checkerframework.checker.units.qual.C;
import org.hl7.fhir.dstu3.model.ResourceType;

import javax.persistence.*;
import java.io.Serializable;
import java.security.interfaces.RSAPublicKey;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

/**
 * The JobModel tracks the work done on a bulk export request. It contains the essential details of the request and
 * the results of the requests.
 */
@Entity(name = "job_queue")
public class JobModel implements Serializable, Cloneable {
    public static final long serialVersionUID = 42L;

    /**
     * The list of resource type supported by DCP
     */
    public static final List<ResourceType> validResourceTypes = List.of(
            ResourceType.Patient,
            ResourceType.ExplanationOfBenefit,
            ResourceType.Coverage);

    /**
     * Test if the resource type is in the list of resources supported by the DCP
     *
     * @param type - {@code true} resource is supported by DPC. {@code false} resource is not supported by DPC.
     * @return True iff the passed in type is valid f
     */
    public static Boolean isValidResourceType(ResourceType type) {
        return validResourceTypes.contains(type);
    }

    /**
     * The unique job identifier
     */
    @Id
    private UUID jobID;

    /**
     * The list of resource types requested
     *
     * We need to use {@link FetchType#EAGER}, otherwise the session will close before we actually read the job results and the call will fail.
     */
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name="jobID")
    private List<JobResult> jobResults;

    /**
     * The provider-id from the request
     */
    @Column(name = "provider_id")
    private String providerID;

    /**
     * The list of patient-ids for the specified provider from the attribution server
     */
    @Convert(converter = StringListConverter.class)
    @Column(name = "patients", columnDefinition = "text")
    private List<String> patients;

    /**
     * The list of resources for this job. Set at job creation.
     */
    @Convert(converter = ResourceTypeListConverter.class)
    @Column(name = "resource_types")
    private List<ResourceType> resourceTypes;

    /**
     * The current status of this job
     */
    private JobStatus status;

    /**
     * The public key used to encrypt the files
     */
    @Column(name = "rsa_public_key")
    private byte[] rsaPublicKey;

    /**
     * The time the job was submitted
     */
    @Column(name = "submit_time", nullable = true)
    private OffsetDateTime submitTime;

    /**
     * The time the job started to work
     */
    @Column(name = "start_time", nullable = true)
    private OffsetDateTime startTime;

    /**
     * The time the job was completed
     */
    @Column(name = "complete_time", nullable = true)
    private OffsetDateTime completeTime;

    public JobModel() {
        // Hibernate required
    }

    public JobModel(UUID jobID, List<ResourceType> resourceTypes, String providerID, List<String> patients) {
        this.jobID = jobID;
        this.resourceTypes = resourceTypes;
        this.jobResults = List.of();
        this.providerID = providerID;
        this.patients = patients;
        this.status = JobStatus.QUEUED;
        this.submitTime = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public JobModel(UUID jobID, List<ResourceType> resourceTypes, String providerID, List<String> patients, RSAPublicKey pubKey) {
        this.jobID = jobID;
        this.resourceTypes = resourceTypes;
        this.jobResults = List.of();
        this.providerID = providerID;
        this.patients = patients;
        this.status = JobStatus.QUEUED;
        this.rsaPublicKey = pubKey.getEncoded();
        this.submitTime = OffsetDateTime.now(ZoneOffset.UTC);
    }

    private JobModel(UUID jobID,
                     List<ResourceType> resourceTypes,
                     String providerID,
                     List<String> patients,
                     JobStatus status,
                     List<JobResult> results,
                     byte[] pubKey,
                     OffsetDateTime submitTime,
                     OffsetDateTime startTime,
                     OffsetDateTime completeTime) {
        this.jobID = jobID;
        this.resourceTypes = resourceTypes;
        this.providerID = providerID;
        this.patients = patients;
        this.status = status;
        this.jobResults = results;
        this.rsaPublicKey = pubKey;
        this.submitTime = submitTime;
        this.startTime = startTime;
        this.completeTime = completeTime;
    }

    /**
     * Is the job model fields consistent. Useful before and after serialization.
     *
     * @return True if the fields are consistent with each other
     */
    public Boolean isValid() {
        switch (status) {
            case QUEUED:
                return submitTime != null;
            case RUNNING:
                return submitTime != null && startTime != null;
            case COMPLETED:
            case FAILED:
                return submitTime != null && startTime != null && completeTime != null;
            default:
                return false;
        }
    }

    public UUID getJobID() {
        return jobID;
    }

    public List<ResourceType> getResourceTypes() {
        return resourceTypes;
    }

    public List<JobResult> getJobResults() {
        return jobResults;
    }

    public Optional<JobResult> getJobResult(ResourceType forResourceType) {
        return jobResults.stream().filter(result -> result.getResourceType().equals(forResourceType)).findFirst();
    }

    public String getProviderID() {
        return providerID;
    }

    public List<String> getPatients() {
        return patients;
    }

    public JobStatus getStatus() {
        return status;
    }

    public byte[] getRsaPublicKey() {
        if (rsaPublicKey == null) {
            throw new NullPointerException("This Job was created without a public key!");
        } else {
            return rsaPublicKey;
        }
    }

    public Optional<OffsetDateTime> getSubmitTime() {
        return Optional.ofNullable(submitTime);
    }

    public Optional<OffsetDateTime> getStartTime() {
        return Optional.ofNullable(startTime);
    }

    public Optional<OffsetDateTime> getCompleteTime() {
        return Optional.ofNullable(completeTime);
    }

    /**
     * Make a JobModel that has a RUNNING status from the current JobModel.
     * This job should be in the QUEUED state.
     *
     * @return the new JobModel in the RUNNING state.
     */
    public JobModel makeRunningJob() {
        if (this.status != JobStatus.QUEUED) {
            throw new JobQueueFailure(jobID, String.format("Cannot run job. JobStatus: %s", this.status));
        }
        return new JobModel(jobID,
                resourceTypes,
                providerID,
                patients,
                JobStatus.RUNNING,
                jobResults,
                rsaPublicKey,
                submitTime,
                OffsetDateTime.now(ZoneOffset.UTC),
                null);
    }

    /**
     * Make a JobModel that has a COMPLETED or FAILED status from the current JobModel.
     * This job should be in the RUNNING state.
     *
     * @param status - the new status
     * @param results - the job results to add the finished state
     * @return the new JobModel in a finished state.
     */
    public JobModel makeFinishedJob(JobStatus status, List<JobResult> results) {
        assert(status == JobStatus.COMPLETED || status == JobStatus.FAILED);
        if (this.status != JobStatus.RUNNING) {
            throw new JobQueueFailure(jobID, String.format("Cannot complete. JobStatus: %s", this.status));
        }
        return new JobModel(jobID,
                resourceTypes,
                providerID,
                patients,
                status,
                List.copyOf(results),
                rsaPublicKey,
                submitTime,
                startTime,
                OffsetDateTime.now(ZoneOffset.UTC));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof JobModel)) return false;
        JobModel other = (JobModel) o;
        return new EqualsBuilder()
                .append(jobID, other.jobID)
                .append(jobResults, other.jobResults)
                .append(resourceTypes, other.resourceTypes)
                .append(providerID, other.providerID)
                .append(patients, other.patients)
                .append(submitTime, other.submitTime)
                .append(startTime, other.startTime)
                .append(completeTime, other.completeTime)
                .append(status, other.status)
                .append(rsaPublicKey, other.rsaPublicKey)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return Objects.hash(jobID, resourceTypes, jobResults, providerID, patients, status, submitTime, startTime, completeTime, rsaPublicKey);
    }

    @Override
    public String toString() {
        return "JobModel{" +
                "jobID=" + jobID +
                ", resourceTypes=" + resourceTypes.toString() +
                ", providerID='" + providerID + '\'' +
                ", patients=" + patients +
                ", status=" + status +
                ", rsaPublicKey=" + Arrays.toString(rsaPublicKey) +
                ", jobResult=" + jobResults +
                ", submitTime=" + submitTime +
                ", startTime=" + startTime +
                ", completeTime=" + completeTime +
                '}';
    }

    @Override
    public JobModel clone() {
        return new JobModel(jobID,resourceTypes, providerID, patients, status, jobResults, rsaPublicKey, submitTime, startTime, completeTime);
    }
}
