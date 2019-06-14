package gov.cms.dpc.queue.models;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.hl7.fhir.dstu3.model.ResourceType;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * A JobResult represents the output of a job. There is a one-to-one relationship with export files.
 * The object is immutable.
 */
@Entity(name = "job_result")
public class JobResult implements Serializable {
    public static final long serialVersionUID = 42L;

    @Embeddable
    public static class JobResultID implements Serializable {
        public static final long serialVersionUID = 3L;

        private UUID jobID;

        @Column(name = "resource_type")
        private ResourceType resourceType;

        @Column(name = "sequence")
        private int sequence;

        public JobResultID() {
        }

        public JobResultID(UUID jobID, ResourceType resourceType, int sequence) {
            this.jobID = jobID;
            this.resourceType = resourceType;
            this.sequence = sequence;
        }

        public ResourceType getResourceType() {
            return resourceType;
        }

        public UUID getJobID() {
            return jobID;
        }

        public int getSequence() {
            return sequence;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof JobResultID)) return false;
            JobResultID other = (JobResultID) o;
            return Objects.equals(jobID, other.jobID)
                    && Objects.equals(resourceType, other.resourceType)
                    && Objects.equals(sequence, other.sequence);
        }

        @Override
        public int hashCode() {
            return Objects.hash(jobID, resourceType, sequence);
        }
    }

    /**
     * Form a file name for passed in parameters.
     *
     * @param jobID        - the jobs id
     * @param resourceType - the resource type
     * @param sequence     - the sequence
     * @return a file name
     */
    public static String formOutputFileName(UUID jobID, ResourceType resourceType, int sequence) {
        return String.format("%s-%s.%s", jobID.toString(), sequence, resourceType.getPath());
    }

    @EmbeddedId
    private JobResultID jobResultID;

    @Column(name = "count")
    private int count;

    public JobResult() {
        // for hibernate
    }

    public JobResult(UUID jobID, ResourceType resourceType, int sequence, int count) {
        this.jobResultID = new JobResultID(jobID, resourceType, sequence);
        this.count = count;
    }

    public JobResultID getJobResultID() {
        return jobResultID;
    }

    public UUID getJobID() {
        return jobResultID.getJobID();
    }

    public ResourceType getResourceType() {
        return jobResultID.getResourceType();
    }

    public int getSequence() {
        return jobResultID.sequence;
    }

    public int getCount() {
        return count;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof JobResult)) return false;
        JobResult other = (JobResult) o;
        return new EqualsBuilder()
                .append(jobResultID, other.jobResultID)
                .append(count, other.count)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return Objects.hash(jobResultID, count);
    }

    @Override
    public String toString() {
        return "JobResult{" +
                "jobID=" + jobResultID.jobID +
                ", resourceType=" + jobResultID.resourceType +
                ", sequence=" + jobResultID.sequence +
                ", count=" + count +
                '}';
    }
}
