package gov.cms.dpc.api.resources.v1;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ResultSeverityEnum;
import ca.uhn.fhir.validation.ValidationOptions;
import ca.uhn.fhir.validation.ValidationResult;
import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import gov.cms.dpc.api.APIHelpers;
import gov.cms.dpc.api.auth.OrganizationPrincipal;
import gov.cms.dpc.api.auth.annotations.PathAuthorizer;
import gov.cms.dpc.api.resources.AbstractPatientResource;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import gov.cms.dpc.fhir.annotations.FHIR;
import gov.cms.dpc.fhir.annotations.Profiled;
import gov.cms.dpc.fhir.validations.ValidationHelpers;
import gov.cms.dpc.fhir.validations.profiles.PatientProfile;
import io.dropwizard.auth.Auth;
import io.swagger.annotations.*;
import org.eclipse.jetty.http.HttpStatus;
import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.instance.model.api.IBaseOperationOutcome;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import static gov.cms.dpc.api.APIHelpers.bulkResourceClient;
import static gov.cms.dpc.fhir.helpers.FHIRHelpers.handleMethodOutcome;

@Api(value = "Patient", authorizations = @Authorization(value = "apiKey"))
@Path("/v1/Patient")
public class PatientResource extends AbstractPatientResource {

    // TODO: This should be moved into a helper class, in DPC-432.
    // This checks to see if the Identifier is fully specified or not.
    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("^[a-z0-9]+://.*$");

    private final IGenericClient client;
    private final FhirValidator validator;

    @Inject
    PatientResource(IGenericClient client, FhirValidator validator) {
        this.client = client;
        this.validator = validator;
    }

    @GET
    @FHIR
    @Timed
    @ExceptionMetered
    @ApiOperation(value = "Search for Patients", notes = "FHIR endpoint for searching for Patient resources." +
            "<p> If Patient Identifier is provided, results will be filtered to match the given property")
    @Override
    public Bundle patientSearch(@ApiParam(hidden = true)
                                @Auth OrganizationPrincipal organization,
                                @ApiParam(value = "Patient MBI")
                                @QueryParam(value = Patient.SP_IDENTIFIER) String patientMBI) {

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
    @ExceptionMetered
    @ApiOperation(value = "Create Patient", notes = "Create a Patient record associated to the Organization.")
    @ApiResponses(@ApiResponse(code = 422, message = "Patient does not satisfy the required FHIR profile"))
    @Override
    public Response submitPatient(@ApiParam(hidden = true) @Auth OrganizationPrincipal organization, @Valid @Profiled(profile = PatientProfile.PROFILE_URI) Patient patient) {

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
    @ApiOperation(value = "Bulk submit Patient resources", notes = "FHIR operation for submitting a Bundle of Patient resources, which will be associated to the given Organization." +
            "<p> Each Patient resource MUST implement the " + PatientProfile.PROFILE_URI + "profile.")
    @ApiResponses(@ApiResponse(code = 422, message = "Patient does not satisfy the required FHIR profile"))
    @Override
    public Bundle bulkSubmitPatients(@Auth OrganizationPrincipal organization, Parameters params) {
        final Bundle patientBundle = (Bundle) params.getParameterFirstRep().getResource();
        final Consumer<Patient> entryHandler = (patient) -> validateAndAddOrg(patient, organization.getOrganization().getId(), validator, PatientProfile.PROFILE_URI);

        return bulkResourceClient(Patient.class, client, entryHandler, patientBundle);
    }


    @GET
    @FHIR
    @Path("/{patientID}")
    @PathAuthorizer(type = ResourceType.Patient, pathParam = "patientID")
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

    @DELETE
    @FHIR
    @Path("/{patientID}")
    @PathAuthorizer(type = ResourceType.Patient, pathParam = "patientID")
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
    @PathAuthorizer(type = ResourceType.Patient, pathParam = "patientID")
    @FHIR
    @Timed
    @ExceptionMetered
    @ApiOperation(value = "Update Patient record", notes = "Update specific Patient record." +
            "<p>Currently, this method only allows for updating of the Patient first/last name, and BirthDate.")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Unable to find Patient to update"),
            @ApiResponse(code = 422, message = "Patient does not satisfy the required FHIR profile")
    })
    @Override
    public Patient updatePatient(@ApiParam(value = "Patient resource ID", required = true) @PathParam("patientID") UUID patientID, @Valid @Profiled(profile = PatientProfile.PROFILE_URI) Patient patient) {
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
    @ApiOperation(value = "Validate Patient resource", notes = "Validates the given resource against the " + PatientProfile.PROFILE_URI + " profile." +
            "<p>This method always returns a 200 status, even in respond to a non-conformant resource.")
    @Override
    public IBaseOperationOutcome validatePatient(@Auth @ApiParam(hidden = true) OrganizationPrincipal organization, Parameters parameters) {
        return ValidationHelpers.validateAgainstProfile(this.validator, parameters, PatientProfile.PROFILE_URI);
    }

    private static void validateAndAddOrg(Patient patient, String organizationID, FhirValidator validator, String profileURL) {
        {
            // Set the Managing Org, since we need it for the validation
            patient.setManagingOrganization(new Reference(new IdType("Organization", organizationID)));
            final ValidationResult result = validator.validateWithResult(patient, new ValidationOptions().addProfile(profileURL));
            if (!result.isSuccessful()) {
                // Temporary until DPC-536 is merged in
                if (result.getMessages().get(0).getSeverity() != ResultSeverityEnum.INFORMATION) {
                    throw new WebApplicationException(APIHelpers.formatValidationMessages(result.getMessages()), HttpStatus.UNPROCESSABLE_ENTITY_422);
                }
            }
        }
    }
}
