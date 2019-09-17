package gov.cms.dpc.api.resources.v1;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import gov.cms.dpc.api.auth.OrganizationPrincipal;
import gov.cms.dpc.api.models.JobCompletionModel;
import gov.cms.dpc.api.resources.AbstractJobResource;
import gov.cms.dpc.common.annotations.APIV1;
import gov.cms.dpc.fhir.FHIRExtractors;
import gov.cms.dpc.queue.JobQueueInterface;
import gov.cms.dpc.queue.JobStatus;
import gov.cms.dpc.queue.exceptions.JobQueueFailure;
import gov.cms.dpc.queue.models.JobQueueBatch;
import gov.cms.dpc.queue.models.JobQueueBatchFile;
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
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * See https://github.com/smart-on-fhir/fhir-bulk-data-docs/blob/master/export.md for details.
 */
@Api(tags = {"Bulk Data", "Job"})
public class JobResource extends AbstractJobResource {

    private static final Logger logger = LoggerFactory.getLogger(JobResource.class);

    private final JobQueueInterface queue;
    private final String baseURL;

    @Inject
    public JobResource(JobQueueInterface queue, @APIV1 String baseURL) {
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
                    "When the job is in progress, the API returns a 202 status." +
                    "When completed, an output response is returned, which contains the necessary metadata for retrieving any output files.")
    @ApiResponses({
            @ApiResponse(code = 202, message = "Export job is in progress. X-Progress header is present with the format \"<STATUS>: <50.00%>\""),
            @ApiResponse(code = 404, message = "Export job cannot be found"),
            @ApiResponse(code = 500, message = "Export job has failed with no results"),
            @ApiResponse(code = 200, message = "Export job has completed. Any failures are listed in the response body", response = JobCompletionModel.class)
    })
    public Response checkJobStatus(@Auth OrganizationPrincipal organizationPrincipal, @PathParam("jobID") String jobID) {
        final UUID jobUUID = UUID.fromString(jobID);
        final UUID orgUUID = FHIRExtractors.getEntityUUID(organizationPrincipal.getOrganization().getId());
        final List<JobQueueBatch> batches = this.queue.getJobBatches(jobUUID);

        if ( batches.isEmpty() ) {
            return Response.status(HttpStatus.NOT_FOUND_404).entity("Could not find job").build();
        }

        // Validate the batches
        for ( JobQueueBatch batch : batches ) {
            logger.debug("Fetched Batch: {}", batch);
            if (!batch.getOrgID().equals(orgUUID)) {
                return Response.status(HttpStatus.UNAUTHORIZED_401).entity("Invalid organization for job").build();
            }
            if (!batch.isValid()) {
                throw new JobQueueFailure(jobUUID, batch.getBatchID(), "Fetched an invalid job model");
            }
        }

        Response.ResponseBuilder builder = Response.noContent();
        Set<JobStatus> jobStatusSet = batches.stream().map(JobQueueBatch::getStatus).collect(Collectors.toSet());

        if ( jobStatusSet.contains(JobStatus.FAILED) ) {
            // If any part of the job has failed, report a failed status
            builder = builder.status(HttpStatus.INTERNAL_SERVER_ERROR_500);
        } else if ( jobStatusSet.contains(JobStatus.RUNNING) || jobStatusSet.contains(JobStatus.QUEUED) ) {
            // The job is still being processed
            String progress = "QUEUED: 0.00%";
            if ( jobStatusSet.contains(JobStatus.RUNNING) ) {
                AtomicInteger done = new AtomicInteger();
                AtomicInteger total = new AtomicInteger();
                for ( JobQueueBatch batch : batches ) {
                    batch.getPatientIndex().ifPresent(value -> done.addAndGet(value + 1));
                    total.addAndGet(batch.getPatients().size());
                }
                progress = String.format("RUNNING: %.2f%%", total.get() > 0 ? (done.get() * 100.0f) / total.get() : 0f);
            }
            builder = builder.header("X-Progress", progress)
                    .status(HttpStatus.ACCEPTED_202);
        } else if ( jobStatusSet.size() == 1 && jobStatusSet.contains(JobStatus.COMPLETED) ) {
            // All batches in the job have finished
            JobQueueBatch firstBatch = batches.get(0);
            final String resourceQueryParam = firstBatch.getResourceTypes().stream()
                    .map(ResourceType::toString)
                    .collect(Collectors.joining(GroupResource.LIST_DELIMITER));
            final JobCompletionModel completionModel = new JobCompletionModel(
                    batches.stream().map(JobQueueBatch::getStartTime).map(Optional::get).min(OffsetDateTime::compareTo).orElseThrow(),
                    String.format("%s/Group/%s/$export?_type=%s", baseURL, firstBatch.getProviderID(), resourceQueryParam),
                    formOutputList(batches, false),
                    formOutputList(batches, true));
            builder = builder.status(HttpStatus.OK_200).entity(completionModel);
        } else {
            builder = builder.status(HttpStatus.ACCEPTED_202);
        }

        return builder.build();
    }

    /**
     * Form a list of output entries for the output file
     *
     * @param batchList              - The list of all batches in a job
     * @param forOperationalOutcomes - Only return operational outcomes if true, don't include them otherwise
     * @return the list of OutputEntry
     */
    private List<JobCompletionModel.OutputEntry> formOutputList(List<JobQueueBatch> batchList, boolean forOperationalOutcomes) {
        // Assert batches are from the same job
        assert ( batchList.stream().map(JobQueueBatch::getJobID).collect(Collectors.toSet()).size() == 1 );

        return batchList.stream()
                .map(JobQueueBatch::getJobQueueBatchFiles)
                .flatMap(List::stream)
                .map(result -> new JobCompletionModel.OutputEntry(
                        result.getResourceType(),
                        String.format("%s/Data/%s", this.baseURL, JobQueueBatchFile.formOutputFileName(result.getBatchID(), result.getResourceType(), result.getSequence())),
                        result.getCount()))
                .filter(entry -> (entry.getType() == ResourceType.OperationOutcome ^ !forOperationalOutcomes)
                        && entry.getCount() > 0)
                .collect(Collectors.toList());
    }

}
