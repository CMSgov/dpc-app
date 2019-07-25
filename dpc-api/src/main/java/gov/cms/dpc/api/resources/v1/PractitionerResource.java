package gov.cms.dpc.api.resources.v1;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import gov.cms.dpc.api.auth.OrganizationPrincipal;
import gov.cms.dpc.api.auth.annotations.PathAuthorizer;
import gov.cms.dpc.api.resources.AbstractPractionerResource;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import gov.cms.dpc.fhir.annotations.FHIR;
import gov.cms.dpc.fhir.annotations.Profiled;
import io.dropwizard.auth.Auth;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.hl7.fhir.dstu3.model.*;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.*;

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

        // Create search params
        Map<String, List<String>> searchParams = new HashMap<>();
        searchParams.put("organization", Collections
                .singletonList(organization
                        .getOrganization()
                        .getIdElement()
                        .getIdPart()));

        final var request = this.client
                .search()
                .forResource(Practitioner.class)
                .encodedJson()
                .returnBundle(Bundle.class);

        if (providerNPI != null && !providerNPI.equals("")) {
            searchParams.put("identifier", Collections.singletonList(providerNPI));
        }

        return request
                .whereMap(searchParams)
                .execute();
    }

    @Override
    @GET
    @FHIR
    @Path("/{providerID}")
    @PathAuthorizer(type = ResourceType.Practitioner, pathParam = "providerID")
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
    @Profiled(profile = "https://dpc.cms.gov/fhir/v1/StructureDefinition/dpc-profile-practitioner")
    @ApiOperation(value = "Register provider", notes = "FHIR endpoint to register a provider with the system")
    @ApiResponses(@ApiResponse(code = 201, message = "Successfully created organization"))
    public Response submitProvider(@Auth OrganizationPrincipal organization, Practitioner provider) {

        addOrganizationTag(provider, organization.getOrganization().getIdElement().getIdPart());
        final var test = this.client
                .create()
                .resource(provider)
                .encodedJson();

        final MethodOutcome outcome = test.execute();

        if (!outcome.getCreated() || (outcome.getResource() == null)) {
            throw new WebApplicationException("Unable to submit provider", Response.Status.INTERNAL_SERVER_ERROR);
        }

        final Practitioner resource = (Practitioner) outcome.getResource();
        return Response.status(Response.Status.CREATED).entity(resource).build();
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

    private static void addOrganizationTag(Practitioner provider, String organizationID) {
        final Coding orgTag = new Coding(DPCIdentifierSystem.DPC.getSystem(), organizationID, "Organization ID");
        final Meta meta = provider.getMeta();
        // If no Meta, create new values
        if (meta == null) {
            final Meta newMeta = new Meta();
            newMeta.addTag(orgTag);
            provider.setMeta(newMeta);
        } else {
            meta.addTag(orgTag);
        }
    }
}
