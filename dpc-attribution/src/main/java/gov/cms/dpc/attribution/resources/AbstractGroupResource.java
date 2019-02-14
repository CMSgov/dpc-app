package gov.cms.dpc.attribution.resources;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

@Path("/Group")
public abstract class AbstractGroupResource {

    @GET
    @Path("/{groupID}")
    public abstract Response getAttributedPatients(String groupID);
}
