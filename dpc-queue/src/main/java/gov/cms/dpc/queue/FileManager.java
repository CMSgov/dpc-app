package gov.cms.dpc.queue;

import gov.cms.dpc.common.annotations.ExportPath;
import gov.cms.dpc.queue.models.JobQueueBatch;
import gov.cms.dpc.queue.models.JobQueueBatchFile;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.zip.GZIPInputStream;

public class FileManager {

    private static final Logger logger = LoggerFactory.getLogger(FileManager.class);

    private final String fileLocation;
    private final IJobQueue jobQueue;

    @Inject
    public FileManager(@ExportPath String fileLocation, IJobQueue jobQueue) {
        this.fileLocation = fileLocation;
        this.jobQueue = jobQueue;
    }

    public FilePointer getFile(UUID organizationID, String fileID) {
        final JobQueueBatchFile batchFile = this.jobQueue.getJobBatchFile(organizationID, fileID)
                .orElseThrow(() -> new WebApplicationException("Cannot find file", Response.Status.NOT_FOUND));

        return getFile(batchFile);
    }

    public FilePointer getFile(JobQueueBatchFile batchFile) {
        final JobQueueBatch jobQueueBatch = this.jobQueue.getBatch(batchFile.getBatchID())
            .orElseThrow(() -> new WebApplicationException("Cannot export job for file", Response.Status.NOT_FOUND));

        // File might be compressed or not
        final Path compressedPath = Paths.get(String.format("%s/%s.ndjson.gz", fileLocation, batchFile.getFileName()));
        final Path nonCompressedPath = Paths.get(String.format("%s/%s.ndjson", fileLocation, batchFile.getFileName()));

        Path path;
        boolean compressed;
        if (Files.exists(compressedPath)) {
            path = compressedPath;
            compressed = true;
        } else {
            path = nonCompressedPath;
            compressed = false;
        }
        logger.debug("Streaming file {}", path);

        return new FilePointer(Hex.toHexString(batchFile.getChecksum()),
            batchFile.getFileLength(),
            batchFile.getJobID(),
            jobQueueBatch.getStartTime().orElseThrow(() -> new IllegalStateException("Cannot find start time of completed job")),
            new File(path.toString()),
            compressed);
    }

    public static class FilePointer {

        private final String checksum;
        private final long fileSize;
        private final UUID jobID;
        private final OffsetDateTime creationTime;
        private final File file;
        private final boolean compressed;

        public FilePointer(String checksum, long fileSize, UUID jobID, OffsetDateTime creationTime, File file, boolean compressed) {
            this.checksum = checksum;
            this.fileSize = fileSize;
            this.jobID = jobID;
            this.creationTime = creationTime;
            this.file = file;
            this.compressed = compressed;
        }

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

        public boolean isCompressed() { return compressed; }

        public InputStream getUncompressedInputStream() throws IOException {
            if (compressed) {
                return new GZIPInputStream(new FileInputStream(file));
            } else {
                return new FileInputStream(file);
            }
        }
    }
}
