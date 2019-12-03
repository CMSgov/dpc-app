package gov.cms.dpc.api.resources.v1;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import gov.cms.dpc.api.auth.OrganizationPrincipal;
import gov.cms.dpc.api.core.FileManager;
import gov.cms.dpc.api.models.RangeHeader;
import gov.cms.dpc.api.resources.AbstractDataResource;
import io.dropwizard.auth.Auth;
import io.swagger.annotations.*;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHeaders;
import org.hl7.fhir.dstu3.model.OperationOutcome;
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
import java.util.Optional;

import static gov.cms.dpc.fhir.dropwizard.filters.StreamingContentSizeFilter.X_CONTENT_LENGTH;

/**
 * Streaming and range logic was taken from here: https://github.com/aruld/jersey-streaming
 */
@Api(tags = {"Bulk Data", "Data"}, authorizations = @Authorization(value = "apiKey"))
@Path("/v1/Data")
public class DataResource extends AbstractDataResource {

    private static final Logger logger = LoggerFactory.getLogger(DataResource.class);
    private static final int CHUNK_SIZE = 1024 * 1024; // Return a maximum of 1MB chunks, but we can modify this later if we need to

    private final FileManager manager;

    @Inject
    public DataResource(FileManager manager) {
        this.manager = manager;
    }

    @Path("/{fileID}.ndjson")
    @HEAD
    @Timed
    @ExceptionMetered
    @ApiOperation(value = "Do head things")
    @Override
    public Response exportHead(@ApiParam(hidden = true) @Auth OrganizationPrincipal organizationPrincipal,
                               @HeaderParam(HttpHeaders.RANGE) RangeHeader rangeHeader,
                               @HeaderParam(HttpHeaders.IF_NONE_MATCH) Optional<String> fileChecksum,
                               @HeaderParam(HttpHeaders.IF_MODIFIED_SINCE) Optional<String> modifiedHeader,
                               @PathParam("fileID") String fileID) {
        final FileManager.FilePointer filePointer = this.manager.getFile(organizationPrincipal.getID(), fileID);

        if (returnCachedValue(filePointer, fileChecksum, modifiedHeader)) {
            return Response.status(Response.Status.NOT_MODIFIED).build();
        }

        return Response.ok()
                .header(HttpHeaders.ETAG, filePointer.getChecksum())
                .header(HttpHeaders.CONTENT_LENGTH, filePointer.getFileSize())
                .header(HttpHeaders.LAST_MODIFIED, filePointer.getCreationTime().toInstant().toEpochMilli())
                .build();
    }

    @Override
    @Path("/{fileID}.ndjson")
    @GET
    @Timed
    @ExceptionMetered
    @ApiOperation(value = "Download output files.")
    @ApiResponses({
            @ApiResponse(code = 200, message = "File of newline-delimited JSON FHIR objects"),
            @ApiResponse(code = 500, message = "An error occurred", response = OperationOutcome.class)
    })
    public Response export(@ApiParam(hidden = true) @Auth OrganizationPrincipal organizationPrincipal,
                           @HeaderParam(HttpHeaders.RANGE) RangeHeader rangeHeader,
                           @HeaderParam(HttpHeaders.IF_NONE_MATCH) Optional<String> fileChecksum,
                           @HeaderParam(HttpHeaders.IF_MODIFIED_SINCE) Optional<String> modifiedHeader,
                           @PathParam("fileID") String fileID) {

        final FileManager.FilePointer filePointer = this.manager.getFile(organizationPrincipal.getID(), fileID);

        // If we're provided a file checksum, verify it matches, if so, return a 304
        if (returnCachedValue(filePointer, fileChecksum, modifiedHeader)) {
            return Response.status(Response.Status.NOT_MODIFIED).build();
        }

        final Response response;

        // Process the range request and return a partial stream, but only if they request bytes, ignore everything else
        if (rangeHeader != null && rangeHeader.getUnit().equals("bytes")) {
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
        final Long rangeStart = range.getStart() < 0 ? 0 : range.getStart();
        final Long rangeEnd = range.getEnd().orElse(rangeStart + CHUNK_SIZE);
        final long len = rangeEnd - rangeStart;

        // If we have a negative range, throw an exception
        if (len < 0) {
            throw new WebApplicationException("Range end cannot be before begin", Response.Status.REQUESTED_RANGE_NOT_SATISFIABLE);
        }

        try {
            final RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r");
            randomAccessFile.seek(rangeStart);

            final PartialFileStreamer fileStreamer = new PartialFileStreamer((int) len, randomAccessFile);

            final String responseRange = String.format("bytes %d-%d/%d", rangeStart, rangeEnd, file.length());
            return Response
                    .status(Response.Status.PARTIAL_CONTENT)
                    .entity(fileStreamer)
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .header(HttpHeaders.CONTENT_RANGE, responseRange)
                    // Set the X-Content-Length header, so we can manually override what Jersey does
                    .header(X_CONTENT_LENGTH, fileStreamer.getLength())
                    .build();
        } catch (IOException e) {
            throw new WebApplicationException(String.format("Unable to open file `%s`.`.", fileID), e, Response.Status.INTERNAL_SERVER_ERROR);
        }
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
