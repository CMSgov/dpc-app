package gov.cms.dpc.api.resources.v1;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.name.Named;
import gov.cms.dpc.api.auth.OrganizationPrincipal;
import gov.cms.dpc.api.auth.annotations.Authorizer;
import gov.cms.dpc.api.auth.annotations.PathAuthorizer;
import gov.cms.dpc.api.resources.AbstractEndpointResource;
import gov.cms.dpc.fhir.DPCResourceType;
import gov.cms.dpc.fhir.annotations.FHIR;
import gov.cms.dpc.fhir.annotations.Profiled;
import gov.cms.dpc.fhir.helpers.FHIRHelpers;
import io.dropwizard.auth.Auth;
import org.eclipse.jetty.http.HttpStatus;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Endpoint;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Reference;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.UUID;

@Path("/v1/Endpoint")
public class EndpointResource extends AbstractEndpointResource {

    private final IGenericClient client;

    @Inject
    EndpointResource(@Named("attribution") IGenericClient client) {
        this.client = client;
    }

    @POST
    @FHIR
    @Timed
    @ExceptionMetered
    @Authorizer
    @Override
    public Response createEndpoint(@Auth OrganizationPrincipal organizationPrincipal, @Valid @Profiled Endpoint endpoint) {
        Reference organizationPrincipalRef = new Reference(new IdType("Organization", organizationPrincipal.getID().toString()));
        if (endpoint.hasManagingOrganization() && !endpoint.getManagingOrganization().getReference().equals(organizationPrincipalRef.getReference())) {
            throw new WebApplicationException("An Endpoint cannot be created for a different Organization", HttpStatus.UNPROCESSABLE_ENTITY_422);
        }

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
    @Authorizer
    @Override
    public Bundle getEndpoints(@Auth OrganizationPrincipal organization) {
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
    @PathAuthorizer(type = DPCResourceType.Endpoint, pathParam = "endpointID")
    @FHIR
    @Timed
    @ExceptionMetered
    @Override
    public Endpoint fetchEndpoint(@PathParam("endpointID") @NotNull UUID endpointID) {
        return this.client
                .read()
                .resource(Endpoint.class)
                .withId(new IdType("Organization", endpointID.toString()))
                .encodedJson()
                .execute();
    }

    @PUT
    @Path("/{endpointID}")
    @PathAuthorizer(type = DPCResourceType.Endpoint, pathParam = "endpointID")
    @FHIR
    @Timed
    @ExceptionMetered
    @Override
    public Endpoint updateEndpoint(@NotNull @PathParam("endpointID") UUID endpointID, @Valid @Profiled Endpoint endpoint) {
        Endpoint currEndpoint = fetchEndpoint(endpointID);
        if (!endpoint.getManagingOrganization().getReference().equals(currEndpoint.getManagingOrganization().getReference())) {
            throw new WebApplicationException("An Endpoint's Organization cannot be changed", HttpStatus.UNPROCESSABLE_ENTITY_422);
        }

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
    @PathAuthorizer(type = DPCResourceType.Endpoint, pathParam = "endpointID")
    @FHIR
    @Timed
    @ExceptionMetered
    @Override
    public Response deleteEndpoint(@NotNull @PathParam("endpointID") UUID endpointID) {
        this.client
                .delete()
                .resourceById("Endpoint", endpointID.toString())
                .execute();

        return Response.ok().build();
    }
}
