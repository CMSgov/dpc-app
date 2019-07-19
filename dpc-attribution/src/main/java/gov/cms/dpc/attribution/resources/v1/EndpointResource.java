package gov.cms.dpc.attribution.resources.v1;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import gov.cms.dpc.attribution.resources.AbstractEndpointResource;
import gov.cms.dpc.fhir.annotations.FHIR;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.hl7.fhir.dstu3.model.Endpoint;

import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.core.Response;

@Api(value = "Endpoint")
public class EndpointResource extends AbstractEndpointResource {

    @Inject
    public EndpointResource() {
        // Not used
    }

    @Override
    @POST
    @FHIR
    @Timed
    @ExceptionMetered
    @ApiOperation(value = "Create Organization Endpoint",
            notes = "FHIR endpoint to create an Endpoint resource associated to a specific Organization")
    @ApiResponses(value = @ApiResponse(code = 201, message = "Endpoint was successfully created"))
    public Response createEndpoint(Endpoint endpoint) {
        return Response.status(Response.Status.CREATED).build();
    }
}
