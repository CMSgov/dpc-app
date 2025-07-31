package gov.cms.dpc.api.resources.v1;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.SummaryEnum;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.server.exceptions.ForbiddenOperationException;
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
import gov.cms.dpc.api.resources.AbstractPatientResource;
import gov.cms.dpc.bluebutton.client.BlueButtonClient;
import gov.cms.dpc.common.annotations.APIV1;
import gov.cms.dpc.common.annotations.NoHtml;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import gov.cms.dpc.fhir.DPCResourceType;
import gov.cms.dpc.fhir.FHIRExtractors;
import gov.cms.dpc.fhir.FHIRHeaders;
import gov.cms.dpc.fhir.annotations.FHIR;
import gov.cms.dpc.fhir.annotations.Profiled;
import gov.cms.dpc.fhir.annotations.ProvenanceHeader;
import gov.cms.dpc.fhir.validations.ValidationHelpers;
import gov.cms.dpc.fhir.validations.profiles.PatientProfile;
import gov.cms.dpc.queue.service.DataService;
import io.dropwizard.auth.Auth;
import io.swagger.annotations.*;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import org.eclipse.jetty.http.HttpStatus;
import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.instance.model.api.IBaseOperationOutcome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Pattern;

import static gov.cms.dpc.api.APIHelpers.bulkResourceClient;
import static gov.cms.dpc.fhir.helpers.FHIRHelpers.handleMethodOutcome;

@Api(value = "Patient", authorizations = @Authorization(value = "access_token"))
@Path("/v1/Patient")
public class PatientResource extends AbstractPatientResource {
    private static final Logger logger = LoggerFactory.getLogger(PatientResource.class);

    // TODO: This should be moved into a helper class, in DPC-432.
    // This checks to see if the Identifier is fully specified or not.
    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("^[a-z0-9]+://.*$");

    private final IGenericClient client;
    private final FhirValidator validator;
    private final DataService dataService;
    private final BlueButtonClient bfdClient;
    private final String baseURL;

    @Inject
    public PatientResource(@Named("attribution") IGenericClient client,
                           FhirValidator validator,
                           DataService dataService,
                           BlueButtonClient bfdClient,
                           @APIV1 String baseURL) {
        this.client = client;
        this.validator = validator;
        this.dataService = dataService;
        this.bfdClient = bfdClient;
        this.baseURL = baseURL;
    }

    @GET
    @FHIR
    @Timed
    @ExceptionMetered
    @Authorizer
    @ApiOperation(value = "Search for Patients", notes = "FHIR endpoint for searching for Patient resources." +
            "<p> If Patient Identifier is provided, results will be filtered to match the given property")
    @Override
    public Bundle patientSearch(@ApiParam(hidden = true)
                                @Auth OrganizationPrincipal organization,
                                @ApiParam(value = "Patient MBI")
                                @QueryParam(value = Patient.SP_IDENTIFIER) @NoHtml String patientMBI,
                                @ApiParam(value = "Patients per page") // used to determine if pagination logic should be used
                                @QueryParam(value = "_count") Integer count,
                                @ApiParam(value = "Page offset")
                                @QueryParam(value = "_offset") Integer offset) {
        if (count != null && count < 0) {
            throw new WebApplicationException("Parameter _count must be >= 0", Response.Status.BAD_REQUEST);
        }
        if (offset != null && offset < 0) {
            throw new WebApplicationException("Parameter _offset must be >= 0", Response.Status.BAD_REQUEST);
        }

        var request = this.buildPatientSearchQuery(organization.getOrganization().getId(), patientMBI);


        if (count != null) {
            request.count(count);
            if (count == 0) {
                request.summaryMode(SummaryEnum.COUNT); // count gets omitted in request
            }
        }
        if (offset != null) {
            request.offset(offset);
        }
        return request.execute(); // this should call attribution
    }

    @FHIR
    @POST
    @Timed
    @Authorizer
    @ExceptionMetered
    @ApiOperation(value = "Create Patient", notes = "Create a Patient record associated to the Organization.")
    @ApiResponses(@ApiResponse(code = 422, message = "Patient does not satisfy the required FHIR profile"))
    @Override
    public Response submitPatient(@ApiParam(hidden = true) @Auth OrganizationPrincipal organization, @Valid @Profiled @ApiParam Patient patient) {

        // Set the Managing Organization on the Patient
        final Reference orgReference = new Reference(new IdType("Organization", organization.getOrganization().getId()));
        patient.setManagingOrganization(orgReference);
        final MethodOutcome outcome = this.client
                .create()
                .resource(patient)
                .encodedJson()
                .execute();

        return handleMethodOutcome(outcome);
    }

