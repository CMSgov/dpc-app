package gov.cms.dpc.attribution.resources;

import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Endpoint;

import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import java.util.UUID;

@Path("/Endpoint")
public abstract class AbstractEndpointResource {

    protected AbstractEndpointResource() {
        // Not used
    }

    @GET
    public abstract Bundle searchEndpoints(@NotNull String organizationID);

    @GET
    @Path("/{endpointID}")
    public abstract Endpoint fetchEndpoint(UUID endpointID);
}
