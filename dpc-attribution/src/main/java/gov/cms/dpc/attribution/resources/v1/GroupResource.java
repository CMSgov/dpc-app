package gov.cms.dpc.attribution.resources.v1;

import gov.cms.dpc.attribution.resources.AbstractGroupResource;

import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

@Path("/v1/")
public class GroupResource extends AbstractGroupResource {

    GroupResource() {
//        Not used
    }

    @Path("/{groupID}")
    @Override
    public Response getAttributedPatients(String groupID) {
        return null;
    }
}
