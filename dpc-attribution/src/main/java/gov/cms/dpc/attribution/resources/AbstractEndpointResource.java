package gov.cms.dpc.attribution.resources;

import org.hl7.fhir.dstu3.model.Endpoint;

import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.UUID;

@Path("/Endpoint")
public abstract class AbstractEndpointResource {

    protected AbstractEndpointResource() {
        // Not used
    }

    @POST
    public abstract Response createEndpoint(@NotNull Endpoint endpoint);

    @GET
    public abstract List<Endpoint> searchEndpoints(@NotNull String organizationID);

    @GET
    @Path("/{endpointID}")
    public abstract Endpoint fetchEndpoint(@NotNull UUID endpointID);

    @PUT
    @Path("/{endpointID}")
    public abstract Endpoint updateEndpoint(@NotNull UUID endpointID, @NotNull Endpoint endpoint);

    @DELETE
    @Path("/{endpointID}")
    public abstract Response deleteEndpoint(@NotNull UUID endpointID);
}
