package gov.cms.dpc.api.resources;

import gov.cms.dpc.api.auth.OrganizationPrincipal;
import gov.cms.dpc.api.models.RangeHeader;
import gov.cms.dpc.fhir.FHIRMediaTypes;

import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.time.OffsetDateTime;
import java.util.Optional;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
@Produces(FHIRMediaTypes.FHIR_NDJSON)
@Path("/Data")
public abstract class AbstractDataResource {

    @Path("/{fileID}/")
    @HEAD
    public abstract Response exportHead(OrganizationPrincipal organizationPrincipal, RangeHeader range, Optional<String> fileChecksum, Optional<String> modifiedHeader, String fileID);

    @Path("/{fileID}/")
    @GET
    public abstract Response export(OrganizationPrincipal organizationPrincipal, RangeHeader range, Optional<String> fileChecksum, Optional<String> modifiedHeader, String fileID);
}
