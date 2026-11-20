package gov.cms.dpc.api.resources.r4;

import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ResultSeverityEnum;
import ca.uhn.fhir.validation.ValidationOptions;
import ca.uhn.fhir.validation.ValidationResult;
import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import gov.cms.dpc.api.APIHelpers;
import gov.cms.dpc.api.auth.OrganizationPrincipal;
import gov.cms.dpc.api.auth.annotations.Authorizer;
import gov.cms.dpc.api.auth.annotations.PathAuthorizer;
import gov.cms.dpc.attribution.client.AttributionClient;
import gov.cms.dpc.attribution.client.search.PatientSearch;
import gov.cms.dpc.common.annotations.NoHtml;
import gov.cms.dpc.common.annotations.ServiceBaseURL;
import gov.cms.dpc.fhir.DPCResourceType;
import gov.cms.dpc.fhir.annotations.FHIR;
import gov.cms.dpc.fhir.annotations.Profiled;
import gov.cms.dpc.fhir.validations.profiles.r4.PatientProfile;
import io.dropwizard.auth.Auth;
import io.swagger.annotations.*;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import org.eclipse.jetty.http.HttpStatus;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseOperationOutcome;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.UUID;

import static gov.cms.dpc.fhir.helpers.r4.FHIRHelpers.handleMethodOutcome;

@Api(value = "Patient", authorizations = @Authorization(value = "access_token"))
public class PatientResource {
    private static final Logger logger = LoggerFactory.getLogger(PatientResource.class);
    private static final Set<String> VALID_BUNDLE_LINK_NAMES =
            Set.of("first", IBaseBundle.LINK_PREV, IBaseBundle.LINK_NEXT, IBaseBundle.LINK_SELF);

    private final AttributionClient client;
    private final FhirValidator validator;
    private final String baseURL;

    @Inject
    public PatientResource(AttributionClient client, @Named("r4") FhirValidator validator, @ServiceBaseURL String baseURL) {
        this.client = client;
        this.validator = validator;
        this.baseURL = baseURL;
    }

    @GET
    @FHIR
    @Timed
    @ExceptionMetered
    @Authorizer
    @ApiOperation(value = "Search for Patients", notes = "FHIR endpoint for searching for Patient resources." +
            "<p> If Patient Identifier is provided, results will be filtered to match the given property")
    public Bundle patientSearch(@ApiParam(hidden = true)
                                @Auth OrganizationPrincipal organization,
                                @ApiParam(value = "Patient MBI")
                                @QueryParam(value = Patient.SP_IDENTIFIER) @NoHtml String patientMBI,
                                @ApiParam(value = "Patients per page")
                                @QueryParam(value = "_count") Integer count,
                                @ApiParam(value = "Page offset")
                                @QueryParam(value = "_offset") Integer offset) {
        if (count != null && count < 0) {
            throw new WebApplicationException("Parameter _count must be >= 0", Response.Status.BAD_REQUEST);
        }
        if (offset != null && offset < 0) {
            throw new WebApplicationException("Parameter _offset must be >= 0", Response.Status.BAD_REQUEST);
        }

        Bundle bundle = client.searchPatients(new PatientSearch(null, patientMBI, organization.getID(), count, offset, null));
        handlePagingLinkPathing(bundle);
        return bundle;
    }

    @FHIR
    @POST
    @Timed
    @Authorizer
    @ExceptionMetered
    @ApiOperation(value = "Create Patient", notes = "Create a Patient record associated to the Organization.")
    @ApiResponses(@ApiResponse(code = 422, message = "Patient does not satisfy the required FHIR profile"))
    public Response submitPatient(@ApiParam(hidden = true) @Auth OrganizationPrincipal organization,
                                  @Valid @Profiled @ApiParam Patient patient) {
        // Set the Managing Organization on the Patient
        patient.setManagingOrganization(new Reference(new IdType("Organization", organization.getID().toString())));
        return handleMethodOutcome(client.createPatient(patient));
    }

    @FHIR
    @POST
    @Path("/$submit")
    @Timed
    @ExceptionMetered
    @Authorizer
    @ApiOperation(value = "Bulk submit Patient resources", notes = "FHIR operation for submitting a Bundle of Patient resources, which will be associated to the given Organization." +
            "<p> Each Patient resource MUST implement the " + PatientProfile.PROFILE_URI + " profile.")
    @ApiResponses(@ApiResponse(code = 422, message = "Patient does not satisfy the required FHIR profile"))
    public Bundle bulkSubmitPatients(@ApiParam(hidden = true) @Auth OrganizationPrincipal organization,
                                     @ApiParam Parameters params) {
        if (params == null || !(params.getParameterFirstRep().getResource() instanceof Bundle patientBundle)) {
            throw new WebApplicationException("Expected a Bundle of Patients", Response.Status.BAD_REQUEST);
        }
        logger.info("submittedPatients={}", patientBundle.getEntry().size());

        for (Bundle.BundleEntryComponent entry : patientBundle.getEntry()) {
            if (entry.getResource() instanceof Patient patient) {
                validateAndAddOrg(patient, organization.getID());
            }
        }
        return client.submitPatients(patientBundle);
    }

