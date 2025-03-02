package gov.cms.dpc.api.resources.v1;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ValidationOptions;
import ca.uhn.fhir.validation.ValidationResult;
import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import gov.cms.dpc.api.APIHelpers;
import gov.cms.dpc.api.auth.OrganizationPrincipal;
import gov.cms.dpc.api.auth.annotations.Authorizer;
import gov.cms.dpc.api.auth.annotations.PathAuthorizer;
import gov.cms.dpc.api.resources.AbstractPractitionerResource;
import gov.cms.dpc.common.annotations.NoHtml;
import gov.cms.dpc.fhir.DPCResourceType;
import gov.cms.dpc.fhir.annotations.FHIR;
import gov.cms.dpc.fhir.annotations.Profiled;
import gov.cms.dpc.fhir.validations.ValidationHelpers;
import gov.cms.dpc.fhir.validations.profiles.PractitionerProfile;
import io.dropwizard.auth.Auth;
import io.swagger.annotations.*;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import org.eclipse.jetty.http.HttpStatus;
import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.instance.model.api.IBaseOperationOutcome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Function;

import static gov.cms.dpc.api.APIHelpers.bulkResourceClient;
import static gov.cms.dpc.fhir.helpers.FHIRHelpers.handleMethodOutcome;

@Api(value = "Practitioner", authorizations = @Authorization(value = "access_token"))
@Path("/v1/Practitioner")
public class PractitionerResource extends AbstractPractitionerResource {

    private static final String PRACTITIONER_PROFILE = "https://dpc.cms.gov/api/v1/StructureDefinition/dpc-profile-practitioner";
    private static final Logger logger = LoggerFactory.getLogger(PractitionerResource.class);
    private final IGenericClient client;
    private final FhirValidator validator;

    @Inject
    PractitionerResource(@Named("attribution") IGenericClient client, FhirValidator validator) {
        this.client = client;
        this.validator = validator;
    }

    @GET
    @FHIR
    @Timed
    @ExceptionMetered
    @Authorizer
    @ApiOperation(value = "Search for providers", notes = "FHIR endpoint to search for Practitioner resources." +
            "<p>If a provider NPI is given, the results are filtered accordingly. " +
            "Otherwise, the method returns all Practitioners associated to the given Organization")
    @Override
    public Bundle practitionerSearch(@ApiParam(hidden = true)
                                     @Auth OrganizationPrincipal organization,
                                     @ApiParam(value = "Provider NPI")
                                     @QueryParam(value = Practitioner.SP_IDENTIFIER) @NoHtml String providerNPI) {

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

        if (providerNPI != null && !providerNPI.isEmpty()) {
            searchParams.put("identifier", Collections.singletonList(providerNPI));
        }

        return request
                .whereMap(searchParams)
                .execute();
    }

