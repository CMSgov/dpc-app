package gov.cms.dpc.web.resources.v1;

import gov.cms.dpc.queue.JobQueue;
import gov.cms.dpc.queue.JobStatus;
import gov.cms.dpc.web.resources.AbstractJobResource;
import org.eclipse.jetty.http.HttpStatus;

import javax.inject.Inject;
import javax.ws.rs.core.Response;
import java.util.Optional;
import java.util.UUID;

public class JobResource extends AbstractJobResource {

    private final JobQueue queue;

    @Inject
    public JobResource(JobQueue queue) {
        this.queue = queue;
    }

    @Override
    public Response checkJobStatus(String jobID) {
        final Optional<JobStatus> jobStatus = this.queue.getJobStatus(UUID.fromString(jobID));

        if (jobStatus.isEmpty()) {
            return Response.status(HttpStatus.NOT_FOUND_404).entity("Could not find job").build();
        }

        Response.ResponseBuilder builder = Response.noContent();

        switch (jobStatus.get()) {
            case RUNNING: {
                builder = builder.status(HttpStatus.ACCEPTED_202).header("X-Progress", jobStatus.get());
                break;
            }
            case COMPLETED: {
                builder = builder.status(HttpStatus.OK_200);
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
    }
}
