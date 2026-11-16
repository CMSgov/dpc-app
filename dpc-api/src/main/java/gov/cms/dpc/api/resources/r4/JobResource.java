package gov.cms.dpc.api.resources.r4;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import gov.cms.dpc.api.auth.OrganizationPrincipal;
import gov.cms.dpc.api.auth.annotations.Authorizer;
import gov.cms.dpc.api.resources.AbstractJobResource;
import gov.cms.dpc.common.annotations.NoHtml;
import gov.cms.dpc.common.models.JobCompletionModel;
import gov.cms.dpc.fhir.DPCResourceType;
import gov.cms.dpc.fhir.stu3.FHIRExtractors;
import gov.cms.dpc.oneecosystem.client.OneEcosystemClient;
import gov.cms.dpc.oneecosystem.models.OneEcosystemJobStatus;
import gov.cms.dpc.oneecosystem.models.OneEcosystemJobStatusOutput;
import gov.cms.dpc.oneecosystem.models.OneEcosystemJobStatusResponse;
import gov.cms.dpc.queue.IJobQueue;
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
import java.util.stream.Collectors;

@Api(tags = {"Bulk Data", "Job"}, authorizations = @Authorization(value = "access_token"))
@Path("/v3/Jobs")
public class JobResource extends AbstractJobResource {

    private static final Logger logger = LoggerFactory.getLogger(JobResource.class);

    private final OneEcosystemClient oneEcosystemClient;
    private final IJobQueue queue;

    @Inject
    public JobResource(OneEcosystemClient oneEcosystemClient, IJobQueue queue) {
        this.oneEcosystemClient = oneEcosystemClient;
        this.queue = queue;
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
            @ApiResponse(code = 404, message = "Export job cannot be found")
    })
    public Response checkJobStatus(@Auth OrganizationPrincipal organizationPrincipal, @PathParam("jobID") @NoHtml String jobID) {
        final UUID jobUUID = UUID.fromString(jobID);
        final UUID orgUUID = FHIRExtractors.getEntityUUID(organizationPrincipal.getOrganization().getId());

        final List<JobQueueBatch> batches = this.queue.getJobBatches(jobUUID);
        if (batches.isEmpty()) {
            return Response.status(HttpStatus.NOT_FOUND_404).entity("Could not find job").build();
        }

        final JobQueueBatch batch = batches.get(0);
        if (!batch.getOrgID().equals(orgUUID)) {
            return Response.status(HttpStatus.NOT_FOUND_404).entity("Could not find job").build();
        }

        final OneEcosystemJobStatus status = this.oneEcosystemClient.jobStatus(jobUUID);

        if (status.statusCode() == 202) {
            return Response.status(HttpStatus.ACCEPTED_202).build();
        }
        if (status.statusCode() != 200) {
            return Response.status(HttpStatus.NOT_FOUND_404).entity("Could not find job").build();
        }

        return Response.status(HttpStatus.OK_200)
                .entity(buildCompletionModel(status.body(), batch.getRequestUrl()))
                .build();
    }

    private JobCompletionModel buildCompletionModel(OneEcosystemJobStatusResponse response, String requestUrl) {
        final List<JobCompletionModel.OutputEntry> outputEntries = response.output().stream()
                .map(this::toOutputEntry)
                .collect(Collectors.toList());

        if (!response.error().isEmpty()) {
            logger.warn("Export job {} reported errors: {}", response.JobID(), response.error());
        }

        return new JobCompletionModel(
                OffsetDateTime.ofInstant(response.transactionTime(), ZoneOffset.UTC),
                requestUrl,
                outputEntries,
                List.of(),
                List.of());
    }

    private JobCompletionModel.OutputEntry toOutputEntry(OneEcosystemJobStatusOutput output) {
        final DPCResourceType type = resolveResourceType(output.type());
        return new JobCompletionModel.OutputEntry(type, output.url(), 0, List.of());
    }

    private DPCResourceType resolveResourceType(String resourceTypeName) {
        return Arrays.stream(DPCResourceType.values())
                .filter(type -> type.getPath().equalsIgnoreCase(resourceTypeName))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Unknown resource type returned by export backend: " + resourceTypeName));
    }
}
