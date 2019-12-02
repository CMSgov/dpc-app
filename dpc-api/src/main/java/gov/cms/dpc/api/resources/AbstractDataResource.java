package gov.cms.dpc.api.resources;

import gov.cms.dpc.api.auth.OrganizationPrincipal;
import gov.cms.dpc.api.models.RangeHeader;
import gov.cms.dpc.fhir.FHIRMediaTypes;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Optional;

@Produces(FHIRMediaTypes.FHIR_NDJSON)
@Path("/Data")
public abstract class AbstractDataResource {

    @Path("/{fileID}/")
    @GET
    public abstract Response export(OrganizationPrincipal organizationPrincipal, RangeHeader range, String fileID);
}
