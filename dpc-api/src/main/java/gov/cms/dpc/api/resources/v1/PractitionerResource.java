package gov.cms.dpc.api.resources.v1;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ValidationResult;
import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import gov.cms.dpc.api.APIHelpers;
import gov.cms.dpc.api.auth.OrganizationPrincipal;
import gov.cms.dpc.api.auth.annotations.PathAuthorizer;
import gov.cms.dpc.api.resources.AbstractPractitionerResource;
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
import java.util.function.Consumer;

import static gov.cms.dpc.api.APIHelpers.bulkResourceClient;

public class PractitionerResource extends AbstractPractitionerResource {

    private static final String PRACTITIONER_PROFILE = "https://dpc.cms.gov/api/v1/StructureDefinition/dpc-profile-practitioner";
    private final IGenericClient client;
    private final FhirValidator validator;

    @Inject
    PractitionerResource(IGenericClient client, FhirValidator validator) {
        this.client = client;
        this.validator = validator;
    }

    @Override
    @GET
    @FHIR
    @Timed
    @ExceptionMetered
    @ApiOperation(value = "Search for providers", notes = "FHIR endpoint to search for Practitioner resources." +
            "<p>If a provider NPI is given, the results are filtered accordingly. " +
            "Otherwise, the method returns all Practitioners associated to the given Organization")
    public Bundle practitionerSearch(@ApiParam(hidden = true)
                                   @Auth OrganizationPrincipal organization,
                                     @ApiParam(value = "Provider NPI")
                                   @QueryParam(value = Practitioner.SP_IDENTIFIER) String providerNPI) {

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
    @Profiled(profile = PRACTITIONER_PROFILE)
    @ApiOperation(value = "Register provider", notes = "FHIR endpoint to register a provider with the system")
    @ApiResponses(@ApiResponse(code = 201, message = "Successfully created organization"))
    public Response submitProvider(@Auth OrganizationPrincipal organization, Practitioner provider) {

        APIHelpers.addOrganizationTag(provider, organization.getOrganization().getIdElement().getIdPart());
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

    @POST
    @Path("/$submit")
    @FHIR
    @Timed
    @ExceptionMetered
    @ApiOperation(value = "Bulk submit Practitioner resources", notes = "FHIR operation for submitting a Bundle of Practitioner resources, which will be associated to the given Organization." +
            "<p> Each Practitioner MUST implement the " + PRACTITIONER_PROFILE + " profile.")
    @Override
    public Bundle bulkSubmitProviders(@Auth OrganizationPrincipal organization, Bundle providerBundle) {
        final Consumer<Practitioner> entryHandler = (resource) -> validateAndTagProvider(resource,
                organization.getOrganization().getId(),
                validator,
                PRACTITIONER_PROFILE);

        return bulkResourceClient(Practitioner.class, client, entryHandler, providerBundle);
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

    private static void validateAndTagProvider(Practitioner provider, String organizationID, FhirValidator validator, String profileURL) {
        if (!APIHelpers.hasProfile(provider, profileURL)) {
            throw new WebApplicationException("Provider must have correct profile", Response.Status.BAD_REQUEST);
        }
        final ValidationResult result = validator.validateWithResult(provider);
        if (!result.isSuccessful()) {
            throw new WebApplicationException(APIHelpers.formatValidationMessages(result.getMessages()), Response.Status.BAD_REQUEST);
        }
        APIHelpers.addOrganizationTag(provider, organizationID);
    }

}
