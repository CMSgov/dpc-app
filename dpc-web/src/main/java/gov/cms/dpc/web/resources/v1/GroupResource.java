package gov.cms.dpc.web.resources.v1;

import ca.uhn.fhir.parser.IParser;
import gov.cms.dpc.common.models.JobModel;
import gov.cms.dpc.queue.JobQueue;
import gov.cms.dpc.web.core.FHIRMediaTypes;
import gov.cms.dpc.web.core.annotations.FHIR;
import gov.cms.dpc.web.resources.AbstractGroupResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.UUID;


public class GroupResource extends AbstractGroupResource {

    private static final Logger logger = LoggerFactory.getLogger(GroupResource.class);

    private final IParser parser;
    private final JobQueue queue;

    @Inject
    public GroupResource(IParser jsonParser, JobQueue queue) {
        this.parser = jsonParser;
        this.queue = queue;
    }

    /**
     * Begin export process for the given provider
     * On success, returns a {@link org.eclipse.jetty.http.HttpStatus#NO_CONTENT_204} response along with a {@link org.hl7.fhir.r4.model.OperationOutcome} result.
     * The `Content-Location` header contains the URI to call when
     *
     * @param providerID {@link String} ID of provider to retrieve data for
     * @param req
     * @return - {@link org.hl7.fhir.r4.model.OperationOutcome} specifying whether or not the request was successful.
     */
    @Override
    @Path("/{providerID}/$export")
    @GET // Need this here, since we're using a path param
    public Response export(@PathParam("providerID") String providerID, @Context HttpServletRequest req) {
        logger.debug("Exporting data for provider: {}", providerID);

        // Generate a job ID and submit it to the queue
        final UUID jobID = UUID.randomUUID();

        this.queue.submitJob(jobID, new JobModel(providerID));

        return Response.status(Response.Status.NO_CONTENT)
                .contentLocation(URI.create("http://localhost:3002/v1/Jobs/" + jobID)).build();
    }

    @GET
    @Path("/test")
    public String getTest() {
        return "group test";
    }
}
