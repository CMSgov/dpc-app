package gov.cms.dpc.api.resources;

import gov.cms.dpc.fhir.FHIRMediaTypes;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

@Produces(FHIRMediaTypes.FHIR_NDJSON)
@Path("/Data")
public abstract class AbstractDataResource {

    @Path("/{fileID}/")
    @GET
    public abstract Response export(String fileID);
}
