package gov.cms.dpc.api.resources;

import gov.cms.dpc.api.auth.OrganizationPrincipal;
import io.swagger.annotations.Api;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Endpoint;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import java.util.UUID;

@Path("/Endpoint")
@Api(value = "Endpoint")
public abstract class AbstractEndpointResource {

    protected AbstractEndpointResource() {
        // Not used
    }

    @GET
    public abstract Bundle getEndpoints(OrganizationPrincipal organization);

    @GET
    @Path("/{endpointID}")
    public abstract Endpoint fetchEndpoint(UUID endpointID);
}
