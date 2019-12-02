package gov.cms.dpc.api.resources;

import gov.cms.dpc.fhir.annotations.FHIR;
import gov.cms.dpc.fhir.annotations.Profiled;
import gov.cms.dpc.fhir.validations.profiles.OrganizationProfile;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Organization;

import javax.validation.Valid;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import java.util.UUID;

@Path("/Organization")
@FHIR
public abstract class AbstractOrganizationResource {

    @POST
    @Path("/$submit")
    public abstract Organization submitOrganization(Bundle organizationBundle);

    @GET
    @Path("/{organizationID}")
    public abstract Organization getOrganization(UUID organizationID);

    @PUT
    @Path("/{organizationID}")
    public abstract Organization updateOrganization(UUID organizationID, @Valid @Profiled(profile = OrganizationProfile.PROFILE_URI) Organization organization);
}
