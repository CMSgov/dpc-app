package gov.cms.dpc.api.resources.r4;

import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ValidationOptions;
import ca.uhn.fhir.validation.ValidationResult;
import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import gov.cms.dpc.api.APIHelpers;
import gov.cms.dpc.api.auth.OrganizationPrincipal;
import gov.cms.dpc.api.auth.annotations.Authorizer;
import gov.cms.dpc.api.auth.annotations.PathAuthorizer;
import gov.cms.dpc.attribution.client.AttributionClient;
import gov.cms.dpc.common.annotations.NoHtml;
import gov.cms.dpc.fhir.DPCResourceType;
import gov.cms.dpc.fhir.annotations.FHIR;
import gov.cms.dpc.fhir.annotations.Profiled;
import gov.cms.dpc.fhir.r4.FHIRBuilders;
import gov.cms.dpc.fhir.validations.profiles.r4.PractitionerProfile;
import io.dropwizard.auth.Auth;
import io.swagger.annotations.*;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import org.eclipse.jetty.http.HttpStatus;
import org.hl7.fhir.instance.model.api.IBaseOperationOutcome;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Practitioner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

import static gov.cms.dpc.fhir.helpers.r4.FHIRHelpers.handleMethodOutcome;

@Api(value = "Practitioner", authorizations = @Authorization(value = "access_token"))
public class PractitionerResource {

    private static final String PRACTITIONER_PROFILE = PractitionerProfile.PROFILE_URI;
    private static final Logger logger = LoggerFactory.getLogger(PractitionerResource.class);
    private final AttributionClient client;
    private final FhirValidator validator;

    @Inject
    public PractitionerResource(AttributionClient client, @Named("r4") FhirValidator validator) {
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
    public Bundle practitionerSearch(@ApiParam(hidden = true)
                                     @Auth OrganizationPrincipal organization,
                                     @ApiParam(value = "Provider NPI")
                                     @QueryParam(value = Practitioner.SP_IDENTIFIER) @NoHtml String providerNPI) {
        return client.searchPractitioners(organization.getID(), providerNPI);
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
    public Practitioner getProvider(@ApiParam(value = "Practitioner resource ID", required = true) @PathParam("providerID") UUID providerID) {
        return client.getPractitioner(providerID);
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
    public Response submitProvider(@ApiParam(hidden = true) @Auth OrganizationPrincipal organizationPrincipal,
                                   @Valid @Profiled Practitioner provider) {
        FHIRBuilders.addOrganizationTag(provider, organizationPrincipal.getID());
        return handleMethodOutcome(client.createPractitioner(provider));
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
    public Bundle bulkSubmitProviders(@ApiParam(hidden = true) @Auth OrganizationPrincipal organization,
                                      @ApiParam Parameters params) {
        // This is a convoluted, but concise way of making sure params has a Bundle and assigning it to providerBundle
        if (params == null || !(params.getParameterFirstRep().getResource() instanceof Bundle providerBundle)) {
            throw new WebApplicationException("Expected a Bundle of Practitioners", Response.Status.BAD_REQUEST);
        }
        logger.info("submittedProviders={}", providerBundle.getEntry().size());

        // Validate each practitioner before we do a bulk submit to the attribution service
        for (Bundle.BundleEntryComponent entry : providerBundle.getEntry()) {
            if (entry.getResource() instanceof Practitioner provider) {
                validateProvider(provider, organization.getID());
            }
        }

        return client.submitPractitioners(providerBundle);
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
    public Response deleteProvider(@ApiParam(value = "Practitioner resource ID", required = true) @PathParam("providerID") UUID providerID) {
        client.deletePractitioner(providerID);
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
    public Practitioner updateProvider(@ApiParam(value = "Practitioner resource ID", required = true) @PathParam("providerID") UUID providerID,
                                       @Valid @Profiled Practitioner provider) {
        throw new WebApplicationException("Update Practitioner not yet implemented.", Response.Status.NOT_IMPLEMENTED);
    }

    @POST
    @Path("/$validate")
    @FHIR
    @Timed
    @ExceptionMetered
    @Authorizer
    @ApiOperation(value = "Validate Practitioner resource", notes = "Validates the given resource against the " + PRACTITIONER_PROFILE + " profile." +
            "<p>This method always returns a 200 status, even in response to a non-conformant resource.")
    public IBaseOperationOutcome validateProvider(@Auth @ApiParam(hidden = true) OrganizationPrincipal organization, Parameters parameters) {
        if (parameters == null || parameters.getParameterFirstRep().getResource() == null) {
            throw new WebApplicationException("No resource provided for validation", Response.Status.BAD_REQUEST);
        }
        return validator.validateWithResult(parameters.getParameterFirstRep().getResource(),
                new ValidationOptions().addProfile(PRACTITIONER_PROFILE)).toOperationOutcome();
    }

    private void validateProvider(Practitioner provider, UUID organizationID) {
        logger.debug("Validating Practitioner {}", provider);
        final ValidationResult result = validator.validateWithResult(provider, new ValidationOptions().addProfile(PRACTITIONER_PROFILE));
        if (!result.isSuccessful()) {
            throw new WebApplicationException(APIHelpers.formatValidationMessages(result.getMessages()), HttpStatus.UNPROCESSABLE_ENTITY_422);
        }
        FHIRBuilders.addOrganizationTag(provider, organizationID);
    }
}