    @FHIR
    @POST
    @Path("/$submit")
    @Timed
    @ExceptionMetered
    @Authorizer
    @ApiOperation(value = "Bulk submit Patient resources", notes = "FHIR operation for submitting a Bundle of Patient resources, which will be associated to the given Organization." +
            "<p> Each Patient resource MUST implement the " + PatientProfile.PROFILE_URI + "profile.")
    @ApiResponses(@ApiResponse(code = 422, message = "Patient does not satisfy the required FHIR profile"))
    @Override
    public Bundle bulkSubmitPatients(@ApiParam(hidden = true) @Auth OrganizationPrincipal organization,
                                     @ApiParam Parameters params) {
        final Bundle patientBundle = (Bundle) params.getParameterFirstRep().getResource();
        logger.info("submittedPatients={}", patientBundle.getEntry().size());

        final Function<Patient, Optional<WebApplicationException>> entryHandler =
            patient -> validateAndAddOrg(patient, organization.getOrganization().getId(), validator);

        return bulkResourceClient(Patient.class, client, entryHandler, patientBundle);
    }


    @GET
    @FHIR
    @Path("/{patientID}")
    @PathAuthorizer(type = DPCResourceType.Patient, pathParam = "patientID")
    @Timed
    @ExceptionMetered
    @ApiOperation(value = "Fetch Patient", notes = "Fetch specific Patient record.")
    @ApiResponses(@ApiResponse(code = 404, message = "Cannot find Patient with given ID"))
    @Override
    public Patient getPatient(@ApiParam(value = "Patient resource ID", required = true) @PathParam("patientID") UUID patientID) {
        return this.client
                .read()
                .resource(Patient.class)
                .withId(patientID.toString())
                .encodedJson()
                .execute();
    }

