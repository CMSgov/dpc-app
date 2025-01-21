package gov.cms.dpc.api.core;

import gov.cms.dpc.common.annotations.ExportPath;
import gov.cms.dpc.queue.IJobQueue;
import gov.cms.dpc.queue.models.JobQueueBatch;
import gov.cms.dpc.queue.models.JobQueueBatchFile;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.UUID;

public class FileManager {

    private static final Logger logger = LoggerFactory.getLogger(FileManager.class);

    private final String fileLocation;
    private final IJobQueue jobQueue;

    @Inject
    FileManager(@ExportPath String fileLocation, IJobQueue jobQueue) {
        this.fileLocation = fileLocation;
        this.jobQueue = jobQueue;
    }

    public FilePointer getFile(UUID organizationID, String fileID) {

        final JobQueueBatchFile batchFile = this.jobQueue.getJobBatchFile(organizationID, fileID)
                .orElseThrow(() -> new WebApplicationException("Cannot find file", Response.Status.NOT_FOUND));

        final JobQueueBatch jobQueueBatch = this.jobQueue.getBatch(batchFile.getBatchID())
                .orElseThrow(() -> new WebApplicationException("Cannot export job for file", Response.Status.NOT_FOUND));

        final java.nio.file.Path path = Paths.get(String.format("%s/%s.ndjson", fileLocation, batchFile.getFileName()));
        logger.debug("Streaming file {}", path);
        return new FilePointer(Hex.toHexString(batchFile.getChecksum()),
                batchFile.getFileLength(),
                batchFile.getJobID(),
                jobQueueBatch.getStartTime().orElseThrow(() -> new IllegalStateException("Cannot find start time of completed job")),
                new File(path.toString()));
    }

    public record FilePointer(String checksum, long fileSize, UUID jobID, OffsetDateTime creationTime, File file) {
        public String getChecksum() {
            return checksum;
        }

        public long getFileSize() {
            return fileSize;
        }

        public UUID getJobID() {
            return jobID;
        }

        public OffsetDateTime getCreationTime() {
            return creationTime;
        }

        public File getFile() {
            return file;
        }
    }
}
