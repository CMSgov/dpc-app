package gov.cms.dpc.api.resources;


import gov.cms.dpc.api.auth.OrganizationPrincipal;
import gov.cms.dpc.fhir.annotations.FHIR;
import gov.cms.dpc.fhir.annotations.Profiled;
import gov.cms.dpc.fhir.validations.profiles.AttestationProfile;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Group;
import org.hl7.fhir.dstu3.model.Provenance;

import javax.validation.Valid;
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
    public abstract Response createRoster(OrganizationPrincipal organizationPrincipal, @Valid @Profiled(profile = AttestationProfile.PROFILE_URI) Provenance rosterAttestation, Group attributionRoster);

    @GET
    public abstract Bundle rosterSearch(OrganizationPrincipal organizationPrincipal, String providerNPI, String patientID);

    @GET
    @Path("/{rosterID}")
    public abstract Group getRoster(UUID rosterID);

    @PUT
    @Path("/{rosterID}")
    public abstract Group updateRoster(UUID rosterID, @Valid @Profiled(profile = AttestationProfile.PROFILE_URI) Provenance rosterAttestation, Group rosterUpdate);

    @POST
    @Path("/{rosterID}/$add")
    public abstract Group addRosterMembers(UUID rosterID, @Valid @Profiled(profile = AttestationProfile.PROFILE_URI) Provenance rosterAttestation, Group groupUpdate);

    @POST
    @Path("/{rosterID}/$remove")
    public abstract Group removeRosterMembers(UUID rosterID, Group groupUpdate);

    @DELETE
    @Path("/{rosterID}")
    public abstract Response deleteRoster(UUID rosterID);

    @Path("/{rosterID}/$export")
    @GET
    public abstract Response export(OrganizationPrincipal organizationPrincipal,
                                    @PathParam("rosterID") String rosterID,
                                    @QueryParam("_type") String resourceTypes,
                                    @QueryParam("_outputFormat") String outputFormat,
                                    @QueryParam("_since") String since);
}
