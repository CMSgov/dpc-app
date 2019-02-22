package gov.cms.dpc.web.resources.v1;

import gov.cms.dpc.common.annotations.ExportPath;
import gov.cms.dpc.web.resources.AbstractDataResource;
import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

public class DataResource extends AbstractDataResource {

    private static final Logger logger = LoggerFactory.getLogger(DataResource.class);

    private final String fileLocation;

    @Inject
    public DataResource(@ExportPath String exportPath) {
        this.fileLocation = exportPath;
    }


    @Override
    @Path("/{fileID}/")
    @GET
    public Response export(@PathParam("fileID") String fileID) {
        final StreamingOutput fileStream = new StreamingOutput() {
            @Override
            public void write(OutputStream outputStream) throws IOException, WebApplicationException {
                final java.nio.file.Path path = Paths.get(String.format("%s/%s.ndjson", fileLocation, fileID));
                logger.debug("Streaming file {}", path.toString());
                final byte[] data = Files.readAllBytes(path);
                outputStream.write(data);
                outputStream.flush();
            }
        };

        return Response.ok(fileStream).build();
    }
}
