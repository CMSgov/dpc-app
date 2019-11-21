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
import gov.cms.dpc.common.annotations.ExportPath;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import gov.cms.dpc.fhir.annotations.FHIR;
import gov.cms.dpc.fhir.annotations.Profiled;
import gov.cms.dpc.fhir.validations.ValidationHelpers;
import gov.cms.dpc.fhir.validations.profiles.PatientProfile;
import gov.cms.dpc.queue.IJobQueue;
import gov.cms.dpc.queue.JobStatus;
import gov.cms.dpc.queue.exceptions.JobQueueFailure;
import gov.cms.dpc.queue.models.JobQueueBatch;
import gov.cms.dpc.queue.models.JobQueueBatchFile;
import io.dropwizard.auth.Auth;
import io.swagger.annotations.*;
import org.eclipse.jetty.http.HttpStatus;
import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.instance.model.api.IBaseOperationOutcome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static gov.cms.dpc.api.APIHelpers.bulkResourceClient;
import static gov.cms.dpc.fhir.helpers.FHIRHelpers.handleMethodOutcome;

@Api(value = "Patient", authorizations = @Authorization(value = "apiKey"))
@Path("/v1/Patient")
public class PatientResource extends AbstractPatientResource {
    private static final Logger logger = LoggerFactory.getLogger(PatientResource.class);

    // TODO: This should be moved into a helper class, in DPC-432.
    // This checks to see if the Identifier is fully specified or not.
    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("^[a-z0-9]+://.*$");
    private static final int JOB_POLLING_TIMEOUT = 3 * 5;

    private final IJobQueue queue;
    private final IGenericClient client;
    private final FhirValidator validator;
    private final String exportPath;

    @Inject
    public PatientResource(IJobQueue queue, IGenericClient client, FhirValidator validator, @ExportPath String exportPath) {
        this.queue = queue;
        this.client = client;
        this.validator = validator;
        this.exportPath = exportPath;
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
                expandedMBI = String.format("%s|%s", DPCIdentifierSystem.BENE_ID.getSystem(), patientMBI);
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
            "<p>This method always returns a 200 status, even in response to a non-conforming resource.")
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