    @GET
    @FHIR
    @Path("/{patientID}/$everything")
    @PathAuthorizer(type = DPCResourceType.Patient, pathParam = "patientID")
    @Timed
    @ExceptionMetered
    @ApiImplicitParams(
            @ApiImplicitParam(name = "X-Provenance", required = true, paramType = "header", type = "string", dataTypeClass = Provenance.class))
    @ApiOperation(value = "Fetch entire Patient record", notes = "Fetch entire record for Patient with given ID synchronously. " +
            "All resources available for the Patient are included in the result Bundle.")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Cannot find Patient record with given ID"),
            @ApiResponse(code = 504, message = "", response = OperationOutcome.class),
            @ApiResponse(code = 500, message = "A system error occurred", response = OperationOutcome.class)
    })
    @Override
    public Response everything(@ApiParam(hidden = true) @Auth OrganizationPrincipal organization,
                             @Valid @Profiled @ProvenanceHeader Provenance provenance,
                             @ApiParam(value = "Patient resource ID", required = true) @PathParam("patientID") UUID patientId,
                             @QueryParam("_since") @NoHtml String sinceParam,
                             @Context HttpServletRequest request,
                             @HeaderParam(FHIRHeaders.PREFER_HEADER) @DefaultValue("") String preferHeader) {
        final Provenance.ProvenanceAgentComponent performer = FHIRExtractors.getProvenancePerformer(provenance);
        final UUID practitionerId = FHIRExtractors.getEntityUUID(performer.getOnBehalfOfReference().getReference());
        Practitioner practitioner = this.client
                .read()
                .resource(Practitioner.class)
                .withId(practitionerId.toString())
                .encodedJson()
                .execute();

        if (practitioner == null) {
            // Is this the best code to be throwing here?
            throw new WebApplicationException(HttpStatus.UNAUTHORIZED_401);
        }

        final Patient patient = getPatient(patientId);
        final var since = handleSinceQueryParam(sinceParam);
        final String patientMbi = FHIRExtractors.getPatientMBI(patient);
        final UUID orgId = organization.getID();
        final Organization org = this.client
                .read()
                .resource(Organization.class)
                .withId(orgId.toString())
                .encodedJson()
                .execute();
        final String orgNPI = FHIRExtractors.findMatchingIdentifier(org.getIdentifier(), DPCIdentifierSystem.NPPES).getValue();
        final String practitionerNPI = FHIRExtractors.findMatchingIdentifier(practitioner.getIdentifier(), DPCIdentifierSystem.NPPES).getValue();

        final String requestingIP = APIHelpers.fetchRequestingIP(request);
        final String requestUrl = APIHelpers.fetchRequestUrl(request);

        if(preferHeader.equals(FHIRHeaders.PREFER_RESPOND_ASYNC)) {
            // Submit asynchronous job
            final UUID jobID = this.dataService.createJob(
                orgId,
                orgNPI,
                practitionerNPI,
                List.of(patientMbi),
                List.of(DPCResourceType.Patient, DPCResourceType.ExplanationOfBenefit, DPCResourceType.Coverage),
                since,
                APIHelpers.fetchTransactionTime(bfdClient),
                requestingIP,
                requestUrl,
                false,
                false
            );
            return Response.status(Response.Status.ACCEPTED).contentLocation(URI.create(this.baseURL + "/Jobs/" + jobID)).build();
        } else {
            // Submit synchronous job
            Resource result = dataService.retrieveData(orgId, orgNPI, practitionerNPI, List.of(patientMbi), since, APIHelpers.fetchTransactionTime(bfdClient),
                requestingIP, requestUrl, DPCResourceType.Patient, DPCResourceType.ExplanationOfBenefit, DPCResourceType.Coverage);
            if (DPCResourceType.Bundle.getPath().equals(result.getResourceType().getPath())) {
                // A Bundle containing patient data was returned
                return Response.status(Response.Status.OK).entity(result).build();
            }
            if (DPCResourceType.OperationOutcome.getPath().equals(result.getResourceType().getPath())) {
                // An OperationOutcome (ERROR) was returned
                OperationOutcome operationOutcome = (OperationOutcome) result;
                throw new ForbiddenOperationException(operationOutcome.getIssueFirstRep().getDetails().getText(), operationOutcome);
            }
        }

        throw new WebApplicationException(HttpStatus.INTERNAL_SERVER_ERROR_500);
    }

    @DELETE
    @FHIR
    @Path("/{patientID}")
    @PathAuthorizer(type = DPCResourceType.Patient, pathParam = "patientID")
    @Timed
    @ExceptionMetered
    @ApiOperation(value = "Delete Patient", notes = "Remove specific Patient record")
    @ApiResponses(@ApiResponse(code = 404, message = "Unable to find Patient to delete"))
    @Override
    public Response deletePatient(@ApiParam(value = "Patient resource ID", required = true) @PathParam("patientID") UUID patientID) {
        this.client
                .delete()
                .resourceById("Patient", patientID.toString())
                .encodedJson()
                .execute();

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
    @Override
    public Patient updatePatient(@ApiParam(value = "Patient resource ID", required = true) @PathParam("patientID") UUID patientID, @Valid @Profiled @ApiParam Patient patient) {
        final MethodOutcome outcome = this.client
                .update()
                .resource(patient)
                .withId(new IdType("Patient", patientID.toString()))
                .encodedJson()
                .execute();

        final Patient resource = (Patient) outcome.getResource();
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
    @Override
    public IBaseOperationOutcome validatePatient(@Auth @ApiParam(hidden = true) OrganizationPrincipal organization, @ApiParam Parameters parameters) {
        return ValidationHelpers.validateAgainstProfile(this.validator, parameters, PatientProfile.PROFILE_URI);
    }

    private static Optional<WebApplicationException> validateAndAddOrg(Patient patient, String organizationID, FhirValidator validator) {
        // Set the Managing Org, since we need it for the validation
        patient.setManagingOrganization(new Reference(new IdType("Organization", organizationID)));
        final ValidationResult result = validator.validateWithResult(patient, new ValidationOptions().addProfile(PatientProfile.PROFILE_URI));
        if ((!result.isSuccessful()) && (result.getMessages().get(0).getSeverity() != ResultSeverityEnum.INFORMATION)) {
            return Optional.of(new WebApplicationException(APIHelpers.formatValidationMessages(result.getMessages()), HttpStatus.UNPROCESSABLE_ENTITY_422));
        } else {
            return Optional.empty();
        }
    }

    private IQuery<Bundle> buildPatientSearchQuery(String orgId, @Nullable String patientMBI) {
        IQuery<Bundle> query = this.client
                .search()
                .forResource(Patient.class)
                .encodedJson()
                .where(Patient.ORGANIZATION.hasId(orgId))
                .returnBundle(Bundle.class);
        if (patientMBI != null && !patientMBI.isEmpty()) {
            // Handle MBI parsing
            // This should come out as part of DPC-432
            final String expandedMBI;
            if (IDENTIFIER_PATTERN.matcher(patientMBI).matches()) {
                expandedMBI = patientMBI;
            } else {
                expandedMBI = String.format("%s|%s", DPCIdentifierSystem.MBI.getSystem(), patientMBI);
            }
            query = query.where(Patient.IDENTIFIER.exactly().identifier(expandedMBI));
        }

        return query;
    }
}
