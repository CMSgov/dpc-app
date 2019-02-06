package gov.cms.dpc.resources.v1;

import ca.uhn.fhir.parser.IParser;
import gov.cms.dpc.resources.AbstractGroupResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.UUID;

public class GroupResource extends AbstractGroupResource {

    private static final Logger logger = LoggerFactory.getLogger(GroupResource.class);

    private final IParser parser;

    @Inject
    public GroupResource(IParser jsonParser) {
        // Not used
        this.parser = jsonParser;
    }

    /**
     * Begin export process for the given provider
     * On success, returns a {@link org.eclipse.jetty.http.HttpStatus#NO_CONTENT_204} response along with a {@link org.hl7.fhir.r4.model.OperationOutcome} result.
     * The `Content-Location` header contains the URI to call when
     *
     * @param providerID {@link String} ID of provider to retrieve data for
     * @return - {@link org.hl7.fhir.r4.model.OperationOutcome} specifying whether or not the request was successful.
     */
    @Override
    @Path("/{providerID}/$export")
    @GET // Need this here, since we're using a path param
    public Response export(@PathParam("providerID") String providerID) {
        logger.debug("Exporting data for provider: {}", providerID);
        return Response.status(Response.Status.NO_CONTENT)
                .contentLocation(URI.create("http://localhost:3002/jobs/" + UUID.randomUUID())).build();
    }
}