    @GET
    @FHIR
    @Path("/{patientID}/$everything")
    @PathAuthorizer(type = ResourceType.Patient, pathParam = "patientID")
    @Timed
    @ExceptionMetered
    @ApiOperation(value = "Fetch entire Patient record", notes = "Fetch entire record for Patient with given ID synchronously. " +
            "All resources available for the Patient are included in the result bundle.")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Cannot find Patient record with given ID"),
            @ApiResponse(code = 504, message = "", response = OperationOutcome.class),
            @ApiResponse(code = 500, message = "A system error occurred", response = OperationOutcome.class)
    })
    @Override
    public Response everything(@Auth OrganizationPrincipal organization, @ApiParam(value = "Patient resource ID", required = true) @PathParam("patientID") String patientID) {
        // TODO find providerID given organization and patientID (?) or receive in macaroon?
        final String providerUUID = UUID.randomUUID().toString();           // uh oh! doesn't matter what providerID is used !?!
        final UUID orgUUID = organization.getID();                          // or organization.getOrganization.getID()? FHIRExtractor.getOrganizationID()?

        // TODO worth it to validate that patientID is valid before queueing job?

        UUID jobUUID;
        PollingStatus status;

        try {
            jobUUID = this.queue.createJob(orgUUID, providerUUID, Collections.singletonList(patientID), List.of(ResourceType.Patient, ResourceType.ExplanationOfBenefit, ResourceType.Coverage));
            status = pollUntilFinalStatus(jobUUID, orgUUID, this.queue);
        } catch (JobQueueFailure e) {
            throw new WebApplicationException("Failed to queue job", HttpStatus.INTERNAL_SERVER_ERROR_500);
        }

        List<JobQueueBatch> batches = queue.getJobBatches(jobUUID);
        if (batches.isEmpty()) {
            // why would there be no batches if the job was queued properly?
            throw new WebApplicationException("No job results available", HttpStatus.INTERNAL_SERVER_ERROR_500);
        }

        Response.ResponseBuilder builder;
        switch (status) {
            case SUCCEEDED:
                List<JobQueueBatchFile> files = batches.stream().map(JobQueueBatch::getJobQueueBatchFiles).flatMap(List::stream).collect(Collectors.toList());
                if (files.size() == 1 && files.get(0).getResourceType() == ResourceType.OperationOutcome) {
                    OperationOutcome outcome = assembleOperationOutcome(batches);
                    builder = Response.status(HttpStatus.NOT_FOUND_404).entity(outcome);
                } else {
                    Bundle bundle = assembleEverythingBundle(batches);
                    builder = Response.ok(bundle);
                }
                break;
            case TIMED_OUT:
                builder = Response.status(HttpStatus.GATEWAY_TIMEOUT_504);
                break;
            case FAILED:
                // is there an operation outcome to get here?
            default:
                builder = Response.status(HttpStatus.INTERNAL_SERVER_ERROR_500);
                break;
        }
        return builder.build();
    }

    enum PollingStatus {
        FAILED,
        SUCCEEDED,
        TIMED_OUT,
        UNKNOWN_FAILURE
    }

    PollingStatus pollUntilFinalStatus(UUID jobUUID, UUID orgUUID, IJobQueue queue) {
        CompletableFuture<PollingStatus> finalStatusFuture = new CompletableFuture<>();

        final ScheduledExecutorService poller = Executors.newSingleThreadScheduledExecutor();

        final ScheduledFuture<?> task = poller.scheduleAtFixedRate(() -> {
            JobStatus status = checkEverythingJobStatus(jobUUID, orgUUID, queue);
            if (status == JobStatus.COMPLETED) {
                finalStatusFuture.complete(PollingStatus.SUCCEEDED);
            }
            if (status == JobStatus.FAILED) {
                finalStatusFuture.complete(PollingStatus.FAILED);
            }
        }, 0, 250, TimeUnit.MILLISECONDS);

        // this timeout value should probably be adjusted according to the number of types being requested
        finalStatusFuture.completeOnTimeout(PollingStatus.TIMED_OUT, JOB_POLLING_TIMEOUT, TimeUnit.SECONDS);

        PollingStatus status;
        try {
            status = finalStatusFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            status = PollingStatus.UNKNOWN_FAILURE;
        }

        task.cancel(true);
        poller.shutdown();
        return status;
    }

    JobStatus checkEverythingJobStatus(UUID jobUUID, UUID orgUUID, IJobQueue queue) {
        final List<JobQueueBatch> batches = queue.getJobBatches(jobUUID);

        if (batches.isEmpty()) {
            return JobStatus.FAILED;
        }

        Set<JobStatus> jobStatusSet = batches
                .stream()
                .filter(b -> b.getOrgID().equals(orgUUID))
                .filter(JobQueueBatch::isValid)
                .map(JobQueueBatch::getStatus).collect(Collectors.toSet());

        logger.debug("JobStatusSet: {}", jobStatusSet);

        // This condition was copied from JobStatus. It implies that no matter how many batches
        // you start with, you wind up with only 1 one job status when all batches are completed? Correct?
        if (jobStatusSet.size() == 1 && jobStatusSet.contains(JobStatus.COMPLETED)) {
            // success
            return JobStatus.COMPLETED;
        } else if (jobStatusSet.contains(JobStatus.FAILED)) {
            return JobStatus.FAILED;
        } else {
            // it might actually be queued or running, but we don't care about that distinction here
            return JobStatus.RUNNING;
        }
    }

    OperationOutcome assembleOperationOutcome(List<JobQueueBatch> batches) {
        // There is only ever 1 OperationOutcome file
        final JobQueueBatchFile batchFile = batches.stream()
                .map(JobQueueBatch::getJobQueueBatchFiles)
                .flatMap(List::stream)
                .filter(bf -> bf.getResourceType() == ResourceType.OperationOutcome)
                .findFirst().get();

        OperationOutcome outcome = new OperationOutcome();
        java.nio.file.Path path = Paths.get(String.format("%s/%s.ndjson", exportPath, batchFile.getFileName()));
        try (BufferedReader br = Files.newBufferedReader(path)) {
            br.lines()
                    .map(line -> client.getFhirContext().newJsonParser().parseResource(OperationOutcome.class, line))
                    .map(OperationOutcome::getIssue)
                    .flatMap(List::stream)
                    .forEach(outcome::addIssue);
        } catch (IOException e) {
            throw new WebApplicationException(String.format("Unable to read OperationOutcome because %s", e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR_500);
        }

        return outcome;
    }

    Bundle assembleEverythingBundle(List<JobQueueBatch> batches) {
        final Bundle bundle = new Bundle().setType(Bundle.BundleType.SEARCHSET);

        batches.stream()
                .map(JobQueueBatch::getJobQueueBatchFiles)
                .flatMap(List::stream)
                .forEach(batchFile -> {
                    java.nio.file.Path path = Paths.get(String.format("%s/%s.ndjson", exportPath, batchFile.getFileName()));
                    addResourceEntries(Resource.class, path, bundle);
                });

        // set a bundle id here? anything else?
        bundle.setId(UUID.randomUUID().toString());
        return bundle.setTotal(bundle.getEntry().size());
    }

    private void addResourceEntries(Class<? extends Resource> clazz, java.nio.file.Path path, Bundle bundle) {
        try (BufferedReader br = Files.newBufferedReader(path)) {
            br.lines().forEach(line -> {
                Resource r = client.getFhirContext().newJsonParser().parseResource(clazz, line);
                bundle.addEntry().setResource(r);
            });
        } catch (IOException e) {
            throw new WebApplicationException(String.format("Unable to read resource because %s", e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR_500);
        }
    }
}
