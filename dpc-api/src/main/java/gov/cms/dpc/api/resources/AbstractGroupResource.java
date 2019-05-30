package gov.cms.dpc.api.resources;


import gov.cms.dpc.fhir.annotations.FHIR;
import org.hl7.fhir.dstu3.model.Group;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

@FHIR
@Path("/Group")
public abstract class AbstractGroupResource {

    protected AbstractGroupResource() {
        // Not used
    }

    @Path("/{providerID}/$export")
    @GET
    public abstract Response export(@PathParam("providerID") String groupID, @QueryParam("_type") String resourceTypes);

    @Path("/{providerID}/$export")
    @POST
    public abstract Response export(Group exportGroup, @PathParam("providerID") String groupID, @QueryParam("_type") String resourceTypes);
}
