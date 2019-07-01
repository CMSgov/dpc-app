package gov.cms.dpc.aggregation.engine;

import ca.uhn.fhir.context.FhirContext;
import gov.cms.dpc.queue.exceptions.JobQueueFailure;
import gov.cms.dpc.queue.models.JobModel;
import gov.cms.dpc.queue.models.JobResult;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.CipherOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Writes files from batches of FHIR Resources
 */
class ResourceWriter {
    private static final Logger logger = LoggerFactory.getLogger(ResourceWriter.class);
    private static final char DELIM = '\n';

    private FhirContext fhirContext;
    private OperationsConfig config;
    private JobModel job;
    private UUID jobID;
    private ResourceType resourceType;

    /**
     * Form the full file name of an output file
     * @param jobID        - {@link UUID} ID of export job
     * @param resourceType - {@link ResourceType} to append to filename
     * @param sequence     - batch sequence number
     * @return return the path
     */
    static String formOutputFilePath(String exportPath, UUID jobID, ResourceType resourceType, int sequence) {
        return String.format("%s/%s.ndjson", exportPath, JobResult.formOutputFileName(jobID, resourceType, sequence));
    }

    static String formEncryptedOutputFilePath(String exportPath, UUID jobID, ResourceType resourceType, int sequence) {
        return String.format("%s/%s.ndjson.enc", exportPath, JobResult.formOutputFileName(jobID, resourceType, sequence));
    }

    static String formEncryptedMetadataPath(String exportPath, UUID jobID, ResourceType resourceType, int sequence) {
        return String.format("%s/%s-metadata.json", exportPath, JobResult.formOutputFileName(jobID, resourceType, sequence));
    }

    /**
     * Create a context for fetching FHIR resources
     * @param fhirContext - the single context for the engine
     * @param config - config to use for the engine
     * @param job - the context for logging and reporting
     * @param resourceType - the resource type to fetch
     */
    ResourceWriter(FhirContext fhirContext,
                    JobModel job,
                    ResourceType resourceType,
                    OperationsConfig config) {
        this.fhirContext = fhirContext;
        this.config = config;
        this.job = job;
        this.jobID = job.getJobID();
        this.resourceType = resourceType;
    }

    /**
     * Write a batch of resources to a file. Encrypt if the encryption is enabled.
     *
     * @param batch is the list of resources to write
     * @param counter is general counter for batch number
     * @return The JobResult associated with this file
     */
    JobResult writeBatch(AtomicInteger counter, List<Resource> batch) {
        try {
            final var byteStream = new ByteArrayOutputStream();
            final var sequence = counter.getAndIncrement();
            final var jsonParser = fhirContext.newJsonParser();
            OutputStream writer = config.isEncryptionEnabled() ?
                    formCipherStream(byteStream, job, resourceType, sequence):
                    byteStream;
            String outputPath = config.isEncryptionEnabled() ?
                    formEncryptedOutputFilePath(config.getExportPath(), jobID, resourceType, sequence):
                    formOutputFilePath(config.getExportPath(), jobID, resourceType, sequence);

            logger.debug("Start writing to {}", outputPath);
            for (var resource: batch) {
                final String str = jsonParser.encodeResourceToString(resource);
                writer.write(str.getBytes(StandardCharsets.UTF_8));
                writer.write(DELIM);
            }
            writer.flush();
            writer.close();
            writeToFile(byteStream.toByteArray(), outputPath);

            logger.debug("Finished writing to '{}'", outputPath);
            return new JobResult(jobID, resourceType, sequence, batch.size());
        } catch(IOException ex) {
            throw new JobQueueFailure(jobID, "IO error writing a resource", ex);
        } catch(SecurityException ex) {
            throw new JobQueueFailure(jobID, "Error encrypting a resource", ex);
        } catch(Exception ex) {
            throw new JobQueueFailure(jobID, "General failure consuming a resource", ex);
        }
    }

    /**
     * Build an encrypting stream that contains an inner stream.
     *
     * @param writer is the inner stream to write to
     * @param job is the context including the RSA key
     * @param resourceType is the type of resource being written
     * @param sequence is the batch sequence being written
     * @return a output stream to write to
     * @throws GeneralSecurityException if there is something wrong with the encryption config
     * @throws IOException if there is something wrong with the file io.
     */
    private OutputStream formCipherStream(OutputStream writer, JobModel job, ResourceType resourceType, int sequence) throws GeneralSecurityException, IOException {
        final var metadataPath = formEncryptedMetadataPath(config.getExportPath(), job.getJobID(), resourceType, sequence);
        try(final CipherBuilder cipherBuilder = new CipherBuilder();
            final FileOutputStream metadataWriter = new FileOutputStream(metadataPath)) {
            cipherBuilder.generateKeyMaterial();
            final String json = cipherBuilder.getMetadata(job.getRsaPublicKey());
            metadataWriter.write(json.getBytes(StandardCharsets.UTF_8));
            return new CipherOutputStream(writer, cipherBuilder.formCipher());
        }
    }

    /**
     * Write a array of bytes to a file. Name the file according to the supplied name
     *
     * @param bytes - Bytes to write
     * @param fileName - The fileName to write too
     * @throws IOException - If the write fails
     */
    private void writeToFile(byte[] bytes, String fileName) throws IOException {
        if (bytes.length == 0) {
            return;
        }
        try (final var outputFile = new FileOutputStream(fileName)) {
            outputFile.write(bytes);
            outputFile.flush();
        }
    }
}
