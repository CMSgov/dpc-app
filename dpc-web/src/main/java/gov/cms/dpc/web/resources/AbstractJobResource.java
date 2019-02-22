package gov.cms.dpc.web.resources;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

@Produces("application/json")
@Path("/Jobs")
public abstract class AbstractJobResource {

    protected AbstractJobResource() {
        // Not used
    }

    @Path("/{jobID}")
    @GET
    public abstract Response checkJobStatus(String jobID);
}
