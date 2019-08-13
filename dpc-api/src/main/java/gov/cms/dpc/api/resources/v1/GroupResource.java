package gov.cms.dpc.api.resources.v1;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import gov.cms.dpc.api.auth.OrganizationPrincipal;
import gov.cms.dpc.api.auth.annotations.PathAuthorizer;
import gov.cms.dpc.api.resources.AbstractGroupResource;
import gov.cms.dpc.common.annotations.APIV1;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import gov.cms.dpc.fhir.FHIRExtractors;
import gov.cms.dpc.fhir.annotations.FHIR;
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
import java.util.*;
import java.util.stream.Collectors;

import static gov.cms.dpc.api.APIHelpers.addOrganizationTag;
import static gov.cms.dpc.fhir.FHIRMediaTypes.FHIR_NDJSON;
import static gov.cms.dpc.fhir.helpers.FHIRHelpers.handleMethodOutcome;


@Api(value = "Group")
public class GroupResource extends AbstractGroupResource {

    private static final Logger logger = LoggerFactory.getLogger(GroupResource.class);

    // The delimiter for the '_types' list query param.
    static final String LIST_DELIMITER = ",";

    private final JobQueue queue;
    private final IGenericClient client;
    private final String baseURL;

    @Inject
    public GroupResource(JobQueue queue, IGenericClient client, @APIV1 String baseURL) {
        this.queue = queue;
        this.client = client;
        this.baseURL = baseURL;
    }

