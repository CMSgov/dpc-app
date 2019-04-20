package gov.cms.dpc.api.resources.v1;

import gov.cms.dpc.queue.JobQueue;
import gov.cms.dpc.queue.JobStatus;
import gov.cms.dpc.common.annotations.APIV1;
import gov.cms.dpc.api.models.JobCompletionModel;
import gov.cms.dpc.api.resources.AbstractJobResource;
import gov.cms.dpc.queue.models.JobModel;
import org.eclipse.jetty.http.HttpStatus;
import org.hl7.fhir.dstu3.model.ResourceType;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class JobResource extends AbstractJobResource {

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
            Response.ResponseBuilder builder = Response.noContent();
            JobStatus jobStatus = job.getStatus();
            switch (jobStatus) {
                case RUNNING: case QUEUED: {
                    builder = builder.status(HttpStatus.ACCEPTED_202).header("X-Progress", jobStatus);
                    break;
                }
                case COMPLETED: {
                    final JobCompletionModel completionModel = new JobCompletionModel(
                            Instant.now().atOffset(ZoneOffset.UTC),
                            String.format("%s/Job/%s", baseURL, jobID),
                            outputURLs(jobUUID, job.getResourceTypes()));
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
     * Form a list of output file directories
     * @return the output list for the response
     */
    private List<String> outputURLs(UUID jobID, List<ResourceType> resourceTypes) {
        return resourceTypes.stream()
                .map(resourceType -> String.format("%s/Data/%s", this.baseURL, JobModel.outputFileName(jobID, resourceType)))
                .collect(Collectors.toList());
    }
}
