package gov.cms.dpc.api.resources.v1;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import gov.cms.dpc.api.auth.OrganizationPrincipal;
import gov.cms.dpc.api.core.FileManager;
import gov.cms.dpc.api.resources.AbstractDataResource;
import io.dropwizard.auth.Auth;
import io.swagger.annotations.*;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.HttpHeaders;
import org.hl7.fhir.dstu3.model.OperationOutcome;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.*;

/**
 * Streaming and range logic was taken from here: https://github.com/aruld/jersey-streaming
 */
@Api(tags = {"Bulk Data", "Data"}, authorizations = @Authorization(value = "apiKey"))
@Path("/v1/Data")
public class DataResource extends AbstractDataResource {

    private static final int CHUNK_SIZE = 1024 * 1024; // Return a maximum of 1MB chunks, but we can modify this later if we need to

    private final FileManager manager;

    @Inject
    public DataResource(FileManager manager) {
        this.manager = manager;
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
    public Response export(@ApiParam(hidden = true) @Auth OrganizationPrincipal organizationPrincipal, @HeaderParam(HttpHeaders.RANGE) String range, @PathParam("fileID") String fileID) {

        final FileManager.FilePointer filePointer = this.manager.getFile(organizationPrincipal.getID(), fileID);

        final Response response;
        // Return a non-ranged streamed response if the requester doesn't actually send the range header
        if (range == null) {
            response = buildDefaultResponse(fileID, filePointer);
        } else { // Process the range request and return a partial stream
            response = buildRangedRequest(fileID, filePointer.getFile(), range);
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
                .build();
    }

    private Response buildRangedRequest(String fileID, File file, String range) {
        final Pair<Long, Long> rangePair = parseRangeHeader(range, file.length());
        final String responseRange = String.format("bytes %d-%d/%d", rangePair.getLeft(), rangePair.getRight(), file.length());

        try {
            final RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r");
            randomAccessFile.seek(rangePair.getLeft());
            final long len = rangePair.getRight() - rangePair.getLeft();
            final PartialFileStreamer fileStreamer = new PartialFileStreamer((int) len, randomAccessFile);

            return Response
                    .status(Response.Status.PARTIAL_CONTENT)
                    .entity(fileStreamer)
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .header(HttpHeaders.CONTENT_RANGE, responseRange)
                    .header(HttpHeaders.CONTENT_LENGTH, fileStreamer.getLength())
                    .build();

        } catch (IOException e) {
            throw new WebApplicationException(String.format("Unable to open file `%s`.`.", fileID), e, Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    private Pair<Long, Long> parseRangeHeader(String range, long fileLength) {
        // Split the range request
        final String[] ranges = range.split("=", -1)[1].split("-", -1);
        final long from = Long.parseLong(ranges[0]);

         /*
          Chunk media if the range upper bound is unspecified. Chrome, Opera sends "bytes=0-"
         */
        long to = CHUNK_SIZE + from;
        if (to >= fileLength) {
            to = fileLength - 1;
        }
        // If we're given a to range, use that directly
        // Can they give us a value larger than the actual byte size?
        if (ranges.length == 2 && !ranges[1].equals("")) {
            to = Long.parseLong(ranges[1]);
        }

        return Pair.of(from, to);
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
