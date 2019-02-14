package gov.cms.dpc.attribution.resources;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;

@Path("/Group")
@Produces()
@Consumes()
public abstract class AbstractGroupResource {

    protected AbstractGroupResource() {
        // Not used
    }

    @GET
    @Path("/{groupID}")
    public abstract Response getAttributedPatients(@PathParam("groupID") String groupID, HttpServletRequest req);
}
