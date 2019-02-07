package gov.cms.dpc.web.resources;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/")
@Produces(MediaType.TEXT_PLAIN)
public class TestResource {

    @Inject
    public TestResource() {
        // Not used
    }

    @GET
    public Response base() {
        return Response.status(Response.Status.OK).entity("Hello there!").build();
    }
}
