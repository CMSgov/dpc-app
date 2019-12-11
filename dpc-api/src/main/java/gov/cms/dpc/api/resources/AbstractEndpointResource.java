package gov.cms.dpc.api.resources;

import gov.cms.dpc.api.auth.OrganizationPrincipal;
import gov.cms.dpc.fhir.annotations.Profiled;
import gov.cms.dpc.fhir.validations.profiles.EndpointProfile;
import io.dropwizard.auth.Auth;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Endpoint;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.UUID;

@Path("/Endpoint")
@Api(value = "Endpoint")
public abstract class AbstractEndpointResource {

    protected AbstractEndpointResource() {
        // Not used
    }

    @POST
    public abstract Response createEndpoint(@ApiParam(hidden = true) @Auth OrganizationPrincipal organization, @Valid @Profiled(profile = EndpointProfile.PROFILE_URI) Endpoint endpoint);

    @GET
    public abstract Bundle getEndpoints(OrganizationPrincipal organization);

    @GET
    @Path("/{endpointID}")
    public abstract Endpoint fetchEndpoint(@NotNull UUID endpointID);

    @PUT
    @Path("/{endpointID}")
    public abstract Endpoint updateEndpoint(@NotNull UUID endpointID, @Valid @Profiled(profile = EndpointProfile.PROFILE_URI) Endpoint endpoint);

    @DELETE
    @Path("/{endpointID}")
    public abstract Response deleteEndpoint(@NotNull UUID endpointID);
}
