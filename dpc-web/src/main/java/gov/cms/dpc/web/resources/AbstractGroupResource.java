package gov.cms.dpc.web.resources;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

@Produces("application/fhir+json")
@Path("/Group")
public abstract class AbstractGroupResource {

    public AbstractGroupResource() {
        // Not used
    }

    @Path("/{providerID}/$export")
    @GET
    public abstract Response export(@PathParam("providerID") String groupID);
}
