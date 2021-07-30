package gov.cms.dpc.aggregation.engine;

import ca.uhn.fhir.context.FhirContext;
import gov.cms.dpc.fhir.DPCResourceType;
import gov.cms.dpc.queue.exceptions.JobQueueFailure;
import gov.cms.dpc.queue.models.JobQueueBatch;
import gov.cms.dpc.queue.models.JobQueueBatchFile;
import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Writes files from batches of FHIR Resources
 */
class ResourceWriterV2 {
    private static final Logger logger = LoggerFactory.getLogger(ResourceWriterV2.class);
    private static final char DELIM = '\n';

    private final FhirContext fhirContext;
    private final OperationsConfig config;
    private final JobQueueBatch job;
    private final DPCResourceType resourceType;

    /**
     * Form the full file name of an output file
     * @param batchID      - {@link UUID} ID of the batch job
     * @param resourceType - {@link DPCResourceType} to append to filename
     * @param sequence     - batch sequence number
     * @return return the path
     */
    static String formOutputFilePath(String exportPath, UUID batchID, DPCResourceType resourceType, int sequence) {
        return String.format("%s/%s.ndjson", exportPath, JobQueueBatchFile.formOutputFileName(batchID, resourceType, sequence));
    }

    /**
     * Create a context for fetching FHIR resources
     * @param fhirContext - the single context for the engine
     * @param job - the context for logging and reporting
     * @param resourceType - the resource type to fetch
     * @param config - config to use for the engine
     */
    ResourceWriterV2(FhirContext fhirContext,
                     JobQueueBatch job,
                     DPCResourceType resourceType,
                     OperationsConfig config) {
        this.fhirContext = fhirContext;
        this.config = config;
        this.job = job;
        this.resourceType = resourceType;
    }

    /**
     * @return The resourceType of this resource
     */
    DPCResourceType getResourceType() {
        return resourceType;
    }

    /**
     * Write a batch of resources to a file.
     *
     * @param batch is the list of resources to write
     * @param counter is general counter for batch number
     * @return The JobQueueBatchFile associated with this file
     */
    JobQueueBatchFile writeBatch(AtomicInteger counter, List<Resource> batch) {
        try {
            final var byteStream = new ByteArrayOutputStream();
            final var sequence = counter.getAndIncrement();
            final var jsonParser = fhirContext.newJsonParser();
            OutputStream writer = byteStream;
            String outputPath = formOutputFilePath(config.getExportPath(), job.getBatchID(), resourceType, sequence);
            JobQueueBatchFile file = job.addJobQueueFile(resourceType, sequence, batch.size());

            boolean isStartOfFile = batch.size() == file.getCount();
            Boolean shouldAppendToFile = !isStartOfFile;

            logger.debug("Start writing to {}", outputPath);
            for (var resource: batch) {
                final String str = jsonParser.encodeResourceToString(resource);
                writer.write(str.getBytes(StandardCharsets.UTF_8));
                writer.write(DELIM);
            }
            writer.flush();
            writer.close();
            writeToFile(byteStream.toByteArray(), outputPath, shouldAppendToFile);
            logger.debug("Finished writing to '{}'", outputPath);

            return file;
        } catch(IOException ex) {
            throw new JobQueueFailure(job.getJobID(), job.getBatchID(), "IO error writing a resource", ex);
        } catch(SecurityException ex) {
            throw new JobQueueFailure(job.getJobID(), job.getBatchID(), "Error encrypting a resource", ex);
        } catch(Exception ex) {
            throw new JobQueueFailure(job.getJobID(), job.getBatchID(), "General failure consuming a resource", ex);
        }
    }

    /**
     * Write a array of bytes to a file. Name the file according to the supplied name
     *
     * @param bytes - Bytes to write
     * @param fileName - The fileName to write too
     * @param append - If the
     * @throws IOException - If the write fails
     */
    private void writeToFile(byte[] bytes, String fileName, Boolean append) throws IOException {
        if (bytes.length == 0) {
            return;
        }
        try (final var outputFile = new FileOutputStream(fileName, append)) {
            outputFile.write(bytes);
            outputFile.flush();
        }
    }
}
