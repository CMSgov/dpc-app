package gov.cms.dpc.api.resources.v1;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import gov.cms.dpc.api.auth.OrganizationPrincipal;
import gov.cms.dpc.api.auth.annotations.PathAuthorizer;
import gov.cms.dpc.api.resources.AbstractEndpointResource;
import gov.cms.dpc.fhir.annotations.FHIR;
import io.dropwizard.auth.Auth;
import io.swagger.annotations.*;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Endpoint;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.ResourceType;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import java.util.UUID;

@Api(value = "Endpoint", authorizations = @Authorization(value = "apiKey"))
public class EndpointResource extends AbstractEndpointResource {

    private final IGenericClient client;

    @Inject
    EndpointResource(IGenericClient client) {
        this.client = client;
    }

    @GET
    @FHIR
    @Timed
    @ExceptionMetered
    @ApiOperation(value = "Search for Endpoints", notes = "Search for public Endpoint resources associated to the given Organization.")
    @Override
    public Bundle getEndpoints(@ApiParam(hidden = true) @Auth OrganizationPrincipal organization) {
        return this.client
                .search()
                .forResource(Endpoint.class)
                .where(Endpoint.ORGANIZATION.hasId(organization.getOrganization().getId()))
                .returnBundle(Bundle.class)
                .encodedJson()
                .execute();
    }

    @GET
    @Path("/{endpointID}")
    @PathAuthorizer(type = ResourceType.Endpoint, pathParam = "endpointID")
    @FHIR
    @Timed
    @ExceptionMetered
    @ApiOperation(value = "Fetch Endpoint resource", notes = "Fetch a specific Endpoint associated to an Organization.")
    @ApiResponses(@ApiResponse(code = 404, message = "Resource not found"))
    @Override
    public Endpoint fetchEndpoint(@PathParam("endpointID") UUID endpointID) {
        return this.client
                .read()
                .resource(Endpoint.class)
                .withId(new IdType("Organization", endpointID.toString()))
                .encodedJson()
                .execute();
    }
}
