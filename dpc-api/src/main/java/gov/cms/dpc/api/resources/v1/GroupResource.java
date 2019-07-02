package gov.cms.dpc.api.resources.v1;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import ca.uhn.fhir.context.FhirContext;
import gov.cms.dpc.api.resources.AbstractGroupResource;
import gov.cms.dpc.common.annotations.APIV1;
import gov.cms.dpc.common.interfaces.AttributionEngine;
import gov.cms.dpc.fhir.FHIRBuilders;
import gov.cms.dpc.queue.JobQueue;
import gov.cms.dpc.queue.models.JobModel;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.http.HttpStatus;
import org.hl7.fhir.dstu3.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static gov.cms.dpc.fhir.FHIRMediaTypes.FHIR_JSON;


public class GroupResource extends AbstractGroupResource {

    private static final Logger logger = LoggerFactory.getLogger(GroupResource.class);

    // The delimiter for the '_types' list query param.
    public static final String LIST_DELIM = ",";
    public static final String PREFER_RESPOND_ASYNC = "respond-async";
    public static final String PREFER_HEADER = "Prefer";

    private final JobQueue queue;
    private final AttributionEngine client;
    private final String baseURL;
    private final FhirContext fhirContext;

    @Inject
    public GroupResource(JobQueue queue, AttributionEngine client, @APIV1 String baseURL, FhirContext fhirContext) {
        this.queue = queue;
        this.client = client;
        this.baseURL = baseURL;
        this.fhirContext = fhirContext;
    }

    /**
     * Begin export process for the given provider
     * On success, returns a {@link org.eclipse.jetty.http.HttpStatus#NO_CONTENT_204} response with no content in the result.
     * The `Content-Location` header contains the URI to call when checking job status. On failure, return an {@link OperationOutcome}.
     *
     * @param providerID    {@link String} ID of provider to retrieve data for
     * @param resourceTypes - {@link String} of comma separated values corresponding to FHIR {@link ResourceType}
     * @return - {@link OperationOutcome} specifying whether or not the request was successful.
     */
    @Override
    @GET // Need this here, since we're using a path param
    @Path("/{providerID}/$export")
    @Timed
    @ExceptionMetered
    public Response export(@Context HttpHeaders headers,
                           @PathParam("providerID") String providerID,
                           @QueryParam("_type") String resourceTypes,
                           @QueryParam("_since") String since) {
        logger.debug("Exporting data for provider: {}", providerID);

        // Check the parameters
        final Optional<OperationOutcome> requestOutcome = checkExportRequest(headers.getRequestHeaders(), resourceTypes, since);
        if (requestOutcome.isPresent()) {
            return formErrorResponse(HttpStatus.BAD_REQUEST_400, requestOutcome.get());
        }

        // Get a list of attributed beneficiaries
        final Optional<List<String>> attributedBeneficiaries = this.client.getAttributedPatientIDs(FHIRBuilders.buildPractitionerFromNPI(providerID));
        if (attributedBeneficiaries.isEmpty()) {
            return formErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR_500, formExceptionOutcome("Attribution server connection"));
        }

        // Generate a job ID and submit it to the queue
        final UUID jobID = UUID.randomUUID();

        // Handle the _type query parameter
        final var resources = handleTypeQueryParam(resourceTypes);
        this.queue.submitJob(jobID, new JobModel(jobID, resources, providerID, attributedBeneficiaries.get()));

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
        for (String queryResource : resourcesListParam.split(LIST_DELIM, -1)) {
            final var foundResourceType = matchResourceType(queryResource);
            if (foundResourceType.isEmpty()) {
                throw new WebApplicationException(String.format("Unsupported resource name in the '_type' query parameter: %s", queryResource), Response.Status.BAD_REQUEST);
            }
            resources.add(foundResourceType.get());
        }
        return resources;
    }

    /**
     * Check the query parameters of the request. If valid, return empty. If not valid,
     * return an error response with an {@link OperationOutcome} in the body.
     * @param headers to check
     * @param resourceTypes list to check
     * @param since list to check (must be null)
     * @return a optional {@link Response} with a BAD_REQUEST status
     */
    public static Optional<OperationOutcome> checkExportRequest(MultivaluedMap<String, String> headers, String resourceTypes, String since) {
        var issues = new ArrayList<OperationOutcome.OperationOutcomeIssueComponent>();

        // Accept
        final List<String> accepts = headers.get(HttpHeaders.ACCEPT);
        if (accepts == null || accepts.stream().noneMatch(value -> value.startsWith(FHIR_JSON))) {
            issues.add(formNotSupportedIssue("'application/fhir+json' is the only supported response format"));
        }

        // Prefer
        final List<String> prefers = headers.get(PREFER_HEADER);
        if (prefers == null || prefers.size() != 1 || !PREFER_RESPOND_ASYNC.equalsIgnoreCase(prefers.get(0))) {
            issues.add(formNotSupportedIssue("One 'Prefer' header is required with a 'respond-async' value"));
        }

        // _since
        if (StringUtils.isNotEmpty(since)) {
            issues.add(formNotSupportedIssue("'_since' query parameter is not supported"));
        }

        // _type
        if (StringUtils.isNotEmpty(resourceTypes)) {
            for (String queryResource : resourceTypes.split(LIST_DELIM, -1)) {
                final var foundResourceType = matchResourceType(queryResource);
                if (foundResourceType.isEmpty()) {
                    issues.add(formNotSupportedIssue(String.format("Invalid '_type' parameter found: %s", queryResource)));
                }
            }
        }

        if (issues.size() == 0) {
            return Optional.empty();
        }

        final var outcome = new OperationOutcome().setIssue(issues);
        return Optional.of(outcome);
    }

    /**
     * Form a Response from a status code and an {@link OperationOutcome}.
     * @param status - HttpStatus code
     * @param outcome - Operational Outcome to use
     * @return
     */
    private Response formErrorResponse(int status, OperationOutcome outcome) {
        final var outcomeString = fhirContext.newJsonParser().encodeResourceToString(outcome);
        return Response.status(status).entity(outcomeString).type(FHIR_JSON).build();
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

    /**
     * Form an issue for an {@link OperationOutcome}
     * @param diagnostics to put into the issue
     * @return {@link OperationOutcome.OperationOutcomeIssueComponent}
     */
    private static OperationOutcome.OperationOutcomeIssueComponent formNotSupportedIssue(String diagnostics) {
        return new OperationOutcome.OperationOutcomeIssueComponent()
                .setSeverity(OperationOutcome.IssueSeverity.ERROR)
                .setCode(OperationOutcome.IssueType.NOTSUPPORTED)
                .setDiagnostics(diagnostics);
    }

    /**
     * Form a outcome for INTERNAL_SERVER_ERROR
     * @param diagnostics to add to the response
     * @return an {@link Response} with a INTERNAL_SERVER_ERROR.
     */
    private static OperationOutcome formExceptionOutcome(String diagnostics) {
        final var outcome = new OperationOutcome();
        outcome.addIssue()
                .setSeverity(OperationOutcome.IssueSeverity.FATAL)
                .setCode(OperationOutcome.IssueType.EXCEPTION)
                .setDiagnostics(diagnostics);
        return outcome;
    }
}
