package gov.cms.dpc.api.resources.v1;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import gov.cms.dpc.api.auth.OrganizationPrincipal;
import gov.cms.dpc.api.auth.annotations.PathAuthorizer;
import gov.cms.dpc.api.resources.AbstractPractionerResource;
import gov.cms.dpc.fhir.annotations.FHIR;
import io.dropwizard.auth.Auth;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.hl7.fhir.dstu3.model.*;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.UUID;

public class PractitionerResource extends AbstractPractionerResource {

    private final IGenericClient client;

    @Inject
    PractitionerResource(IGenericClient client) {
        this.client = client;
    }

    @Override
    @GET
    @FHIR
    @Timed
    @ExceptionMetered
    @ApiOperation(value = "Search for providers", notes = "FHIR endpoint to search for Practitioner resources." +
            "<p>If a provider NPI is given, the results are filtered accordingly. " +
            "Otherwise, the method returns all Practitioners associated to the given Organization")
    public Bundle getPractitioners(@ApiParam(hidden = true)
                                   @Auth OrganizationPrincipal organization,
                                   @ApiParam(value = "Provider NPI")
                                   @QueryParam("identifier") String providerNPI) {
        final var request = this.client
                .search()
                .forResource(Practitioner.class)
                .encodedJson()
                .withTag("organization", organization.getOrganization().getId())
                .returnBundle(Bundle.class);

        if (providerNPI != null && !providerNPI.equals("")) {
            return request
                    .where(Practitioner.IDENTIFIER.exactly().identifier(providerNPI))
                    .execute();
        } else {
            return request.execute();
        }
    }

    @Override
    @GET
    @FHIR
    @Path("/{providerID}")
    @PathAuthorizer(type = ResourceType.PractitionerRole, pathParam = "providerID")
    @Timed
    @ExceptionMetered
    @ApiOperation(value = "Fetch provider", notes = "FHIR endpoint to fetch a specific Practitioner resource." +
            "<p>Note: FHIR refers to *Providers* as *Practitioners* and names the resources and endpoints accordingly")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "No matching Practitioner resource was found", response = OperationOutcome.class)
    })
    public Practitioner getProvider(@ApiParam(value = "Practitioner resource ID", required = true) @PathParam("providerID") UUID providerID) {
        return this.client
                .read()
                .resource(Practitioner.class)
                .withId(providerID.toString())
                .encodedJson()
                .execute();
    }

    @Override
    @POST
    @FHIR
    @Timed
    @ExceptionMetered
    @ApiOperation(value = "Register provider", notes = "FHIR endpoint to register a provider with the system")
    public Practitioner submitProvider(@Auth OrganizationPrincipal organization, Practitioner provider) {
        final var test = this.client
                .create()
                .resource(provider)
                .encodedJson();

        final MethodOutcome outcome = test.execute();

        if (!outcome.getCreated() || (outcome.getResource() == null)) {
            throw new WebApplicationException("Unable to submit provider", Response.Status.INTERNAL_SERVER_ERROR);
        }

        final Practitioner resource = (Practitioner) outcome.getResource();

        // Now, submit the Practitioner Role
        final PractitionerRole role = new PractitionerRole();
        role.setOrganization(new Reference(organization.getOrganization().getIdElement()));
        role.setPractitioner(new Reference(resource.getIdElement()));

        final MethodOutcome roled = this.client
                .create()
                .resource(role)
                .encodedJson()
                .execute();

        if (!roled.getCreated()) {
            throw new WebApplicationException("Unable to link provider to organization", Response.Status.INTERNAL_SERVER_ERROR);
        }
        return resource;
    }

    @Override
    @DELETE
    @Path("/{providerID}")
    @PathAuthorizer(type = ResourceType.PractitionerRole, pathParam = "providerID")
    @FHIR
    @Timed
    @ExceptionMetered
    @ApiOperation(value = "Delete provider", notes = "FHIR endpoint to remove the given Practitioner resource")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "No matching Practitioner resource was found", response = OperationOutcome.class)
    })
    public Response deleteProvider(@ApiParam(value = "Practitioner resource ID", required = true) @PathParam("providerID") UUID providerID) {
        this.client
                .delete()
                .resourceById(new IdType("Practitioner", providerID.toString()))
                .encodedJson()
                .execute();

        return Response.ok().build();
    }

    @Override
    @PUT
    @Path("/{providerID}")
    @PathAuthorizer(type = ResourceType.PractitionerRole, pathParam = "providerID")
    @FHIR
    @Timed
    @ExceptionMetered
    @ApiOperation(value = "Update provider", notes = "FHIR endpoint to update the given Practitioner resource with new values.")
    public Practitioner updateProvider(@ApiParam(value = "Practitioner resource ID", required = true) @PathParam("providerID") UUID providerID, Practitioner provider) {
        return null;
    }
}
