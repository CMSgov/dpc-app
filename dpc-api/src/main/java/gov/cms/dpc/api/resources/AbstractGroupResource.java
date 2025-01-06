package gov.cms.dpc.api.resources;


import gov.cms.dpc.api.auth.OrganizationPrincipal;
import gov.cms.dpc.common.annotations.NoHtml;
import gov.cms.dpc.fhir.annotations.FHIR;
import gov.cms.dpc.fhir.annotations.Profiled;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Group;
import org.hl7.fhir.dstu3.model.Provenance;

import java.util.UUID;

@FHIR
@Path("/Group")
public abstract class AbstractGroupResource extends AbstractResourceWithSince {

    protected AbstractGroupResource() {
        // Not used
    }

    @POST
    public abstract Response createRoster(OrganizationPrincipal organizationPrincipal, @Valid @Profiled Provenance rosterAttestation, Group attributionRoster);

    @GET
    public abstract Bundle rosterSearch(OrganizationPrincipal organizationPrincipal, @NoHtml String providerNPI, @NoHtml String patientID);

    @GET
    @Path("/{rosterID}")
    public abstract Group getRoster(UUID rosterID);

    @PUT
    @Path("/{rosterID}")
    public abstract Group updateRoster(OrganizationPrincipal security, UUID rosterID, @Valid @Profiled Provenance rosterAttestation, Group rosterUpdate);

    @POST
    @Path("/{rosterID}/$add")
    public abstract Group addRosterMembers(OrganizationPrincipal organizationPrincipal, UUID rosterID, @Valid @Profiled Provenance rosterAttestation, Group rosterUpdate);

    @POST
    @Path("/{rosterID}/$remove")
    public abstract Group removeRosterMembers(UUID rosterID, Group groupUpdate);

    @DELETE
    @Path("/{rosterID}")
    public abstract Response deleteRoster(UUID rosterID);

    @Path("/{rosterID}/$export")
    @GET
    public abstract Response export(OrganizationPrincipal organizationPrincipal,
                                    @NoHtml String rosterID,
                                    @NoHtml String resourceTypes,
                                    @NoHtml String outputFormat,
                                    @NoHtml String since,
                                    @Valid String Prefer,
                                    HttpServletRequest httpServletRequest);
}
