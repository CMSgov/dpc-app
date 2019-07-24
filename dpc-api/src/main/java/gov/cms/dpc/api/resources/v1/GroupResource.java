package gov.cms.dpc.api.resources.v1;

import gov.cms.dpc.api.auth.OrganizationPrincipal;
import gov.cms.dpc.api.auth.annotations.*;
import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import gov.cms.dpc.api.resources.AbstractGroupResource;
import gov.cms.dpc.common.annotations.APIV1;
import gov.cms.dpc.common.interfaces.AttributionEngine;
import gov.cms.dpc.fhir.FHIRBuilders;
import gov.cms.dpc.fhir.FHIRExtractors;
import gov.cms.dpc.fhir.annotations.FHIRAsync;
import gov.cms.dpc.queue.JobQueue;
import gov.cms.dpc.queue.models.JobModel;
import io.dropwizard.auth.Auth;
import io.swagger.annotations.*;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static gov.cms.dpc.fhir.FHIRMediaTypes.FHIR_NDJSON;


@Api(value = "Group")
public class GroupResource extends AbstractGroupResource {

    private static final Logger logger = LoggerFactory.getLogger(GroupResource.class);

    // The delimiter for the '_types' list query param.
    static final String LIST_DELIMITER = ",";

    private final JobQueue queue;
    private final AttributionEngine client;
    private final String baseURL;

    @Inject
    public GroupResource(JobQueue queue, AttributionEngine client, @APIV1 String baseURL) {
        this.queue = queue;
        this.client = client;
        this.baseURL = baseURL;
    }

    /**
     * Begin export process for the given provider
     * On success, returns a {@link org.eclipse.jetty.http.HttpStatus#NO_CONTENT_204} response with no content in the result.
     * The `Content-Location` header contains the URI to call when checking job status. On failure, return an {@link OperationOutcome}.
     *
     * @param providerID    {@link String} ID of provider to retrieve data for
     * @param resourceTypes - {@link String} of comma separated values corresponding to FHIR {@link ResourceType}
     * @param outputFormat  - Optional outputFormats parameter
     * @param since         - Optional since parameter
     * @return - {@link OperationOutcome} specifying whether or not the request was successful.
     */
    @Override
    @GET // Need this here, since we're using a path param
    @Path("/{providerID}/$export")
    @Timed
    @ExceptionMetered
    @FHIRAsync
    @ApiOperation(value = "Begin export request", tags = {"Group", "Bulk Data"},
            notes = "FHIR export operation which initiates a bulk data export for the given Provider")
    @ApiImplicitParams(
            @ApiImplicitParam(name = "Prefer", required = true, paramType = "header", value = "respond-async"))
    @ApiResponses(
            @ApiResponse(code = 204, message = "Export request has started", responseHeaders = @ResponseHeader(name = "Content-Location", description = "URL to query job status", response = UUID.class))
    )
    public Response export(@ApiParam(hidden = true)
                           @Auth OrganizationPrincipal organizationPrincipal,
                           @ApiParam(value = "Provider NPI", required = true)
                           @PathParam("providerID") String providerID,
                           @ApiParam(value = "List of FHIR resources to export", allowableValues = "ExplanationOfBenefits, Coverage, Patient")
                           @QueryParam("_type") String resourceTypes,
                           @ApiParam(value = "Output format of requested data", allowableValues = FHIR_NDJSON, defaultValue = FHIR_NDJSON)
                           @QueryParam("_outputFormat") String outputFormat,
                           @ApiParam(value = "Request data that has been updated after the given point. (Not implemented yet)", hidden = true)
                           @QueryParam("_since") String since) {
        logger.debug("Exporting data for provider: {}", providerID);

        // Check the parameters
        checkExportRequest(outputFormat, since);

        // Get a list of attributed beneficiaries
        final Optional<List<String>> attributedBeneficiaries = this.client.getAttributedPatientIDs(FHIRBuilders.buildPractitionerFromNPI(providerID));
        if (attributedBeneficiaries.isEmpty()) {
            throw new WebApplicationException("Attribution server error");
        }

        // Generate a job ID and submit it to the queue
        final UUID jobID = UUID.randomUUID();
        final UUID orgID = FHIRExtractors.getEntityUUID(organizationPrincipal.getOrganization().getId());

        // Handle the _type query parameter
        final var resources = handleTypeQueryParam(resourceTypes);

        this.queue.submitJob(jobID, new JobModel(jobID, orgID, resources, providerID, attributedBeneficiaries.get()));

        return Response.status(Response.Status.NO_CONTENT)
                .contentLocation(URI.create(this.baseURL + "/Jobs/" + jobID)).build();
    }

    /**
     * Test method for verifying FHIR deserialization, it will eventually be removed.
     * TODO(nickrobison): Remove this
     *
     * @param group - {@link Group} to use for testing
     * @return - {@link String} test string
     */
    @POST
    @Public
    @ApiOperation(value = "FHIR marshall test", hidden = true)
    public Patient marshalTest(Group group) {

        if (group.getIdentifierFirstRep().getValue().equals("Group/fail")) {
            throw new IllegalStateException("Should fail");
        }

        final HumanName name = new HumanName().setFamily("Doe").addGiven("John");
        return new Patient().addName(name).addIdentifier(new Identifier().setValue("test-id"));
    }

    /**
     * Convert the '_types' {@link QueryParam} to a list of resources to add to the job. Handle the empty case,
     * by returning all valid resource types.
     *
     * @param resourcesListParam - {@link String} of comma separated values corresponding to FHIR {@link ResourceType}s
     * @return - A list of {@link ResourceType} to return for this request.
     */
    private List<ResourceType> handleTypeQueryParam(String resourcesListParam) {
        // If the query param is omitted, the FHIR spec states that all resources should be returned
        if (resourcesListParam == null || resourcesListParam.isEmpty()) {
            return JobModel.validResourceTypes;
        }

        final var resources = new ArrayList<ResourceType>();
        for (String queryResource : resourcesListParam.split(LIST_DELIMITER, -1)) {
            final var foundResourceType = matchResourceType(queryResource);
            if (foundResourceType.isEmpty()) {
                throw new BadRequestException(String.format("Unsupported resource name in the '_type' query parameter: %s", queryResource));
            }
            resources.add(foundResourceType.get());
        }
        return resources;
    }

    /**
     * Check the query parameters of the request. If valid, return empty. If not valid,
     * return an error response with an {@link OperationOutcome} in the body.
     *
     * @param outputFormat param to check
     * @param since        param to check
     */
    private static void checkExportRequest(String outputFormat, String since) {
        // _since is unsupported
        if (StringUtils.isNotEmpty(since)) {
            throw new BadRequestException("'_since' is not supported");
        }

        // _outputFormat only supports FHIR_NDJSON
        if (StringUtils.isNotEmpty(outputFormat) && !FHIR_NDJSON.equals(outputFormat)) {
            throw new BadRequestException("'_outputFormat' query parameter must be 'application/fhir+ndjson'");
        }
    }

    /**
     * Convert a single resource type in a query param into a {@link ResourceType}.
     *
     * @param queryResourceType - The text from the query param
     * @return If match is found a {@link ResourceType}
     */
    private static Optional<ResourceType> matchResourceType(String queryResourceType) {
        final var canonical = queryResourceType.trim().toUpperCase();
        // Implementation Note: resourceTypeMap is a small list <3 so hashing isn't faster
        return JobModel.validResourceTypes.stream()
                .filter(validResource -> validResource.toString().equalsIgnoreCase(canonical))
                .findFirst();
    }
}
