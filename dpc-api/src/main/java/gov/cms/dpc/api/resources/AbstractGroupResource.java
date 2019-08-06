package gov.cms.dpc.api.resources;


import gov.cms.dpc.api.auth.OrganizationPrincipal;
import gov.cms.dpc.fhir.annotations.FHIR;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Group;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.UUID;

@FHIR
@Path("/Group")
public abstract class AbstractGroupResource {

    protected AbstractGroupResource() {
        // Not used
    }

    @POST
    public abstract Response createRoster(OrganizationPrincipal organizationPrincipal, Group attributionRoster);

    @GET
    public abstract Bundle rosterSearch(OrganizationPrincipal organizationPrincipal, String providerNPI, String patientID);

    @GET
    @Path("/{rosterID}")
    public abstract Group getRoster(UUID rosterID);

    @PUT
    @Path("/{rosterID}")
    public abstract Group updateRoster(UUID rosterID, Group rosterUpdate);

    @DELETE
    @Path("/{rosterID}")
    public abstract Response deleteRoster(UUID rosterID);

    @Path("/{providerID}/$export")
    @GET
    public abstract Response export(OrganizationPrincipal organizationPrincipal,
                                    @PathParam("providerID") String groupID,
                                    @QueryParam("_type") String resourceTypes,
                                    @QueryParam("_outputFormat") String outputFormat,
                                    @QueryParam("_since") String since);
}
