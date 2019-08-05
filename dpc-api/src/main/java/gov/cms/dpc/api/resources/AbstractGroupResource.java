package gov.cms.dpc.api.resources;


import gov.cms.dpc.api.auth.OrganizationPrincipal;
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

    @POST
    public abstract Response createRoster(OrganizationPrincipal organizationPrincipal, Group attributionRoster);

    @Path("/{providerID}/$export")
    @GET
    public abstract Response export(OrganizationPrincipal organizationPrincipal,
                                    @PathParam("providerID") String groupID,
                                    @QueryParam("_type") String resourceTypes,
                                    @QueryParam("_outputFormat") String outputFormat,
                                    @QueryParam("_since") String since);
}