    @GET
    @FHIR
    @Path("/{providerID}")
    @PathAuthorizer(type = DPCResourceType.Practitioner, pathParam = "providerID")
    @Timed
    @ExceptionMetered
    @ApiOperation(value = "Fetch provider", notes = "FHIR endpoint to fetch a specific Practitioner resource." +
            "<p>Note: FHIR refers to *Providers* as *Practitioners* and names the resources and endpoints accordingly")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "No matching Practitioner resource was found", response = OperationOutcome.class)
    })
    @Override
    public Practitioner getProvider(@ApiParam(value = "Practitioner resource ID", required = true) @PathParam("providerID") UUID providerID) {
        return this.client
                .read()
                .resource(Practitioner.class)
                .withId(providerID.toString())
                .encodedJson()
                .execute();
    }

    @POST
    @FHIR
    @Timed
    @ExceptionMetered
    @Authorizer
    @ApiOperation(value = "Register provider", notes = "FHIR endpoint to register a provider with the system")
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Successfully created provider"),
            @ApiResponse(code = 422, message = "Provider does not satisfy the required FHIR profile")
    })
    @Override
    public Response submitProvider(@ApiParam(hidden = true) @Auth OrganizationPrincipal organizationPrincipal,
                                   @Valid @Profiled Practitioner provider) {

        APIHelpers.addOrganizationTag(provider, organizationPrincipal.getID().toString());
        final var providerCreate = this.client
                .create()
                .resource(provider)
                .encodedJson();

        return handleMethodOutcome(providerCreate.execute());
    }

    @POST
    @Path("/$submit")
    @FHIR
    @Timed
    @ExceptionMetered
    @Authorizer
    @ApiOperation(value = "Bulk submit Practitioner resources", notes = "FHIR operation for submitting a Bundle of Practitioner resources, which will be associated to the given Organization." +
            "<p> Each Practitioner MUST implement the " + PRACTITIONER_PROFILE + " profile.")
    @ApiResponses(@ApiResponse(code = 422, message = "Provider does not satisfy the required FHIR profile"))
    @Override
    public Bundle bulkSubmitProviders(@ApiParam(hidden = true) @Auth OrganizationPrincipal organization,
                                      @ApiParam Parameters params) {
        final Bundle providerBundle = (Bundle) params.getParameterFirstRep().getResource();
        logger.info("submittedProviders={}", providerBundle.getEntry().size());

        final Function<Practitioner, Optional<WebApplicationException>> entryHandler =
            resource -> validateProvider(resource,
                organization.getOrganization().getId(),
                validator,
                PRACTITIONER_PROFILE);

        return bulkResourceClient(Practitioner.class, client, entryHandler, providerBundle);
    }

    @DELETE
    @Path("/{providerID}")
    @PathAuthorizer(type = DPCResourceType.Practitioner, pathParam = "providerID")
    @FHIR
    @Timed
    @ExceptionMetered
    @ApiOperation(value = "Delete provider", notes = "FHIR endpoint to remove the given Practitioner resource")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "No matching Practitioner resource was found", response = OperationOutcome.class)
    })
    @Override
    public Response deleteProvider(@ApiParam(value = "Practitioner resource ID", required = true) @PathParam("providerID") UUID providerID) {
        this.client
                .delete()
                .resourceById(new IdType("Practitioner", providerID.toString()))
                .encodedJson()
                .execute();

        return Response.ok().build();
    }

    @PUT
    @Path("/{providerID}")
    @PathAuthorizer(type = DPCResourceType.Practitioner, pathParam = "providerID")
    @FHIR
    @Timed
    @ExceptionMetered
    @ApiOperation(value = "Update provider", notes = "FHIR endpoint to update the given Practitioner resource with new values.")
    @ApiResponses(@ApiResponse(code = 422, message = "Provider does not satisfy the required FHIR profile"))
    @Override
    public Practitioner updateProvider(@ApiParam(value = "Practitioner resource ID", required = true) @PathParam("providerID") UUID providerID, @Valid @Profiled Practitioner provider) {
        throw new WebApplicationException("Update Practitioner not yet implemented.", Response.Status.NOT_IMPLEMENTED);
    }

    @POST
    @Path("/$validate")
    @FHIR
    @Timed
    @ExceptionMetered
    @Authorizer
    @ApiOperation(value = "Validate Practitioner resource", notes = "Validates the given resource against the " + PractitionerProfile.PROFILE_URI + " profile." +
            "<p>This method always returns a 200 status, even in respond to a non-conformant resource.")
    @Override
    public IBaseOperationOutcome validateProvider(@Auth @ApiParam(hidden = true) OrganizationPrincipal organization, Parameters parameters) {
        return ValidationHelpers.validateAgainstProfile(this.validator, parameters, PractitionerProfile.PROFILE_URI);
    }

    private static Optional<WebApplicationException> validateProvider(Practitioner provider, String organizationID, FhirValidator validator, String profileURL) {
        logger.debug("Validating Practitioner {}", provider);
        final ValidationResult result = validator.validateWithResult(provider, new ValidationOptions().addProfile(profileURL));
        if (!result.isSuccessful()) {
            return Optional.of(new WebApplicationException(APIHelpers.formatValidationMessages(result.getMessages()), HttpStatus.UNPROCESSABLE_ENTITY_422));
        } else {
            APIHelpers.addOrganizationTag(provider, organizationID);
            return Optional.empty();
        }
    }

}
