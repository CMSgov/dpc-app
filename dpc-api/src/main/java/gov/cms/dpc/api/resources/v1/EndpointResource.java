package gov.cms.dpc.api.resources.v1;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import gov.cms.dpc.api.auth.OrganizationPrincipal;
import gov.cms.dpc.api.auth.annotations.PathAuthorizer;
import gov.cms.dpc.api.resources.AbstractEndpointResource;
import gov.cms.dpc.fhir.annotations.FHIR;
import gov.cms.dpc.fhir.annotations.Profiled;
import gov.cms.dpc.fhir.helpers.FHIRHelpers;
import gov.cms.dpc.fhir.validations.profiles.EndpointProfile;
import io.dropwizard.auth.Auth;
import io.swagger.annotations.*;
import org.hl7.fhir.dstu3.model.*;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.UUID;

@Api(value = "Endpoint", authorizations = @Authorization(value = "apiKey"))
@Path("/v1/Endpoint")
public class EndpointResource extends AbstractEndpointResource {

    private final IGenericClient client;

    @Inject
    EndpointResource(IGenericClient client) {
        this.client = client;
    }

    @POST
    @FHIR
    @Timed
    @ExceptionMetered
    @ApiOperation(value = "Create an Endpoint", notes = "Create an Endpoint resource for an Organization")
    @ApiResponses(@ApiResponse(code = 204, message = "Endpoint created"))
    @Override
    public Response createEndpoint(@ApiParam(hidden = true) @Auth OrganizationPrincipal organizationPrincipal, @Valid @Profiled(profile = EndpointProfile.PROFILE_URI) Endpoint endpoint) {
        endpoint.setManagingOrganization(new Reference(new IdType("Organization", organizationPrincipal.getID().toString())));
        MethodOutcome outcome = this.client
                .create()
                .resource(endpoint)
                .encodedJson()
                .execute();

        return FHIRHelpers.handleMethodOutcome(outcome);
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
    @ApiResponses(@ApiResponse(code = 404, message = "Endpoint not found"))
    @Override
    public Endpoint fetchEndpoint(@NotNull @PathParam("endpointID") UUID endpointID) {
        return this.client
                .read()
                .resource(Endpoint.class)
                .withId(new IdType("Organization", endpointID.toString()))
                .encodedJson()
                .execute();
    }

    @PUT
    @Path("/{endpointID}")
    @PathAuthorizer(type = ResourceType.Endpoint, pathParam = "endpointID")
    @FHIR
    @Timed
    @ExceptionMetered
    @ApiOperation(value = "Update an Endpoint", notes = "Update an Endpoint resource")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Endpoint updated"),
            @ApiResponse(code = 404, message = "Endpoint not found")
    })
    @Override
    public Endpoint updateEndpoint(@NotNull @PathParam("endpointID") UUID endpointID, @Valid @Profiled(profile = EndpointProfile.PROFILE_URI) Endpoint endpoint) {
        MethodOutcome outcome = this.client
                .update()
                .resource(endpoint)
                .withId(endpointID.toString())
                .encodedJson()
                .execute();

       return (Endpoint) outcome.getResource();
    }

    @DELETE
    @Path("/{endpointID}")
    @PathAuthorizer(type = ResourceType.Endpoint, pathParam = "endpointID")
    @FHIR
    @Timed
    @ExceptionMetered
    @ApiOperation(value = "Delete an Endpoint", notes = "Delete an Endpoint resource")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Endpoint deleted"),
            @ApiResponse(code = 404, message = "Endpoint not found")
    })
    @Override
    public Response deleteEndpoint(@NotNull @PathParam("endpointID") UUID endpointID) {
        this.client
                .delete()
                .resourceById("Endpoint", endpointID.toString())
                .execute();

        return Response.ok().build();
    }
}
