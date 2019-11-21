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
import java.util.UUID;

public class FileManager {

    private static final Logger logger = LoggerFactory.getLogger(FileManager.class);

    private final String fileLocation;
    private final SessionFactory factory;

    @Inject
    FileManager(@ExportPath String fileLocation, DPCQueueManagedSessionFactory factory) {
        this.fileLocation = fileLocation;
        this.factory = factory.getSessionFactory();
    }

    @SuppressWarnings("rawtypes")
    public FilePointer getFile(UUID organizationID, String fileID) {
        // Try

        final JobQueueBatchFile batchFile;
        try (final Session session = this.factory.openSession()) {

            // Using a raw JPA query here, because the Criteria builder doesn't really support joining un-related entities
            final String queryString =
                    "SELECT f FROM gov.cms.dpc.queue.models.JobQueueBatchFile f " +
                            "LEFT JOIN gov.cms.dpc.queue.models.JobQueueBatch b on b.jobID = f.jobID " +
                            "WHERE f.fileName = :fileName AND b.orgID = :org";

            final Query query = session.createQuery(queryString);
            query.setParameter("fileName", fileID);
            query.setParameter("org", organizationID);
            batchFile = (JobQueueBatchFile) query.getSingleResult();
        } catch (NoResultException e) {
            throw new WebApplicationException("Cannot find file", Response.Status.NOT_FOUND);
        }

        final java.nio.file.Path path = Paths.get(String.format("%s/%s.ndjson", fileLocation, batchFile.getFileName()));
        logger.debug("Streaming file {}", path.toString());
        return new FilePointer(Hex.toHexString(batchFile.getChecksum()),
                batchFile.getFileLength(),
                new File(path.toString()));
    }

    public static class FilePointer {

        private final String checksum;
        private final long fileSize;
        private final File file;

        public FilePointer(String checksum, long fileSize, File file) {
            this.checksum = checksum;
            this.fileSize = fileSize;
            this.file = file;
        }

        public String getChecksum() {
            return checksum;
        }

        public long getFileSize() {
            return fileSize;
        }

        public File getFile() {
            return file;
        }
    }
}