    @POST
    @FHIR
    @Timed
    @ExceptionMetered
    @ApiOperation(value = "Create Attribution Roster", notes = "FHIR endpoint to create an Attribution roster (Group resource) associated to the provider listed in the in the Group characteristics.")
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Successfully created Roster"),
            @ApiResponse(code = 200, message = "Roster already exists")
    })
    @Override
    public Response createRoster(@ApiParam(hidden = true) @Auth OrganizationPrincipal organizationPrincipal, Group attributionRoster) {
        addOrganizationTag(attributionRoster, organizationPrincipal.getOrganization().getId());

        final MethodOutcome outcome = this
                .client
                .create()
                .resource(attributionRoster)
                .encodedJson()
                .execute();

        return handleMethodOutcome(outcome);
    }

    @SuppressWarnings("unchecked")
    @GET
    @FHIR
    @Timed
    @ExceptionMetered
    @ApiOperation(value = "Search for Attribution Rosters", notes = "FHIR endpoint for searching for Attribution rosters." +
            "<p> If Provider NPI is given, all attribution groups for that provider will be returned. " +
            "If a Patient ID is given, all attribution groups for which that patient is a member will be returned.")
    @Override
    public Bundle rosterSearch(@ApiParam(hidden = true)
                               @Auth OrganizationPrincipal organizationPrincipal,
                               @ApiParam(value = "Provider NPI")
                               @QueryParam(value = Group.SP_CHARACTERISTIC_VALUE)
                                       String providerNPI,
                               @ApiParam(value = "Patient ID")
                               @QueryParam(value = Group.SP_MEMBER)
                                       String patientID) {

        final Map<String, List<String>> queryParams = new HashMap<>();

        if (providerNPI != null) {
            queryParams.put(Group.SP_CHARACTERISTIC_VALUE, Collections.singletonList(providerNPI));
        }

        if (patientID != null) {
            queryParams.put("member", Collections.singletonList(patientID));
        }

        return this.client
                .search()
                .forResource(Group.class)
                .whereMap(queryParams)
                .withTag(DPCIdentifierSystem.DPC.getSystem(), organizationPrincipal.getOrganization().getIdElement().getIdPart())
                .returnBundle(Bundle.class)
                .encodedJson()
                .execute();
    }

    @GET
    @FHIR
    @Path("/{rosterID}")
    @PathAuthorizer(type = ResourceType.Group, pathParam = "rosterID")
    @Timed
    @ExceptionMetered
    @ApiOperation(value = "Fetch Attribution Roster", notes = "Fetch specific Attribution roster.")
    @ApiResponses(@ApiResponse(code = 404, message = "Cannot find Roster with given ID"))
    @Override
    public Group getRoster(@ApiParam(value = "Attribution roster ID") @PathParam("rosterID") UUID rosterID) {
        return this.client
                .read()
                .resource(Group.class)
                .withId(new IdType("Group", rosterID.toString()))
                .encodedJson()
                .execute();
    }

    @PUT
    @Path("/{rosterID}")
    @PathAuthorizer(type = ResourceType.Patient, pathParam = "rosterID")
    @FHIR
    @Timed
    @ExceptionMetered
    @ApiOperation(value = "Update Attribution Roster", notes = "Update specific Attribution roster." +
            "<p>Updates allow for adding or removing patients from the roster.")
    @ApiResponses(@ApiResponse(code = 404, message = "Cannot find Roster with given ID"))
    @Override
    public Group updateRoster(@ApiParam(value = "Attribution roster ID") @PathParam("rosterID") UUID rosterID, Group rosterUpdate) {
        final MethodOutcome outcome = this.client
                .update()
                .resource(rosterUpdate)
                .withId(new IdType("Group", rosterID.toString()))
                .encodedJson()
                .execute();

        return (Group) outcome.getResource();
    }

    @DELETE
    @FHIR
    @Path("/{rosterID}")
    @PathAuthorizer(type = ResourceType.Patient, pathParam = "rosterID")
    @Timed
    @ExceptionMetered
    @ApiOperation(value = "Delete Attribution Roster", notes = "Remove specific Attribution roster")
    @ApiResponses(@ApiResponse(code = 404, message = "Cannot find Roster with given ID"))
    @Override
    public Response deleteRoster(@ApiParam(value = "Attribution roster ID") @PathParam("rosterID") UUID rosterID) {
        this.client
                .delete()
                .resourceById(new IdType("Group", rosterID.toString()))
                .encodedJson()
                .execute();

        return Response.ok().build();
    }

    /**
     * Begin export process for the given provider
     * On success, returns a {@link org.eclipse.jetty.http.HttpStatus#NO_CONTENT_204} response with no content in the result.
     * The `Content-Location` header contains the URI to call when checking job status. On failure, return an {@link OperationOutcome}.
     *
     * @param rosterID      {@link String} ID of provider to retrieve data for
     * @param resourceTypes - {@link String} of comma separated values corresponding to FHIR {@link ResourceType}
     * @param outputFormat  - Optional outputFormats parameter
     * @param since         - Optional since parameter
     * @return - {@link OperationOutcome} specifying whether or not the request was successful.
     */
    @Override
    @GET // Need this here, since we're using a path param
    @Path("/{rosterID}/$export")
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
                           @ApiParam(value = "Provider ID", required = true)
                           @PathParam("rosterID") String rosterID,
                           @ApiParam(value = "List of FHIR resources to export", allowableValues = "ExplanationOfBenefits, Coverage, Patient")
                           @QueryParam("_type") String resourceTypes,
                           @ApiParam(value = "Output format of requested data", allowableValues = FHIR_NDJSON, defaultValue = FHIR_NDJSON)
                           @QueryParam("_outputFormat") String outputFormat,
                           @ApiParam(value = "Request data that has been updated after the given point. (Not implemented yet)", hidden = true)
                           @QueryParam("_since") String since) {
        logger.debug("Exporting data for provider: {}", rosterID);

        // Check the parameters
        checkExportRequest(outputFormat, since);

        // Get the attributed patients
        final List<String> attributedPatients = fetchPatientMBIs(rosterID);

        // Generate a job ID and submit it to the queue
        final UUID jobID = UUID.randomUUID();
        final UUID orgID = FHIRExtractors.getEntityUUID(organizationPrincipal.getOrganization().getId());

        // Handle the _type query parameter
        final var resources = handleTypeQueryParam(resourceTypes);
        this.queue.submitJob(jobID, new JobModel(jobID, orgID, resources, rosterID, attributedPatients));

        return Response.status(Response.Status.NO_CONTENT)
                .contentLocation(URI.create(this.baseURL + "/Jobs/" + jobID)).build();
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

    private List<String> fetchPatientMBIs(String groupID) {

        final Group attributionRoster = this.client
                .read()
                .resource(Group.class)
                .withId(new IdType("Group", groupID))
                .encodedJson()
                .execute();

        if (attributionRoster.getMember().isEmpty()) {
            throw new WebApplicationException("Cannot perform export with no beneficiaries", Response.Status.NOT_ACCEPTABLE);
        }

        // Get the patients, along with their MBIs
        final Bundle patients = this.client
                .operation()
                .onInstance(new IdType(attributionRoster.getId()))
                .named("patients")
                .withNoParameters(Parameters.class)
                .returnResourceType(Bundle.class)
                .useHttpGet()
                .encodedJson()
                .execute();

        return patients
                .getEntry()
                .stream()
                .map(entry -> (Patient) entry.getResource())
                .map(FHIRExtractors::getPatientMPI)
                .collect(Collectors.toList());
    }
}
