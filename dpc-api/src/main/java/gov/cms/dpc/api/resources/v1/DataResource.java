package gov.cms.dpc.api.resources.v1;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import gov.cms.dpc.api.auth.OrganizationPrincipal;
import gov.cms.dpc.api.auth.annotations.Authorizer;
import gov.cms.dpc.api.core.FileManager;
import gov.cms.dpc.api.models.RangeHeader;
import gov.cms.dpc.api.resources.AbstractDataResource;
import gov.cms.dpc.common.annotations.NoHtml;
import gov.cms.dpc.queue.IJobQueue;
import gov.cms.dpc.queue.JobStatus;
import gov.cms.dpc.queue.models.JobQueueBatch;
import io.dropwizard.auth.Auth;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.*;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static gov.cms.dpc.fhir.dropwizard.filters.StreamingContentSizeFilter.X_CONTENT_LENGTH;

/**
 * Streaming and range logic was taken from here: https://github.com/aruld/jersey-streaming
 */
@Path("/v1/Data")
@Produces("application/ndjson")
public class DataResource extends AbstractDataResource {

    private static final Logger logger = LoggerFactory.getLogger(DataResource.class);
    private static final int CHUNK_SIZE = 1024 * 1024; // Return a maximum of 1MB chunks, but we can modify this later if we need to
    private static final String ACCEPTED_RANGE_VALUE = "bytes";

    private final FileManager manager;
    private final IJobQueue queue;

    @Inject
    public DataResource(FileManager manager, IJobQueue queue) {
        this.manager = manager;
        this.queue = queue;
    }

    @Path("/{fileID}.ndjson")
    @HEAD
    @Timed
    @ExceptionMetered
    @Authorizer
    @Override
    public Response exportFileHead(@Auth OrganizationPrincipal organizationPrincipal,
                                   @HeaderParam(HttpHeaders.IF_NONE_MATCH) Optional<String> fileChecksum,
                                   @HeaderParam(HttpHeaders.IF_MODIFIED_SINCE) Optional<String> modifiedHeader,
                                   @PathParam("fileID") @NoHtml String fileID) {
        final FileManager.FilePointer filePointer = this.manager.getFile(organizationPrincipal.getID(), fileID);

        if (returnCachedValue(filePointer, fileChecksum, modifiedHeader)) {
            return Response.status(Response.Status.NOT_MODIFIED).build();
        }

        return Response.ok()
                .header(HttpHeaders.ETAG, filePointer.getChecksum())
                .header(HttpHeaders.CONTENT_LENGTH, filePointer.getFileSize())
                .header(HttpHeaders.LAST_MODIFIED, filePointer.getCreationTime().toInstant().toEpochMilli())
                .header(HttpHeaders.ACCEPT_RANGES, ACCEPTED_RANGE_VALUE)
                .build();
    }

    @Override
    @Path("/{fileID}.ndjson")
    @GET
    @Timed
    @ExceptionMetered
    @Authorizer
    public Response downloadExportFile(@Auth OrganizationPrincipal organizationPrincipal,
                                       @HeaderParam(HttpHeaders.RANGE) RangeHeader rangeHeader,
                                       @HeaderParam(HttpHeaders.IF_NONE_MATCH) Optional<String> fileChecksum,
                                       @HeaderParam(HttpHeaders.IF_MODIFIED_SINCE) Optional<String> modifiedHeader,
                                       @PathParam("fileID") @NoHtml String fileID) {

        final FileManager.FilePointer filePointer = this.manager.getFile(organizationPrincipal.getID(), fileID);

        // If job is expired, the files should no longer be accessible
        List<JobQueueBatch> batches = queue.getJobBatches(filePointer.getJobID());
        Set<JobStatus> jobStatusSet = batches.stream().map(JobQueueBatch::getStatus).collect(Collectors.toSet());
        if (jobStatusSet.size() == 1 && jobStatusSet.contains(JobStatus.COMPLETED)) {
            OffsetDateTime lastCompleteTime = JobResource.getLatestBatchCompleteTime(batches);

            if (lastCompleteTime.isBefore(OffsetDateTime.now(ZoneOffset.UTC).minusHours(JobResource.JOB_EXPIRATION_HOURS))) {
                return Response.status(Response.Status.GONE).build();
            }
        }

        // If we're provided a file checksum, verify it matches, if so, return a 304
        if (returnCachedValue(filePointer, fileChecksum, modifiedHeader)) {
            return Response.status(Response.Status.NOT_MODIFIED).build();
        }

        final Response response;

        // Process the range request and return a partial stream, but only if they request bytes, ignore everything else
        if (rangeHeader != null) {
            response = buildRangedRequest(fileID, filePointer.getFile(), rangeHeader);
        } else {
            // Return a non-ranged streamed response if the requester doesn't actually send the range header, or if we don't understand the range unit
            response = buildDefaultResponse(fileID, filePointer);
        }

        // Set the cache control headers to make sure the file isn't retained in transit
        final CacheControl cacheControl = new CacheControl();
        cacheControl.setNoCache(true);
        cacheControl.setNoStore(true);

        return Response.fromResponse(response)
                .cacheControl(cacheControl)
                .build();
    }

