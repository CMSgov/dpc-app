package gov.cms.dpc.api.resources.v1;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import gov.cms.dpc.api.auth.OrganizationPrincipal;
import gov.cms.dpc.api.auth.annotations.Authorizer;
import gov.cms.dpc.api.resources.AbstractJobResource;
import gov.cms.dpc.common.annotations.APIV1;
import gov.cms.dpc.common.annotations.NoHtml;
import gov.cms.dpc.common.models.JobCompletionModel;
import gov.cms.dpc.fhir.DPCResourceType;
import gov.cms.dpc.fhir.FHIRExtractors;
import gov.cms.dpc.queue.IJobQueue;
import gov.cms.dpc.queue.JobStatus;
import gov.cms.dpc.queue.exceptions.JobQueueFailure;
import gov.cms.dpc.queue.models.JobQueueBatch;
import gov.cms.dpc.queue.models.JobQueueBatchFile;
import io.dropwizard.auth.Auth;
import io.swagger.annotations.*;
import org.bouncycastle.util.encoders.Hex;
import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * See https://github.com/smart-on-fhir/fhir-bulk-data-docs/blob/master/export.md for details.
 */
@Api(tags = {"Bulk Data", "Job"}, authorizations = @Authorization(value = "access_token"))
@Path("/v1/Jobs")
public class JobResource extends AbstractJobResource {

    private static final Logger logger = LoggerFactory.getLogger(JobResource.class);

    public static final int JOB_EXPIRATION_HOURS = 24;
    public static final DateTimeFormatter HTTP_DATE_FORMAT =
            DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss z", Locale.US).withZone(ZoneId.of("GMT"));

    private final IJobQueue queue;
    private final String baseURL;

    @Inject
    public JobResource(IJobQueue queue, @APIV1 String baseURL) {
        this.queue = queue;
        this.baseURL = baseURL;
    }

    @Override
    @Path("/{jobID}")
    @GET
    @Timed
    @ExceptionMetered
    @Authorizer
    @ApiOperation(value = "Check export job status",
            notes = "This endpoint is used to query the status of a given Export operation. " +
                    "When the job is in progress, the API returns a 202 status." +
                    "When completed, an output response is returned, which contains the necessary metadata for retrieving any output files.")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Export job has completed. Any failures are listed in the response body", response = JobCompletionModel.class),
            @ApiResponse(code = 202, message = "Export job is in progress. X-Progress header is present with the format \"<STATUS>: <50.00%>\""),
            @ApiResponse(code = 404, message = "Export job cannot be found"),
            @ApiResponse(code = 410, message = "Job has expired"),
            @ApiResponse(code = 500, message = "Export job has failed with no results")
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

        Response.ResponseBuilder builder = Response.noContent();
        Set<JobStatus> jobStatusSet = batches.stream().map(JobQueueBatch::getStatus).collect(Collectors.toSet());

        if (jobStatusSet.contains(JobStatus.FAILED)) {
            // If any part of the job has failed, report a failed status
            builder = builder.status(HttpStatus.INTERNAL_SERVER_ERROR_500);
        } else if (jobStatusSet.contains(JobStatus.RUNNING) || jobStatusSet.contains(JobStatus.QUEUED)) {
            // The job is still being processed
            builder = buildJobStatusInProgress(builder, batches, jobStatusSet);
        } else if (jobStatusSet.size() == 1 && jobStatusSet.contains(JobStatus.COMPLETED)) {
            // All batches in the job have finished
            builder = buildJobStatusCompleted(builder, batches);
        } else {
            builder = builder.status(HttpStatus.ACCEPTED_202);
        }

