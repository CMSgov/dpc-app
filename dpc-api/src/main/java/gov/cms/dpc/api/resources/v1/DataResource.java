package gov.cms.dpc.api.resources.v1;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import gov.cms.dpc.api.auth.OrganizationPrincipal;
import gov.cms.dpc.api.core.FileManager;
import gov.cms.dpc.api.resources.AbstractDataResource;
import io.dropwizard.auth.Auth;
import io.swagger.annotations.*;
import org.apache.commons.io.IOUtils;
import org.hl7.fhir.dstu3.model.OperationOutcome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Api(tags = {"Bulk Data", "Data"}, authorizations = @Authorization(value = "apiKey"))
public class DataResource extends AbstractDataResource {

    private static final Logger logger = LoggerFactory.getLogger(DataResource.class);

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
    public Response export(@ApiParam(hidden = true) @Auth OrganizationPrincipal organizationPrincipal, @PathParam("fileID") String fileID) {
        final File file = this.manager.getFile(fileID);
        final StreamingOutput fileStream = outputStream -> {
            try (FileInputStream fileInputStream = new FileInputStream(file)) {
                // Use the IOUtils copy method, which internally buffers the files
                IOUtils.copy(fileInputStream, outputStream);
            } catch (FileNotFoundException e) {
                throw new WebApplicationException(String.format("Unable to open file `%s`.`.", fileID), e, Response.Status.INTERNAL_SERVER_ERROR);
            }
            outputStream.flush();
        };

        // Set the cache control headers to make sure the file isn't retained in transit
        final CacheControl cacheControl = new CacheControl();
        cacheControl.setNoCache(true);
        cacheControl.setNoStore(true);

        return Response
                .ok(fileStream)
                .cacheControl(cacheControl)
                .header(HttpHeaders.ETAG, OffsetDateTime.now(ZoneOffset.UTC).toString())
                .header(HttpHeaders.CONTENT_LENGTH, file.length())
                .build();
    }
}
