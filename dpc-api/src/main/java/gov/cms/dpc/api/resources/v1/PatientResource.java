package gov.cms.dpc.api.resources.v1;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ResultSeverityEnum;
import ca.uhn.fhir.validation.ValidationOptions;
import ca.uhn.fhir.validation.ValidationResult;
import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.name.Named;
import gov.cms.dpc.api.APIHelpers;
import gov.cms.dpc.api.auth.OrganizationPrincipal;
import gov.cms.dpc.api.auth.annotations.Authorizer;
import gov.cms.dpc.api.auth.annotations.PathAuthorizer;
import gov.cms.dpc.api.resources.AbstractPatientResource;
import gov.cms.dpc.bluebutton.client.BlueButtonClient;
import gov.cms.dpc.common.annotations.NoHtml;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import gov.cms.dpc.fhir.DPCResourceType;
import gov.cms.dpc.fhir.FHIRExtractors;
import gov.cms.dpc.fhir.annotations.FHIR;
import gov.cms.dpc.fhir.annotations.Profiled;
import gov.cms.dpc.fhir.annotations.ProvenanceHeader;
import gov.cms.dpc.fhir.validations.ValidationHelpers;
import gov.cms.dpc.fhir.validations.profiles.AttestationProfile;
import gov.cms.dpc.fhir.validations.profiles.PatientProfile;
import gov.cms.dpc.queue.service.DataService;
import io.dropwizard.auth.Auth;
import io.swagger.annotations.*;
import org.eclipse.jetty.http.HttpStatus;
import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.instance.model.api.IBaseOperationOutcome;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import static gov.cms.dpc.api.APIHelpers.bulkResourceClient;
import static gov.cms.dpc.fhir.helpers.FHIRHelpers.handleMethodOutcome;

@Api(value = "Patient", authorizations = @Authorization(value = "access_token"))
@Path("/v1/Patient")
public class PatientResource extends AbstractPatientResource {

    // TODO: This should be moved into a helper class, in DPC-432.
    // This checks to see if the Identifier is fully specified or not.
    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("^[a-z0-9]+://.*$");

    private final IGenericClient client;
    private final FhirValidator validator;
    private final DataService dataService;
    private final BlueButtonClient bfdClient;

    @Inject
    public PatientResource(@Named("attribution") IGenericClient client, FhirValidator validator, DataService dataService, BlueButtonClient bfdClient) {
        this.client = client;
        this.validator = validator;
        this.dataService = dataService;
        this.bfdClient = bfdClient;
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
                                @QueryParam(value = Patient.SP_IDENTIFIER) @NoHtml String patientMBI) {

        final var request = this.client
                .search()
                .forResource(Patient.class)
                .encodedJson()
                .where(Patient.ORGANIZATION.hasId(organization.getOrganization().getId()))
                .returnBundle(Bundle.class);

        if (patientMBI != null && !patientMBI.equals("")) {

            // Handle MBI parsing
            // This should come out as part of DPC-432
            final String expandedMBI;
            if (IDENTIFIER_PATTERN.matcher(patientMBI).matches()) {
                expandedMBI = patientMBI;
            } else {
                expandedMBI = String.format("%s|%s", DPCIdentifierSystem.MBI.getSystem(), patientMBI);
            }
            return request
                    .where(Patient.IDENTIFIER.exactly().identifier(expandedMBI))
                    .execute();
        }

        return request.execute();
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
        final Consumer<Patient> entryHandler = (patient) -> validateAndAddOrg(patient, organization.getOrganization().getId(), validator);

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
    public Bundle everything(@ApiParam(hidden = true) @Auth OrganizationPrincipal organization,
                             @Valid @Profiled @ProvenanceHeader Provenance provenance,
                             @ApiParam(value = "Patient resource ID", required = true) @PathParam("patientID") UUID patientId,
                             @QueryParam("_since") @NoHtml String sinceParam,
                             @Context HttpServletRequest request) {
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
        Resource result = dataService.retrieveData(orgId, orgNPI, practitionerNPI, List.of(patientMbi), since, APIHelpers.fetchTransactionTime(bfdClient),
                requestingIP, requestUrl, DPCResourceType.Patient, DPCResourceType.ExplanationOfBenefit, DPCResourceType.Coverage);
        if (DPCResourceType.Bundle.getPath().equals(result.getResourceType().getPath())) {
            // A Bundle containing patient data was returned
            return (Bundle) result;
        }
        if (DPCResourceType.OperationOutcome.getPath().equals(result.getResourceType().getPath())) {
            // An OperationOutcome (ERROR) was returned
            OperationOutcome resultOp = (OperationOutcome) result;
            // getIssueFirstRep() grabs the first issue only - there may be others
            throw new WebApplicationException(resultOp.getIssueFirstRep().getDetails().getText());
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

    private static void validateAndAddOrg(Patient patient, String organizationID, FhirValidator validator) {
        // Set the Managing Org, since we need it for the validation
        patient.setManagingOrganization(new Reference(new IdType("Organization", organizationID)));
        final ValidationResult result = validator.validateWithResult(patient, new ValidationOptions().addProfile(PatientProfile.PROFILE_URI));
        if (!result.isSuccessful()) {
            // Temporary until DPC-536 is merged in
            if (result.getMessages().get(0).getSeverity() != ResultSeverityEnum.INFORMATION) {
                throw new WebApplicationException(APIHelpers.formatValidationMessages(result.getMessages()), HttpStatus.UNPROCESSABLE_ENTITY_422);
            }
        }
    }
}
