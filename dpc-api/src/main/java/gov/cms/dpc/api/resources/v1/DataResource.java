package gov.cms.dpc.api.resources.v1;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import gov.cms.dpc.api.auth.OrganizationPrincipal;
import gov.cms.dpc.api.auth.annotations.Authorizer;
import gov.cms.dpc.api.models.RangeHeader;
import gov.cms.dpc.api.resources.AbstractDataResource;
import gov.cms.dpc.common.annotations.NoHtml;
import gov.cms.dpc.api.core.GzipStreamingOutput;
import gov.cms.dpc.common.utils.GzipUtil;
import gov.cms.dpc.api.core.UnGzipStreamingOutput;
import gov.cms.dpc.queue.FileManager;
import gov.cms.dpc.queue.IJobQueue;
import gov.cms.dpc.queue.JobStatus;
import gov.cms.dpc.queue.models.JobQueueBatch;
import io.dropwizard.auth.Auth;
import io.swagger.annotations.*;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import org.apache.commons.io.input.BoundedInputStream;
import org.apache.hc.core5.http.HttpHeaders;
import org.eclipse.jetty.http.HttpStatus;
import org.hl7.fhir.dstu3.model.OperationOutcome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
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
 * Streaming and range logic was taken from here:
 * https://github.com/aruld/jersey-streaming
 */
@Api(tags = { "Bulk Data", "Data" }, authorizations = @Authorization(value = "access_token"))
@Path("/v1/Data")
@Produces("application/ndjson")
public class DataResource extends AbstractDataResource {