    @GET
    @FHIR
    @Path("/{patientID}")
    @PathAuthorizer(type = DPCResourceType.Patient, pathParam = "patientID")
    @Timed
    @ExceptionMetered
    @ApiOperation(value = "Fetch Patient", notes = "Fetch specific Patient record.")
    @ApiResponses(@ApiResponse(code = 404, message = "Cannot find Patient with given ID"))
    public Patient getPatient(@ApiParam(value = "Patient resource ID", required = true) @PathParam("patientID") UUID patientID) {
        return client.getPatient(patientID);
    }

    @DELETE
    @FHIR
    @Path("/{patientID}")
    @PathAuthorizer(type = DPCResourceType.Patient, pathParam = "patientID")
    @Timed
    @ExceptionMetered
    @ApiOperation(value = "Delete Patient", notes = "Remove specific Patient record")
    @ApiResponses(@ApiResponse(code = 404, message = "Unable to find Patient to delete"))
    public Response deletePatient(@ApiParam(value = "Patient resource ID", required = true) @PathParam("patientID") UUID patientID) {
        client.deletePatient(patientID);
        return Response.ok().build();
    }

    @PUT
    @Path("/{patientID}")
    @PathAuthorizer(type = DPCResourceType.Patient, pathParam = "patientID")
    @FHIR
    @Timed
    @ExceptionMetered
    @ApiOperation(value = "Update Patient record", notes = "Update specific Patient record." +
            "<p>Currently, this method allows for updating of only the Patient first name, last name, and birthdate.")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Unable to find Patient to update"),
            @ApiResponse(code = 422, message = "Patient does not satisfy the required FHIR profile")
    })
    public Patient updatePatient(@ApiParam(value = "Patient resource ID", required = true) @PathParam("patientID") UUID patientID,
                                 @Valid @Profiled @ApiParam Patient patient) {
        final Patient resource = client.updatePatient(patientID, patient);
        if (resource == null) {
            throw new WebApplicationException("Unable to update Patient", Response.Status.INTERNAL_SERVER_ERROR);
        }
        return resource;
    }

    @POST
    @Path("/$validate")
    @FHIR
    @Timed
    @ExceptionMetered
    @Authorizer
    @ApiOperation(value = "Validate Patient resource", notes = "Validates the given resource against the " + PatientProfile.PROFILE_URI + " profile." +
            "<p>This method always returns a 200 status, even in response to a non-conformant resource.")
    public IBaseOperationOutcome validatePatient(@Auth @ApiParam(hidden = true) OrganizationPrincipal organization,
                                                 @ApiParam Parameters parameters) {
        if (parameters == null || parameters.getParameterFirstRep().getResource() == null) {
            throw new WebApplicationException("No resource provided for validation", Response.Status.BAD_REQUEST);
        }
        return validator.validateWithResult(parameters.getParameterFirstRep().getResource(),
                new ValidationOptions().addProfile(PatientProfile.PROFILE_URI)).toOperationOutcome();
    }

    private void validateAndAddOrg(Patient patient, UUID organizationID) {
        // Set the Managing Org, since we need it for the validation
        patient.setManagingOrganization(new Reference(new IdType("Organization", organizationID.toString())));
        final ValidationResult result = validator.validateWithResult(patient, new ValidationOptions().addProfile(PatientProfile.PROFILE_URI));
        if (!result.isSuccessful() && result.getMessages().stream()
                .anyMatch(message -> message.getSeverity() != ResultSeverityEnum.INFORMATION)) {
            throw new WebApplicationException(APIHelpers.formatValidationMessages(result.getMessages()), HttpStatus.UNPROCESSABLE_ENTITY_422);
        }
    }

    private void handlePagingLinkPathing(Bundle bundle) {
        if (bundle == null || !bundle.hasLink()) {
            return;
        }
        for (Bundle.BundleLinkComponent link : bundle.getLink()) {
            if (link.hasRelation() && VALID_BUNDLE_LINK_NAMES.contains(link.getRelation())) {
                String url = link.getUrl();
                if (url != null && url.startsWith("/")) {
                    if (url.startsWith("/r4/") || url.startsWith("/v3/")) {
                        url = url.substring(3);
                    }
                    link.setUrl(baseURL + "/v3" + url);
                }
            }
        }
    }
}
