package gov.cms.dpc.api.core;

import gov.cms.dpc.common.annotations.ExportPath;
import gov.cms.dpc.common.hibernate.queue.DPCQueueManagedSessionFactory;
import gov.cms.dpc.queue.models.JobQueueBatchFile;
import org.bouncycastle.util.encoders.Hex;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.persistence.NoResultException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.io.File;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@SuppressWarnings("rawtypes")
public class FileManager {

    private static final Logger logger = LoggerFactory.getLogger(FileManager.class);

    private final String fileLocation;
    private final SessionFactory factory;

    @Inject
    FileManager(@ExportPath String fileLocation, DPCQueueManagedSessionFactory factory) {
        this.fileLocation = fileLocation;
        this.factory = factory.getSessionFactory();
    }

    public FilePointer getFile(UUID organizationID, String fileID) {
        // Try

        final JobQueueBatchFile batchFile;
        final OffsetDateTime creationTime;
        try (final Session session = this.factory.openSession()) {

            // Using a raw JPA query here, because the Criteria builder doesn't really support joining un-related entities
            final String queryString =
                    "SELECT b.completeTime, f FROM gov.cms.dpc.queue.models.JobQueueBatchFile f " +
                            "LEFT JOIN gov.cms.dpc.queue.models.JobQueueBatch b on b.jobID = f.jobID " +
                            "WHERE f.fileName = :fileName AND b.orgID = :org";

            final Query query = session.createQuery(queryString);
            query.setParameter("fileName", fileID);
            query.setParameter("org", organizationID);
//            batchFile = (JobQueueBatchFile) query.getSingleResult();
            final List objects = query.getResultList();
            creationTime = (OffsetDateTime) objects.get(0);
            batchFile = (JobQueueBatchFile) objects.get(1);
        } catch (NoResultException e) {
            throw new WebApplicationException("Cannot find file", Response.Status.NOT_FOUND);
        }

        final java.nio.file.Path path = Paths.get(String.format("%s/%s.ndjson", fileLocation, batchFile.getFileName()));
        logger.debug("Streaming file {}", path.toString());
        return new FilePointer(Hex.toHexString(batchFile.getChecksum()),
                batchFile.getFileLength(),
                batchFile.getJobID(),
                creationTime,
                new File(path.toString()));
    }

    public static class FilePointer {

        private final String checksum;
        private final long fileSize;
        private final UUID jobID;
        private final OffsetDateTime creationTime;
        private final File file;

        public FilePointer(String checksum, long fileSize, UUID jobID, OffsetDateTime creationTime, File file) {
            this.checksum = checksum;
            this.fileSize = fileSize;
            this.jobID = jobID;
            this.creationTime = creationTime;
            this.file = file;
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
    }
}
