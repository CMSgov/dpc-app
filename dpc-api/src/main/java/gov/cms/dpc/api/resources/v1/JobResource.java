package gov.cms.dpc.api.resources.v1;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import gov.cms.dpc.api.auth.OrganizationPrincipal;
import gov.cms.dpc.api.auth.annotations.PathAuthorizer;
import gov.cms.dpc.api.models.JobCompletionModel;
import gov.cms.dpc.api.resources.AbstractJobResource;
import gov.cms.dpc.common.annotations.APIV1;
import gov.cms.dpc.fhir.FHIRExtractors;
import gov.cms.dpc.queue.JobQueue;
import gov.cms.dpc.queue.JobStatus;
import gov.cms.dpc.queue.exceptions.JobQueueFailure;
import gov.cms.dpc.queue.models.JobModel;
import gov.cms.dpc.queue.models.JobResult;
import io.dropwizard.auth.Auth;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.eclipse.jetty.http.HttpStatus;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * See https://github.com/smart-on-fhir/fhir-bulk-data-docs/blob/master/export.md for details.
 */
@Api(tags = {"Bulk Data", "Job"})
public class JobResource extends AbstractJobResource {

    private static final Logger logger = LoggerFactory.getLogger(JobResource.class);

    private final JobQueue queue;
    private final String baseURL;

    @Inject
    public JobResource(JobQueue queue, @APIV1 String baseURL) {
        this.queue = queue;
        this.baseURL = baseURL;
    }

    @Override
    @Path("/{jobID}")
    @GET
    @Timed
    @ExceptionMetered
    @ApiOperation(value = "Check export job status",
            notes = "This endpoint is used to query the status of a given Export operation. " +
                    "When the job is in progress, the API returns a 204 status." +
                        "When completed, an output response is returned, which contains the necessary metadata for retrieving any output files.")
    @ApiResponses({
            @ApiResponse(code = 202, message = "Export job is in progress"),
            @ApiResponse(code = 404, message = "Export job cannot be found"),
            @ApiResponse(code = 500, message = "Export job has failed with no results"),
            @ApiResponse(code = 200, message = "Export job has completed. Any failures are listed in the response body", response = JobCompletionModel.class)
    })
    public Response checkJobStatus(@Auth OrganizationPrincipal organizationPrincipal, @PathParam("jobID") String jobID) {
        final UUID jobUUID = UUID.fromString(jobID);
        final UUID orgUUID = FHIRExtractors.getEntityUUID(organizationPrincipal.getOrganization().getId());
        final Optional<JobModel> maybeJob = this.queue.getJob(jobUUID);

        // Return a response based on status
        return maybeJob.map(job -> {
            logger.debug("Fetched Job: {}", job);
            if (!job.isValid()) {
                throw new JobQueueFailure(jobUUID, "Fetched an invalid job model");
            }
            if (!job.getOrgID().equals(orgUUID)) {
                Response.status(HttpStatus.UNAUTHORIZED_401).entity("Invalid organization for job").build();
            }
            Response.ResponseBuilder builder = Response.noContent();
            JobStatus jobStatus = job.getStatus();
            switch (jobStatus) {
                case RUNNING:
                case QUEUED: {
                    builder = builder.status(HttpStatus.ACCEPTED_202).header("X-Progress", jobStatus);
                    break;
                }
                case COMPLETED: {
                    assert (job.getCompleteTime().isPresent());
                    final String resourceQueryParam = job.getResourceTypes().stream()
                            .map(ResourceType::toString)
                            .collect(Collectors.joining(GroupResource.LIST_DELIMITER));
                    final JobCompletionModel completionModel = new JobCompletionModel(
                            job.getStartTime().orElseThrow(),
                            String.format("%s/Group/%s/$export?_type=%s", baseURL, job.getProviderID(), resourceQueryParam),
                            formOutputList(job, false),
                            formOutputList(job, true));
                    builder = builder.status(HttpStatus.OK_200).entity(completionModel);
                    break;
                }
                case FAILED: {
                    builder = builder.status(HttpStatus.INTERNAL_SERVER_ERROR_500);
                    break;
                }
                default: {
                    builder = builder.status(HttpStatus.ACCEPTED_202);
                }
            }
            return builder.build();
        }).orElse(Response.status(HttpStatus.NOT_FOUND_404).entity("Could not find job").build());
    }

    /**
     * Form a list of output entries for the output file
     *
     * @param job                    - The job with its job result list
     * @param forOperationalOutcomes - Only return operational outcomes if true, don't include them otherwise
     * @return the list of OutputEntry
     */
    private List<JobCompletionModel.OutputEntry> formOutputList(JobModel job, boolean forOperationalOutcomes) {
        return job.getJobResults().stream()
                .map(result -> new JobCompletionModel.OutputEntry(
                        result.getResourceType(),
                        String.format("%s/Data/%s", this.baseURL, JobResult.formOutputFileName(result.getJobID(), result.getResourceType(), result.getSequence())),
                        result.getCount()))
                .filter(entry -> (entry.getType() == ResourceType.OperationOutcome ^ !forOperationalOutcomes)
                        && entry.getCount() > 0)
                .collect(Collectors.toList());
    }

}
