package gov.cms.dpc.attribution.resources;

import gov.cms.dpc.fhir.annotations.FHIR;
import org.hl7.fhir.dstu3.model.Endpoint;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

@Path("/Endpoint")
public abstract class AbstractEndpointResource {

    protected AbstractEndpointResource() {
        // Not used
    }

    @POST
    @FHIR
    public abstract Response createEndpoint(Endpoint endpoint);
}
