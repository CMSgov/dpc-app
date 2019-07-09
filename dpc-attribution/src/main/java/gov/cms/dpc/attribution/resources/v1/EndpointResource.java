package gov.cms.dpc.attribution.resources.v1;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import gov.cms.dpc.attribution.resources.AbstractEndpointResource;
import org.hl7.fhir.dstu3.model.Endpoint;

import javax.inject.Inject;
import javax.ws.rs.core.Response;

public class EndpointResource extends AbstractEndpointResource {

    @Inject
    public EndpointResource() {
        // Not used
    }

    @Override
    @Timed
    @ExceptionMetered
    public Response createEndpoint(Endpoint endpoint) {
        return Response.ok().build();
    }
}