        return builder.build();
    }

    /**
     * Builds a job status response for an in progress job. Includes the current progress in the X-Progress header.
     *
     * @param builder      - The current response builder
     * @param batches      - The list of batches made up in a job
     * @param jobStatusSet - The list of all possible statuses in the job
     * @return the response builder
     */
    private Response.ResponseBuilder buildJobStatusInProgress(Response.ResponseBuilder builder, List<JobQueueBatch> batches, Set<JobStatus> jobStatusSet) {
        String progress = "QUEUED: 0.00%";

        if (jobStatusSet.contains(JobStatus.RUNNING) || jobStatusSet.contains(JobStatus.COMPLETED)) {
            final int processedPatients = batches.stream().mapToInt(JobQueueBatch::getPatientsProcessed).sum();
            final int totalPatients = batches.stream().mapToInt(b -> b.getPatients().size()).sum();
            progress = String.format("RUNNING: %.2f%%", totalPatients > 0 ? (processedPatients * 100.0f) / totalPatients : 0f);
        }

        return builder.header("X-Progress", progress).status(HttpStatus.ACCEPTED_202);
    }

    /**
     * Builds a job status response for a completed job. Includes the list of files created from the job.
     *
     * @param builder - The current response builder
     * @param batches - The list of batches made up in a job
     * @return the response builder
     */
    private Response.ResponseBuilder buildJobStatusCompleted(Response.ResponseBuilder builder, List<JobQueueBatch> batches) {
        OffsetDateTime lastCompleteTime = getLatestBatchCompleteTime(batches);

        if (lastCompleteTime.isBefore(OffsetDateTime.now(ZoneOffset.UTC).minusHours(JOB_EXPIRATION_HOURS))) {
            return builder.status(HttpStatus.GONE_410);
        }

        builder.header(HttpHeaders.EXPIRES, lastCompleteTime.plusDays(1).format(HTTP_DATE_FORMAT));

        JobQueueBatch firstBatch = batches.get(0);

        final JobCompletionModel completionModel = new JobCompletionModel(
                firstBatch.getTransactionTime(),
                firstBatch.getRequestUrl(),
                formOutputList(batches, false),
                formOutputList(batches, true),
                buildJobExtension(batches));

        return builder.status(HttpStatus.OK_200).entity(completionModel);
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
        assert (batchList.stream().map(JobQueueBatch::getJobID).collect(Collectors.toSet()).size() == 1);

        return batchList.stream()
                .map(JobQueueBatch::getJobQueueBatchFiles)
                .flatMap(List::stream)
                .map(result -> new JobCompletionModel.OutputEntry(
                        result.getResourceType(),
                        String.format("%s/Data/%s.ndjson", this.baseURL, JobQueueBatchFile.formOutputFileName(result.getBatchID(), result.getResourceType(), result.getSequence())),
                        result.getCount(), buildOutputEntryExtension(result)))
                .filter(entry -> (entry.getType() == DPCResourceType.OperationOutcome ^ !forOperationalOutcomes)
                        && entry.getCount() > 0)
                .collect(Collectors.toList());
    }

    List<JobCompletionModel.FhirExtension> buildOutputEntryExtension(JobQueueBatchFile batchFile) {
        final byte[] byteChecksum = batchFile.getChecksum();
        final String stringChecksum;
        if (byteChecksum == null) {
            stringChecksum = "";
        } else {
            stringChecksum = Hex.toHexString(byteChecksum);
        }
        String formattedChecksum = String.format("%s:%s", "sha256", stringChecksum);
        long fileLength = batchFile.getFileLength();
        return List.of(new JobCompletionModel.FhirExtension(JobCompletionModel.CHECKSUM_URL, formattedChecksum),
                new JobCompletionModel.FhirExtension(JobCompletionModel.FILE_LENGTH_URL, fileLength));
    }

    List<JobCompletionModel.FhirExtension> buildJobExtension(List<JobQueueBatch> batches) {
        final var submitTime = batches.stream()
                .map(b -> b.getSubmitTime().orElse(OffsetDateTime.MIN))
                .min(OffsetDateTime::compareTo)
                .orElse(OffsetDateTime.MIN);
        final var completeTime = batches.stream()
                .map(b -> b.getCompleteTime().orElse(OffsetDateTime.MIN))
                .max(OffsetDateTime::compareTo)
                .orElse(OffsetDateTime.MIN);
        if (submitTime.isEqual(OffsetDateTime.MIN) || completeTime.isEqual(OffsetDateTime.MIN)) {
            return Collections.emptyList();
        }
        return List.of(
                new JobCompletionModel.FhirExtension(JobCompletionModel.SUBMIT_TIME_URL, submitTime),
                new JobCompletionModel.FhirExtension(JobCompletionModel.COMPLETE_TIME_URL, completeTime));
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    public static OffsetDateTime getLatestBatchCompleteTime(List<JobQueueBatch> batches) {
        return batches.stream()
                .filter(b -> b.getCompleteTime().isPresent())
                .map(b -> b.getCompleteTime().get())
                .max(OffsetDateTime::compareTo).get();
    }
}
