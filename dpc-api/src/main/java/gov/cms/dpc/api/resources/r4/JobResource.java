package gov.cms.dpc.api.resources.r4;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import gov.cms.dpc.api.auth.OrganizationPrincipal;
import gov.cms.dpc.api.auth.annotations.Authorizer;
import gov.cms.dpc.api.resources.AbstractJobResource;
import gov.cms.dpc.common.annotations.APIV3;
import gov.cms.dpc.common.annotations.NoHtml;
import gov.cms.dpc.common.models.JobCompletionModel;
import gov.cms.dpc.fhir.DPCResourceType;
import gov.cms.dpc.fhir.stu3.FHIRExtractors;
import gov.cms.dpc.oneecosystem.client.OneEcosystemClient;
import gov.cms.dpc.oneecosystem.models.OneEcosystemJobStatus;
import gov.cms.dpc.oneecosystem.models.OneEcosystemJobStatusOutput;
import gov.cms.dpc.oneecosystem.models.OneEcosystemJobStatusResponse;
import gov.cms.dpc.queue.IJobQueue;
import gov.cms.dpc.queue.exceptions.JobQueueFailure;
import gov.cms.dpc.queue.models.JobQueueBatch;
import io.dropwizard.auth.Auth;
import io.swagger.annotations.*;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;
import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Api(tags = {"Bulk Data", "Job"}, authorizations = @Authorization(value = "access_token"))
@Path("/v3/Jobs")
public class JobResource extends AbstractJobResource {

    private static final Logger logger = LoggerFactory.getLogger(JobResource.class);

    private final IJobQueue queue;
    private final OneEcosystemClient client;
    private final String baseURL;

    @Inject
    public JobResource(IJobQueue queue, OneEcosystemClient client, @APIV3 String baseURL) {
        this.queue = queue;
        this.client = client;
        this.baseURL = baseURL;
    }

    @Override
    @Path("/{jobID}")
    @GET
    @Timed
    @ExceptionMetered
    @Authorizer
    @ApiOperation(value = "Check export job status")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Export job has completed. Any failures are listed in the response body", response = JobCompletionModel.class),
            @ApiResponse(code = 202, message = "Export job is in progress."),
            @ApiResponse(code = 401, message = "Invalid organization for job"),
            @ApiResponse(code = 404, message = "Export job cannot be found"),
            @ApiResponse(code = 500, message = "Export job has failed")
    })
    public Response checkJobStatus(@Auth OrganizationPrincipal organizationPrincipal, @PathParam("jobID") @NoHtml String jobID) {
        final UUID jobUUID = UUID.fromString(jobID);
        final UUID orgUUID = FHIRExtractors.getEntityUUID(organizationPrincipal.getOrganization().getId());
        final List<JobQueueBatch> batches = this.queue.getJobBatches(jobUUID);

        if (batches.isEmpty()) {
            return Response.status(HttpStatus.NOT_FOUND_404).entity("Could not find job").build();
        }

        // Validate the batches
        for (JobQueueBatch batch : batches) {
            logger.debug("Fetched Batch: {}", batch);
            if (!batch.getOrgID().equals(orgUUID)) {
                return Response.status(HttpStatus.UNAUTHORIZED_401).entity("Invalid organization for job").build();
            }
            if (!batch.isValid()) {
                throw new JobQueueFailure(jobUUID, batch.getBatchID(), "Fetched an invalid job model");
            }
        }

        // Get JobStatus from the external client
        final JobQueueBatch batch = batches.get(0);
        final OneEcosystemJobStatus status = this.client.jobStatus(jobUUID);

        Response.ResponseBuilder builder = Response.noContent();

        if (status.statusCode() == HttpStatus.INTERNAL_SERVER_ERROR_500) {
            // Job is reported as failed
            throw new JobQueueFailure(jobUUID, batch.getBatchID(), "Batch failed");
        } else if (status.statusCode() == HttpStatus.ACCEPTED_202) {
            // The job is still being processed
            builder = buildJobStatusInProgress(builder);
        } else if (status.statusCode() == HttpStatus.OK_200) {
            // All batches in the job have finished
            builder = buildJobStatusCompleted(builder, status.body(), batch.getRequestUrl());
        } else if (status.statusCode() == HttpStatus.BAD_REQUEST_400) {
            // Backend doesn't recognize the job ID
            builder = builder.status(HttpStatus.NOT_FOUND_404).entity("Could not find job");
        } else {
            // Unrecognized status
            logger.error("Export job {} returned unrecognized backend status code {}", jobUUID, status.statusCode());
            throw new JobQueueFailure(jobUUID, batch.getBatchID(), "Export job returned unrecognized status code: " + status.statusCode());
        }

        return builder.build();
    }

    private Response.ResponseBuilder buildJobStatusInProgress(Response.ResponseBuilder builder) {
        String progress = "QUEUED: 0.00%";

        // get progress from backend response

        return builder.header("X-Progress", progress).status(HttpStatus.ACCEPTED_202);
    }

    /**
     * Builds a job status response for a completed job.
     *
     * @param builder    - the current response builder
     * @param response   - the backend's completed-job response body
     * @param requestUrl - the original export request URL, as persisted on the batch
     * @return the response builder
     */
    private Response.ResponseBuilder buildJobStatusCompleted(Response.ResponseBuilder builder, OneEcosystemJobStatusResponse response, String requestUrl) {
        OffsetDateTime transactionTime = OffsetDateTime.ofInstant(response.transactionTime(), ZoneOffset.UTC);
        final List<JobCompletionModel.OutputEntry> outputEntries = response.output().stream()
                .map(this::toOutputEntry)
                .toList();

        if (!response.error().isEmpty()) {
            logger.warn("Export job {} reported errors: {}", response.JobID(), response.error());
        }

        final JobCompletionModel completionModel = new JobCompletionModel(
                transactionTime,
                requestUrl,
                outputEntries,
                List.of(),
                List.of());

        return builder.status(HttpStatus.OK_200).entity(completionModel);
    }

    private JobCompletionModel.OutputEntry toOutputEntry(OneEcosystemJobStatusOutput output) {
        final DPCResourceType type = resolveResourceType(output.type());
        return new JobCompletionModel.OutputEntry(type, this.baseURL + output.url(), 0, List.of());
    }

    private DPCResourceType resolveResourceType(String resourceTypeName) {
        return Arrays.stream(DPCResourceType.values())
                .filter(type -> type.getPath().equalsIgnoreCase(resourceTypeName))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Unknown resource type returned by export backend: " + resourceTypeName));
    }
}