    private Response buildDefaultResponse(String fileID, FileManager.FilePointer filePointer) {
        final StreamingOutput fileStream = outputStream -> {
            try (FileInputStream fileInputStream = new FileInputStream(filePointer.getFile())) {
                // Use the IOUtils copy method, which internally buffers the files
                IOUtils.copy(fileInputStream, outputStream);
            } catch (FileNotFoundException e) {
                throw new WebApplicationException(String.format("Unable to open file `%s`.`.", fileID), e, Response.Status.INTERNAL_SERVER_ERROR);
            }
            outputStream.flush();
        };

        return Response
                .status(Response.Status.OK)
                .entity(fileStream)
                .header(HttpHeaders.ETAG, filePointer.getChecksum())
                .header(HttpHeaders.CONTENT_LENGTH, filePointer.getFileSize())
                .header(HttpHeaders.LAST_MODIFIED, filePointer.getCreationTime().toInstant().toEpochMilli())
                .build();
    }

    private Response buildRangedRequest(String fileID, File file, RangeHeader range) {
        if (!range.getUnit().equals(ACCEPTED_RANGE_VALUE)) {
            throw new WebApplicationException("Only `bytes` are acceptable as ranges", Response.Status.REQUESTED_RANGE_NOT_SATISFIABLE);
        }
        final long rangeStart = range.getStart() < 0 ? 0 : range.getStart();
        final long rangeEnd = range.getEnd().orElse(rangeStart + CHUNK_SIZE);
        final long len = rangeEnd - rangeStart;

        // If we have a negative range, throw an exception
        if (len < 0) {
            throw new WebApplicationException("Range end cannot be before begin", Response.Status.REQUESTED_RANGE_NOT_SATISFIABLE);
        }

        RandomAccessFile randomAccessFile = null;
        try {
            randomAccessFile = new RandomAccessFile(file, "r");
        } catch (IOException e) {
            throw new WebApplicationException(String.format("Unable to open file `%s`.`.", fileID), e, Response.Status.INTERNAL_SERVER_ERROR);
        }
        try {
            randomAccessFile.seek(rangeStart);
        } catch (IOException e) {
            try {
                randomAccessFile.close();
            } catch (IOException e1) {
                logger.error("Failed to close file after exception", e1);
            }
            throw new WebApplicationException(String.format("Unable to read file `%s`.`.", fileID), e, Response.Status.INTERNAL_SERVER_ERROR);
        }

        final PartialFileStreamer fileStreamer = new PartialFileStreamer((int) len, randomAccessFile);

        final String responseRange = String.format("bytes %d-%d/%d", rangeStart, rangeEnd, file.length());
        return Response
                .status(Response.Status.PARTIAL_CONTENT)
                .entity(fileStreamer)
                .header(HttpHeaders.ACCEPT_RANGES, ACCEPTED_RANGE_VALUE)
                .header(HttpHeaders.CONTENT_RANGE, responseRange)
                // Set the X-Content-Length header, so we can manually override what Jersey does
                .header(X_CONTENT_LENGTH, fileStreamer.getLength())
                .build();
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private boolean returnCachedValue(FileManager.FilePointer filePointer, Optional<String> checksum, Optional<String> modifiedSince) {
        // If we're provided a file checksum, verify it matches, if so, return a 304
        if (checksum.isPresent() && checksum.get().equals(filePointer.getChecksum())) {
            return true;
        }

        if (modifiedSince.isPresent()) {
            // Try to parse out the OffsetDateTime value
            final OffsetDateTime modifiedValue;
            try {
                modifiedValue = Instant.ofEpochMilli(Long.parseLong(modifiedSince.get())).atOffset(ZoneOffset.UTC);
            } catch (DateTimeParseException | NumberFormatException e) {
                logger.error("Unable to parse modified timestamp", e);
                return false;
            }
            // Verify that the creation timestamp is not after the value of the modified header
            return !filePointer.getCreationTime().truncatedTo(ChronoUnit.MILLIS).isAfter(modifiedValue.truncatedTo(ChronoUnit.MILLIS));
        }
        return false;
    }

    private static class PartialFileStreamer implements StreamingOutput {

        private int length;
        private RandomAccessFile raf;
        final byte[] buf = new byte[4096];

        PartialFileStreamer(int length, RandomAccessFile raf) {
            this.length = length;
            this.raf = raf;
        }

        @Override
        public void write(OutputStream outputStream) throws IOException, WebApplicationException {
            try {
                while (length != 0) {
                    int read = raf.read(buf, 0, Math.min(buf.length, length));
                    outputStream.write(buf, 0, read);
                    length -= read;
                }
                outputStream.flush();
            } finally {
                raf.close();
            }
        }

        int getLength() {
            return length;
        }
    }
}
