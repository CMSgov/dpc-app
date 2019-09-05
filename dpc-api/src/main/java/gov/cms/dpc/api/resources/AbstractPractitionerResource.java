package gov.cms.dpc.api.resources;

import gov.cms.dpc.api.auth.OrganizationPrincipal;
import gov.cms.dpc.fhir.annotations.FHIR;
import gov.cms.dpc.fhir.annotations.Profiled;
import gov.cms.dpc.fhir.validations.profiles.PractitionerProfile;
import io.dropwizard.auth.Auth;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Parameters;
import org.hl7.fhir.dstu3.model.Practitioner;

import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.UUID;

@Path("/Practitioner")
@FHIR
public abstract class AbstractPractitionerResource {

    protected AbstractPractitionerResource() {
        // Not used
    }

    @GET
    public abstract Bundle practitionerSearch(OrganizationPrincipal organization, String providerNPI);

    @POST
    public abstract Response submitProvider(OrganizationPrincipal organization, @Valid @Profiled(profile = PractitionerProfile.PROFILE_URI) Practitioner provider);

    @POST
    @Path("/$submit")
    public abstract Bundle bulkSubmitProviders(@Auth OrganizationPrincipal organization, Parameters params);

    @GET
    @Path("/{providerID}")
    public abstract Practitioner getProvider(UUID providerID);

    @DELETE
    @Path("/{providerID}")
    public abstract Response deleteProvider(UUID providerID);

    @PUT
    @Path("/{providerID}")
    public abstract Practitioner updateProvider(UUID providerID, @Valid @Profiled(profile = PractitionerProfile.PROFILE_URI) Practitioner provider);
}
