package gov.cms.dpc.resources.v1;

import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

@Produces("application/fhir+json")
public class GroupResource {

    @Inject
    public GroupResource() {

    }

    @Path("/{groupID}/$export")
    @POST
    public String export(@PathParam("groupID") Integer groupID) {
        return String.format("Exporting for: %d", groupID);
    }


}
