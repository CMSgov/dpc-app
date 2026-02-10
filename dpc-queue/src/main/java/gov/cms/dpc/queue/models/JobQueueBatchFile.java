package gov.cms.dpc.queue.models;

import gov.cms.dpc.fhir.DPCResourceType;
import jakarta.persistence.*;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.exceptions.FHIRException;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * A JobQueueBatchFile represents the output of a job. There is a one-to-one relationship with export files.
 * The object is immutable.
 */
@Entity
@Table(name = "job_queue_batch_file")
public class JobQueueBatchFile implements Serializable {
    public static final long serialVersionUID = 42L;

    @Embeddable
    public static class JobQueueBatchFileID implements Serializable {
        public static final long serialVersionUID = 3L;

        @Column(name = "batch_id")
        private UUID batchID;

        @Column(name = "resource_type")
        private DPCResourceType resourceType;

        @Column(name = "sequence")
        private int sequence;

        public JobQueueBatchFileID() {
        }

        public JobQueueBatchFileID(UUID batchID, DPCResourceType resourceType, int sequence) {
            this.batchID = batchID;
            this.resourceType = resourceType;
            this.sequence = sequence;
        }

        public UUID getBatchID() {
            return batchID;
        }

        public DPCResourceType getResourceType() {
            return resourceType;
        }

        public int getSequence() {
            return sequence;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof JobQueueBatchFileID that)) return false;
            return sequence == that.sequence &&
                    batchID.equals(that.batchID) &&
                    resourceType == that.resourceType;
        }

        @Override
        public int hashCode() {
            return Objects.hash(batchID, resourceType, sequence);
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
    public static String formOutputFileName(UUID batchID, DPCResourceType resourceType, int sequence) {
        return String.format("%s-%s.%s", batchID.toString(), sequence, resourceType.getPath());
    }

    /**
     * Takes a given filename of the format "batchId-seq.resourceType" and converts it into a {@link JobQueueBatchFileID}
     * with a batchId, sequence and resource type.
     * Throws an {@link IllegalArgumentException} if the filename isn't in the correct format.
     *
     * @param fileName The filename to convert.
     * @return {@link JobQueueBatchFileID}.
     */
    public static JobQueueBatchFileID getFileIdFromName(String fileName) {
        UUID batchId;
        int sequence;
        DPCResourceType resourceType;
        try {
            // Batch ID makes up the first 36 characters
            batchId = UUID.fromString(fileName.substring(0, 36));

            // The rest of the file name is in the format #.resource
            String[] restOfFileName = StringUtils.split(fileName.substring(37), ".");
            if (restOfFileName.length != 2) {
                throw new IllegalArgumentException("Filename has extra data appended after resource: " + fileName);
            }

            sequence = Integer.parseInt(restOfFileName[0]);
            resourceType = DPCResourceType.fromPath(restOfFileName[1]);
        } catch (IllegalArgumentException | IndexOutOfBoundsException | FHIRException e) {
            throw new IllegalArgumentException(String.format("Could not parse file name: %s", fileName), e);
        }

        return new JobQueueBatchFileID(batchId, resourceType, sequence);
    }

    @EmbeddedId
    private JobQueueBatchFileID jobQueueBatchFileID;

    @Column(name = "job_id")
    private UUID jobID;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "count")
    private int count;

    @Column(name = "checksum")
    private byte[] checksum;

    @Column(name = "file_length")
    private long fileLength;

    @Transient
    private long patientFileSize;

    public JobQueueBatchFile() {
        // for hibernate
    }


    public JobQueueBatchFile(UUID jobID, UUID batchID, DPCResourceType resourceType, int sequence, int count) {
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

    public DPCResourceType getResourceType() {
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

    public byte[] getChecksum() {
        return checksum;
    }

    public void setChecksum(byte[] checksum) {
        this.checksum = checksum;
    }

    public long getFileLength() {
        return fileLength;
    }

    public void setFileLength(long fileLength) {
        this.fileLength = fileLength;
    }

    public long getPatientFileSize() {
        return patientFileSize;
    }

    public void setPatientFileSize(long patientFileSize) {
        this.patientFileSize = patientFileSize;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof JobQueueBatchFile that)) return false;
        return jobQueueBatchFileID.equals(that.jobQueueBatchFileID) &&
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
