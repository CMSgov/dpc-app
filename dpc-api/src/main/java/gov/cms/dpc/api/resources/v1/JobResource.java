package gov.cms.dpc.api.resources.v1;

import gov.cms.dpc.queue.JobQueue;
import gov.cms.dpc.queue.JobStatus;
import gov.cms.dpc.common.annotations.APIV1;
import gov.cms.dpc.api.models.JobCompletionModel;
import gov.cms.dpc.api.resources.AbstractJobResource;
import gov.cms.dpc.queue.exceptions.JobQueueFailure;
import gov.cms.dpc.queue.models.JobModel;
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
public class JobResource extends AbstractJobResource {

    private static final Logger logger = LoggerFactory.getLogger(JobResource.class);

    private final JobQueue queue;
    private final String baseURL;

    @Inject
    public JobResource(JobQueue queue, @APIV1 String baseURL) {
        this.queue = queue;
        this.baseURL = baseURL;
    }

    @Path("/{jobID}")
    @GET
    @Override
    public Response checkJobStatus(@PathParam("jobID") String jobID) {
        final UUID jobUUID = UUID.fromString(jobID);
        final Optional<JobModel> maybeJob = this.queue.getJob(jobUUID);

        // Return a response based on status
        return maybeJob.map(job -> {
            logger.debug("Fetched Job: {}", job);
            if (!job.isValid()) {
                throw new JobQueueFailure(jobUUID, "Fetched an invalid job model");
            }
            Response.ResponseBuilder builder = Response.noContent();
            JobStatus jobStatus = job.getStatus();
            switch (jobStatus) {
                case RUNNING: case QUEUED: {
                    builder = builder.status(HttpStatus.ACCEPTED_202).header("X-Progress", jobStatus);
                    break;
                }
                case COMPLETED: {
                    assert(job.getCompleteTime().isPresent());
                    final String resourceQueryParam = job.getResourceTypes().stream()
                            .map(ResourceType::toString)
                            .collect(Collectors.joining(GroupResource.LIST_DELIM));
                    final JobCompletionModel completionModel = new JobCompletionModel(
                            job.getStartTime().get(),
                            String.format("%s/Group/%s/$export?_type=%s", baseURL, job.getProviderID(), resourceQueryParam),
                            outputList(job),
                            errorList(job));
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
     * Form a list of output entries
     *
     * @return the output list for the response
     */
    private List<JobCompletionModel.OutputEntry> outputList(JobModel job) {
        return job.getResourceTypes().stream()
                .map(resourceType -> {
                    final var url = String.format("%s/Data/%s", this.baseURL, JobModel.outputFileName(job.getJobID(), resourceType));
                    return new JobCompletionModel.OutputEntry(resourceType, url);
                })
                .collect(Collectors.toList());
    }

    /**
     * Form a list of output entries that are erring
     *
     * @return the output list for the response
     */
    private List<JobCompletionModel.OutputEntry> errorList(JobModel job) {
        return job.getErringTypes().stream()
                .map(resourceType -> {
                    final var url = String.format("%s/Data/%s", this.baseURL, JobModel.errorFileName(job.getJobID(), resourceType));
                    return new JobCompletionModel.OutputEntry(resourceType, url);
                })
                .collect(Collectors.toList());
    }
}
