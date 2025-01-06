package gov.cms.dpc.api.resources;

import gov.cms.dpc.api.auth.OrganizationPrincipal;
import gov.cms.dpc.fhir.annotations.Profiled;
import io.swagger.annotations.ApiParam;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Endpoint;

import java.util.UUID;

@Path("/Endpoint")
public abstract class AbstractEndpointResource {

    protected AbstractEndpointResource() {
        // Not used
    }

    @POST
    public abstract Response createEndpoint(@ApiParam(hidden = true) OrganizationPrincipal organization, @Valid @Profiled Endpoint endpoint);

    @GET
    public abstract Bundle getEndpoints(OrganizationPrincipal organization);

    @GET
    @Path("/{endpointID}")
    public abstract Endpoint fetchEndpoint(@NotNull UUID endpointID);

    @PUT
    @Path("/{endpointID}")
    public abstract Endpoint updateEndpoint(@NotNull UUID endpointID, @Valid @Profiled Endpoint endpoint);

    @DELETE
    @Path("/{endpointID}")
    public abstract Response deleteEndpoint(@NotNull UUID endpointID);
}