    private static final Logger logger = LoggerFactory.getLogger(DataResource.class);
    private static final int CHUNK_SIZE = 1024 * 1024; // Return a maximum of 1MB chunks, but we can modify this later
                                                       // if we need to
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
    @ApiOperation(value = "Metadata for downloading output files.", notes = "Retrieve the metadata for a corresponding `GET` request to download ndjson formatted output files from the server.")
    @ApiResponses({
            @ApiResponse(code = HttpStatus.OK_200, message = "File of newline-delimited JSON FHIR objects", responseHeaders = {
                    @ResponseHeader(name = HttpHeaders.ETAG, description = "SHA256 checksum of file"),
                    @ResponseHeader(name = HttpHeaders.CONTENT_LENGTH, description = "size of file (in bytes)"),
                    @ResponseHeader(name = HttpHeaders.LAST_MODIFIED, description = "creation timestamp of file (in milliseconds since Unix epoch)"),
                    @ResponseHeader(name = HttpHeaders.ACCEPT_RANGES, description = "Accepted HTTP range request (bytes only)")
            }),
            @ApiResponse(code = HttpStatus.NOT_MODIFIED_304, message = "No newer files available"),
            @ApiResponse(code = HttpStatus.UNAUTHORIZED_401, message = "Not authorized to download file"),
            @ApiResponse(code = HttpStatus.GONE_410, message = "File has expired"),
            @ApiResponse(code = HttpStatus.INTERNAL_SERVER_ERROR_500, message = "An error occurred", response = OperationOutcome.class)
    })
    @Override
    public Response exportFileHead(@ApiParam(hidden = true) @Auth OrganizationPrincipal organizationPrincipal,
            @HeaderParam(HttpHeaders.IF_NONE_MATCH) @ApiParam(value = "Download file only if provided SHA256 checksum doesn't match") Optional<String> fileChecksum,
            @HeaderParam(HttpHeaders.IF_MODIFIED_SINCE) @ApiParam(value = "Download file only if provided timestamp (milliseconds since Unix Epoch) is older than file creation timestamp", example = "1575394136") Optional<String> modifiedHeader,
            @PathParam("fileID") @ApiParam(required = true, value = "NDJSON file name", example = "728b270d-d7de-4143-82fe-d3ccd92cebe4-1-coverage.ndjson") @NoHtml String fileID) {
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
    @ApiOperation(value = "Download output files.", notes = "Download ndjson formatted output files from the server. " +
            "This endpoint supports returning partial results when the `" + HttpHeaders.RANGE + "` header is provided. "
            +
            "<p>This endpoint will return a `" + HttpStatus.NOT_MODIFIED_304 + "` response if the `"
            + HttpHeaders.IF_MODIFIED_SINCE + "` or `" + HttpHeaders.IF_NONE_MATCH
            + "` headers are provided and match an existing file.")
    @ApiResponses({
            @ApiResponse(code = HttpStatus.OK_200, message = "File of newline-delimited JSON FHIR objects", responseHeaders = {
                    @ResponseHeader(name = HttpHeaders.ETAG, description = "SHA256 checksum of file"),
                    @ResponseHeader(name = HttpHeaders.CONTENT_LENGTH, description = "size of file (in bytes)"),
                    @ResponseHeader(name = HttpHeaders.LAST_MODIFIED, description = "creation timestamp of file (in milliseconds since Unix epoch)")
            }),
            @ApiResponse(code = HttpStatus.PARTIAL_CONTENT_206, message = "Returning a partial byte range of file", responseHeaders = {
                    @ResponseHeader(name = HttpHeaders.ACCEPT_RANGES, description = "Accepted HTTP range request (bytes only)"),
                    @ResponseHeader(name = HttpHeaders.CONTENT_RANGE, description = "HTTP range response (e.g. bytes=1-1234/{total file size}"),
            }),
            @ApiResponse(code = HttpStatus.NOT_MODIFIED_304, message = "No newer files available"),
            @ApiResponse(code = HttpStatus.UNAUTHORIZED_401, message = "Not authorized to download file"),
            @ApiResponse(code = HttpStatus.RANGE_NOT_SATISFIABLE_416, message = "Range request is invalid"),
            @ApiResponse(code = HttpStatus.INTERNAL_SERVER_ERROR_500, message = "An error occurred", response = OperationOutcome.class)
    })
    public Response downloadExportFile(@ApiParam(hidden = true) @Auth OrganizationPrincipal organizationPrincipal,
            @HeaderParam(HttpHeaders.RANGE) @ApiParam(value = "HTTP Range request for partial file download", example = "bytes=0-1234") RangeHeader rangeHeader,
            @HeaderParam(HttpHeaders.IF_NONE_MATCH) @ApiParam(value = "Download file only if provided SHA256 checksum doesn't match") Optional<String> fileChecksum,
            @HeaderParam(HttpHeaders.IF_MODIFIED_SINCE) @ApiParam(value = "Download file only if provided timestamp (milliseconds since Unix Epoch) is older than file creation timestamp", example = "1575394136") Optional<String> modifiedHeader,
            @HeaderParam(HttpHeaders.ACCEPT_ENCODING) @ApiParam(value = "Accept-Encoding header", example = "gzip") Optional<String> acceptEncoding,
            @PathParam("fileID") @ApiParam(required = true, value = "NDJSON file name", example = "728b270d-d7de-4143-82fe-d3ccd92cebe4-1-coverage.ndjson") @NoHtml String fileID) {

        final FileManager.FilePointer filePointer = this.manager.getFile(organizationPrincipal.getID(), fileID);

        // If job is expired, the files should no longer be accessible
        List<JobQueueBatch> batches = queue.getJobBatches(filePointer.getJobID());
        Set<JobStatus> jobStatusSet = batches.stream().map(JobQueueBatch::getStatus).collect(Collectors.toSet());
        if (jobStatusSet.size() == 1 && jobStatusSet.contains(JobStatus.COMPLETED)) {
            OffsetDateTime lastCompleteTime = JobResource.getLatestBatchCompleteTime(batches);

            if (lastCompleteTime
                    .isBefore(OffsetDateTime.now(ZoneOffset.UTC).minusHours(JobResource.JOB_EXPIRATION_HOURS))) {
                return Response.status(Response.Status.GONE).build();
            }
        }

        // If we're provided a file checksum, verify it matches, if so, return a 304
        if (returnCachedValue(filePointer, fileChecksum, modifiedHeader)) {
            return Response.status(Response.Status.NOT_MODIFIED).build();
        }

        final Response response;

        // Process the range request and return a partial stream, but only if they
        // request bytes, ignore everything else
        boolean compressResponse = shouldGzipCompress(acceptEncoding);
        if (rangeHeader != null) {
            response = buildRangedRequest(fileID, filePointer, rangeHeader);
        } else {
            // Return a non-ranged streamed response if the requester doesn't actually send
            // the range header, or if we don't understand the range unit
            response = buildDefaultResponse(fileID, filePointer, compressResponse);
        }

        // Set the cache control headers to make sure the file isn't retained in transit
        final CacheControl cacheControl = new CacheControl();
        cacheControl.setNoCache(true);
        cacheControl.setNoStore(true);
        Response.ResponseBuilder responseBuilder = Response.fromResponse(response)
            .cacheControl(cacheControl);

        return responseBuilder.build();
    }

    private Response buildDefaultResponse(String fileID, FileManager.FilePointer filePointer, boolean compressResponse) {
        FileInputStream fileInputStream;
        try {
            fileInputStream = new FileInputStream(filePointer.getFile());
        } catch (IOException e) {
            throw new WebApplicationException(String.format("Unable to open file `%s`.`.", fileID), e,
                    Response.Status.INTERNAL_SERVER_ERROR);
        }

        StreamingOutput streamingOutput;
        if (compressResponse) {
            streamingOutput = new GzipStreamingOutput(fileInputStream, filePointer.isCompressed());
        } else {
            streamingOutput = new UnGzipStreamingOutput(fileInputStream, filePointer.isCompressed());
        }

        Response.ResponseBuilder builder = Response
            .status(Response.Status.OK)
            .entity(streamingOutput)
            .header(HttpHeaders.ETAG, filePointer.getChecksum())
            .header(HttpHeaders.LAST_MODIFIED, filePointer.getCreationTime().toInstant().toEpochMilli());
        if (compressResponse) {
            builder.encoding("gzip");
        }

        return builder.build();
    }

    private Response buildRangedRequest(String fileID, FileManager.FilePointer filePointer, RangeHeader range) {
        if (!range.getUnit().equals(ACCEPTED_RANGE_VALUE)) {
            throw new WebApplicationException("Only `bytes` are acceptable as ranges",
                    Response.Status.REQUESTED_RANGE_NOT_SATISFIABLE);
        }
        final long rangeStart = range.getStart() < 0 ? 0 : range.getStart();
        final long rangeEnd = range.getEnd().orElse(rangeStart + CHUNK_SIZE);
        final long len = rangeEnd - rangeStart;

        // If we have a negative range, throw an exception
        if (len < 0) {
            throw new WebApplicationException("Range end cannot be before begin",
                    Response.Status.REQUESTED_RANGE_NOT_SATISFIABLE);
        }

        // We need to get an uncompressed input stream, then move our cursor to the offset we want. We can't use a
        // RandomAccessFile because our file is more than likely compressed.
        try {
            InputStream uncompressedInputStream = filePointer.getUncompressedInputStream();

            // Build a new stream that starts at the beginning of our range and ends at the
            // end
            uncompressedInputStream.skipNBytes(rangeStart);
            BoundedInputStream boundedInputStream = BoundedInputStream.builder()
                    .setInputStream(uncompressedInputStream)
                    .setMaxCount(len)
                    .get();

            // The default DropWizard behavior is to not gzip compress responses when a range is requested, so we're
            // duplicating that here and we only create non-zipped responses.
            StreamingOutput streamingOutput = new UnGzipStreamingOutput(boundedInputStream, false);

            final String responseRange = String.format("bytes %d-%d/%d", rangeStart, rangeEnd, len);
            return Response
                .status(Response.Status.PARTIAL_CONTENT)
                .entity(streamingOutput)
                .header(HttpHeaders.ACCEPT_RANGES, ACCEPTED_RANGE_VALUE)
                .header(HttpHeaders.CONTENT_RANGE, responseRange)
                // Set the X-Content-Length header, so we can manually override what Jersey does
                .header(X_CONTENT_LENGTH, len)
                .build();
        } catch (IOException e) {
            throw new WebApplicationException(String.format("Unable to open file `%s`.`.", fileID), e,
                    Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private boolean returnCachedValue(FileManager.FilePointer filePointer, Optional<String> checksum,
            Optional<String> modifiedSince) {
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
            // Verify that the creation timestamp is not after the value of the modified
            // header
            return !filePointer.getCreationTime().truncatedTo(ChronoUnit.MILLIS)
                    .isAfter(modifiedValue.truncatedTo(ChronoUnit.MILLIS));
        }
        return false;
    }

    // True if we should gzip our response, false otherwise
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private boolean shouldGzipCompress(Optional<String> acceptEncoding) {
        return (acceptEncoding.isPresent()) && (acceptEncoding.get().toLowerCase().contains(GzipUtil.GZIP));
    }
}
