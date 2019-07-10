package gov.cms.dpc.api.resources;


import gov.cms.dpc.fhir.annotations.FHIR;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

@FHIR
@Path("/Group")
public abstract class AbstractGroupResource {

    protected AbstractGroupResource() {
        // Not used
    }

    @Path("/{providerID}/$export")
    @GET
    public abstract Response export(@PathParam("providerID") String groupID,
                                    @QueryParam("_type") String resourceTypes,
                                    @QueryParam("_outputFormat") String outputFormat,
                                    @QueryParam("_since") String since);
}
