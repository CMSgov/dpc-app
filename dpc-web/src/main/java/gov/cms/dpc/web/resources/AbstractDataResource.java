package gov.cms.dpc.web.resources;

import gov.cms.dpc.fhir.FHIRMediaTypes;
import gov.cms.dpc.fhir.annotations.FHIR;

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
