package gov.cms.dpc.api.resources.r4;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import gov.cms.dpc.api.auth.OrganizationPrincipal;
import gov.cms.dpc.api.resources.AbstractResourceWithExport;
import gov.cms.dpc.common.annotations.NoHtml;
import gov.cms.dpc.fhir.annotations.FHIR;
import gov.cms.dpc.fhir.annotations.FHIRAsync;
import gov.cms.dpc.fhir.annotations.Profiled;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import org.hl7.fhir.r4.model.Group;
import org.hl7.fhir.r4.model.Provenance;

import java.util.UUID;

// Can be removed when v1 is deprecated

@FHIR
@Path("/Group")
public abstract class AbstractGroupResource extends AbstractResourceWithExport {

    protected AbstractGroupResource(IGenericClient client) {
        super(client);
    }

    @POST
    public abstract Response createRoster(OrganizationPrincipal organizationPrincipal, @Valid @Profiled Provenance rosterAttestation, Group attributionRoster);

    @GET
    @Path("/{rosterID}")
    public abstract Group getRoster(UUID rosterID);

    @PUT
    @Path("/{rosterID}")
    public abstract Group updateRoster(OrganizationPrincipal organizationPrincipal, UUID rosterID, @Valid @Profiled Provenance rosterAttestation, Group rosterUpdate);

    @POST
    @Path("/{rosterID}/$member-add")
    public abstract Group addRosterMembers(OrganizationPrincipal organizationPrincipal, UUID rosterID, @Valid @Profiled Provenance rosterAttestation, Group groupUpdate);

    @POST
    @Path("/{rosterID}/$member-remove")
    public abstract Group removeRosterMembers(UUID rosterID, Group groupUpdate);

    @GET
    @Path("/{rosterID}/$export")
    @FHIRAsync
    public abstract Response export(OrganizationPrincipal organizationPrincipal,
                                    @NoHtml String rosterID,
                                    @NoHtml String resourceTypes,
                                    @NoHtml String outputFormat,
                                    @NoHtml String since,
                                    @NoHtml String typeFilter,
                                    @Valid String prefer,
                                    HttpServletRequest httpServletRequest);
}
