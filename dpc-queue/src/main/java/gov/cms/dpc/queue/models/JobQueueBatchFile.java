package gov.cms.dpc.queue.models;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.hibernate.annotations.Immutable;
import org.hl7.fhir.dstu3.model.ResourceType;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * A JobQueueBatchFile represents the output of a job. There is a one-to-one relationship with export files.
 * The object is immutable.
 */
@Immutable
@Entity(name = "job_queue_batch_file")
public class JobQueueBatchFile implements Serializable {
    public static final long serialVersionUID = 42L;

    @Embeddable
    public static class JobQueueBatchFileID implements Serializable {
        public static final long serialVersionUID = 3L;

        @Column(name = "batch_id")
        private UUID batchID;

        @Column(name = "resource_type")
        private ResourceType resourceType;

        @Column(name = "sequence")
        private int sequence;

        public JobQueueBatchFileID() {
        }

        public JobQueueBatchFileID(UUID batchID, ResourceType resourceType, int sequence) {
            this.batchID = batchID;
            this.resourceType = resourceType;
            this.sequence = sequence;
        }

        public UUID getBatchID() {
            return batchID;
        }

        public ResourceType getResourceType() {
            return resourceType;
        }

        public int getSequence() {
            return sequence;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;

            if (o == null || getClass() != o.getClass()) return false;

            JobQueueBatchFileID that = (JobQueueBatchFileID) o;

            return new EqualsBuilder()
                    .append(sequence, that.sequence)
                    .append(batchID, that.batchID)
                    .append(resourceType, that.resourceType)
                    .isEquals();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder(17, 37)
                    .append(batchID)
                    .append(resourceType)
                    .append(sequence)
                    .toHashCode();
        }

        @Override
        public String toString() {
            return "JobQueueBatchFileID{" +
                    "batchID=" + batchID +
                    ", resourceType=" + resourceType +
                    ", sequence=" + sequence +
                    '}';
        }
    }

    /**
     * Form a file name for passed in parameters.
     *
     * @param batchID      - the batch id
     * @param resourceType - the resource type
     * @param sequence     - the sequence
     * @return a file name
     */
    public static String formOutputFileName(UUID batchID, ResourceType resourceType, int sequence) {
        return String.format("%s-%s.%s", batchID.toString(), sequence, resourceType.getPath());
    }

    @EmbeddedId
    private JobQueueBatchFileID jobQueueBatchFileID;

    @Column(name = "job_id")
    private UUID jobID;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "count")
    private int count;

    public JobQueueBatchFile() {
        // for hibernate
    }

    public JobQueueBatchFile(UUID jobID, UUID batchID, ResourceType resourceType, int sequence, int count) {
        this.jobQueueBatchFileID = new JobQueueBatchFileID(batchID, resourceType, sequence);
        this.jobID = jobID;
        this.fileName = formOutputFileName(batchID, resourceType, sequence);
        this.count = count;
    }

    public JobQueueBatchFileID getJobQueueBatchFileID() {
        return jobQueueBatchFileID;
    }

    public UUID getBatchID() {
        return jobQueueBatchFileID.getBatchID();
    }

    public UUID getJobID() {
        return jobID;
    }

    public ResourceType getResourceType() {
        return jobQueueBatchFileID.getResourceType();
    }

    public int getSequence() {
        return jobQueueBatchFileID.getSequence();
    }

    public String getFileName() {
        return fileName;
    }

    public int getCount() {
        return count;
    }

    public void appendCount(int count) {
        this.count += count;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JobQueueBatchFile that = (JobQueueBatchFile) o;
        return count == that.count &&
                jobQueueBatchFileID.equals(that.jobQueueBatchFileID) &&
                jobID.equals(that.jobID);
    }

    @Override
    public int hashCode() {
        return Objects.hash(jobQueueBatchFileID, jobID);
    }

    @Override
    public String toString() {
        return "JobQueueBatchFile{" +
                "jobQueueBatchFileID=" + jobQueueBatchFileID +
                ", jobID=" + jobID +
                ", count=" + count +
                '}';
    }
}
