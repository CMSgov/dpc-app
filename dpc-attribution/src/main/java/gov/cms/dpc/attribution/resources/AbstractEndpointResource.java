package gov.cms.dpc.attribution.resources;

import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Endpoint;

import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.UUID;

@Path("/Endpoint")
public abstract class AbstractEndpointResource {

    protected AbstractEndpointResource() {
        // Not used
    }

    @POST
    public abstract Response createEndpoint(Endpoint endpoint);

    @GET
    public abstract Bundle searchEndpoints(@NotNull String organizationID);

    @GET
    @Path("/{endpointID}")
    public abstract Endpoint fetchEndpoint(UUID endpointID);

    @PUT
    @Path("/{endpointID}")
    public abstract Endpoint updateEndpoint(UUID endpointID, Endpoint endpoint);

    @DELETE
    @Path("/{endpointID}")
    public abstract Response deleteEndpoint(UUID endpointID);
}
