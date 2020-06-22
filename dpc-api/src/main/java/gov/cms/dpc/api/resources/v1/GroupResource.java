package gov.cms.dpc.api.resources.v1;

import ca.uhn.fhir.model.primitive.DateTimeDt;
import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.name.Named;
import gov.cms.dpc.api.auth.OrganizationPrincipal;
import gov.cms.dpc.api.auth.annotations.PathAuthorizer;
import gov.cms.dpc.api.resources.AbstractGroupResource;
import gov.cms.dpc.bluebutton.client.BlueButtonClient;
import gov.cms.dpc.common.annotations.APIV1;
import gov.cms.dpc.common.annotations.NoHtml;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import gov.cms.dpc.fhir.FHIRExtractors;
import gov.cms.dpc.fhir.annotations.FHIR;
import gov.cms.dpc.fhir.annotations.FHIRAsync;
import gov.cms.dpc.fhir.annotations.Profiled;
import gov.cms.dpc.fhir.annotations.ProvenanceHeader;
import gov.cms.dpc.fhir.validations.profiles.AttestationProfile;
import gov.cms.dpc.queue.IJobQueue;
import gov.cms.dpc.queue.models.JobQueueBatch;
import io.dropwizard.auth.Auth;
import io.swagger.annotations.*;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

import static gov.cms.dpc.api.APIHelpers.addOrganizationTag;
import static gov.cms.dpc.fhir.FHIRMediaTypes.FHIR_NDJSON;
import static gov.cms.dpc.fhir.helpers.FHIRHelpers.handleMethodOutcome;


@Api(value = "Group", authorizations = @Authorization(value = "access_token"))
@Path("/v1/Group")
public class GroupResource extends AbstractGroupResource {

    private static final Logger logger = LoggerFactory.getLogger(GroupResource.class);
    static final String SYNTHETIC_BENE_ID = "-19990000000001";

    // The delimiter for the '_types' list query param.
    static final String LIST_DELIMITER = ",";

    private final IJobQueue queue;
    private final IGenericClient client;
    private final String baseURL;
    private final BlueButtonClient bfdClient;

    @Inject
    public GroupResource(IJobQueue queue, @Named("attribution") IGenericClient client, @APIV1 String baseURL, BlueButtonClient bfdClient) {
        this.queue = queue;
        this.client = client;
        this.baseURL = baseURL;
        this.bfdClient = bfdClient;
    }

    @POST
    @FHIR
    @Timed
    @ExceptionMetered
    @ApiOperation(value = "Create Attribution Group", notes = "FHIR endpoint to create an Attribution Group resource) associated to the provider listed in the in the Group characteristics.")
    @ApiImplicitParams(
            @ApiImplicitParam(name = "X-Provenance", required = true, paramType = "header", type="string",dataTypeClass = Provenance.class))
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Successfully created Roster"),
            @ApiResponse(code = 200, message = "Roster already exists")
    })
    @Override
    public Response createRoster(@ApiParam(hidden = true) @Auth OrganizationPrincipal organizationPrincipal,
                                 @ApiParam(hidden=true)  @Valid @Profiled(profile = AttestationProfile.PROFILE_URI) @ProvenanceHeader Provenance rosterAttestation,
                                 Group attributionRoster) {
        // Log attestation
        logAttestation(rosterAttestation, null, attributionRoster);
        addOrganizationTag(attributionRoster, organizationPrincipal.getOrganization().getId());

        final MethodOutcome outcome = this
                .client
                .create()
                .resource(attributionRoster)
                .encodedJson()
                .execute();

        return handleMethodOutcome(outcome);
    }

    @GET
    @FHIR
    @Timed
    @ExceptionMetered
    @ApiOperation(value = "Search for Attribution Groups", notes = "FHIR endpoint for searching for Attribution Groups." +
            "<p> If Provider NPI is given, all attribution groups for that provider will be returned. " +
            "If a Patient ID is given, all attribution groups for which that patient is a member will be returned.")
    @Override
    public Bundle rosterSearch(@ApiParam(hidden = true)
                               @Auth OrganizationPrincipal organizationPrincipal,
                               @ApiParam(value = "Provider NPI")
                               @QueryParam(value = Group.SP_CHARACTERISTIC_VALUE)
                               @NoHtml String providerNPI,
                               @ApiParam(value = "Patient ID")
                               @QueryParam(value = Group.SP_MEMBER)
                                   @NoHtml String patientID) {

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
    @PathAuthorizer(type = ResourceType.Group, pathParam = "rosterID")
    @FHIR
    @Timed
    @ExceptionMetered
    @ApiOperation(value = "Update Attribution Group", notes = "Update specific Attribution Group." +
            "<p>Updates allow for adding or removing patients as members of an Attribution Group.")
    @ApiImplicitParams(
            @ApiImplicitParam(name = "X-Provenance", value = "Provenance Resource attesting to Group attribution",  required = true, paramType = "header", type="string", dataTypeClass = Provenance.class)
     )


    @ApiResponses(@ApiResponse(code = 404, message = "Cannot find Roster with given ID"))
    @Override
    public Group updateRoster(@ApiParam(value = "Attribution Group ID") @PathParam("rosterID") UUID rosterID,
                              @ApiParam(hidden=true)  @Valid @Profiled(profile = AttestationProfile.PROFILE_URI) @ProvenanceHeader Provenance rosterAttestation,
                              Group rosterUpdate) {
        logAttestation(rosterAttestation, rosterID, rosterUpdate);
        final MethodOutcome outcome = this.client
                .update()
                .resource(rosterUpdate)
                .withId(new IdType("Group", rosterID.toString()))
                .encodedJson()
                .execute();

        return (Group) outcome.getResource();
    }

    @POST
    @Path("/{rosterID}/$add")
    @PathAuthorizer(type = ResourceType.Group, pathParam = "rosterID")
    @FHIR
    @Timed
    @ExceptionMetered
    @ApiOperation(value = "Add Group Members (Patients)", notes = "Update specific Attribution Group by adding Patient members given in the provided resource.")
    @ApiImplicitParams(
            @ApiImplicitParam(name = "X-Provenance", required = true, paramType = "header", type="string", dataTypeClass = Provenance.class))
    @ApiResponses(@ApiResponse(code = 404, message = "Cannot find Roster with given ID"))
    @Override
    public Group addRosterMembers(@ApiParam(value = "Attribution roster ID") @PathParam("rosterID") UUID rosterID,
                                  @ApiParam(hidden=true) @Valid @Profiled(profile = AttestationProfile.PROFILE_URI) @ProvenanceHeader Provenance rosterAttestation, @ApiParam Group groupUpdate) {
        logAttestation(rosterAttestation, rosterID, groupUpdate);
        return this.executeGroupOperation(rosterID, groupUpdate, "add");
    }

    @POST
    @Path("/{rosterID}/$remove")
    @PathAuthorizer(type = ResourceType.Group, pathParam = "rosterID")
    @FHIR
    @Timed
    @ExceptionMetered
    @ApiOperation(value = "Remove Group members (Patients)", notes = "Update specific Attribution Group by removing members (Patients) given in the provided resource.")
    @ApiResponses(@ApiResponse(code = 404, message = "Cannot find Roster with given ID"))
    @Override
    public Group removeRosterMembers(@ApiParam(value = "Attribution roster ID") @PathParam("rosterID") UUID rosterID,
                                     @ApiParam Group groupUpdate) {
        return this.executeGroupOperation(rosterID, groupUpdate, "remove");
    }

    @DELETE
    @FHIR
    @Path("/{rosterID}")
    @PathAuthorizer(type = ResourceType.Group, pathParam = "rosterID")
    @Timed
    @ExceptionMetered
    @ApiOperation(value = "Delete Attribution Group", notes = "Remove specific Attribution Group")
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
     * On success, returns a {@link org.eclipse.jetty.http.HttpStatus#ACCEPTED_202} response with no content in the result.
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
    @PathAuthorizer(type = ResourceType.Group, pathParam = "rosterID")
    @Timed
    @ExceptionMetered
    @FHIRAsync
    @ApiOperation(value = "Begin Group export request", tags = {"Group", "Bulk Data"},
            notes = "FHIR export operation which initiates a bulk data export for the given Provider")
    @ApiImplicitParams(
            @ApiImplicitParam(name = "Prefer", required = true, paramType = "header", type="string", value = "respond-async", dataTypeClass = String.class))
    @ApiResponses(
            @ApiResponse(code = 202, message = "Export request has started", responseHeaders = @ResponseHeader(name = "Content-Location", description = "URL to query job status", response = UUID.class))
    )
    public Response export(@ApiParam(hidden = true)
                           @Auth OrganizationPrincipal organizationPrincipal,
                           @ApiParam(value = "Provider ID", required = true)
                           @PathParam("rosterID") @NoHtml String rosterID,
                           @ApiParam(value = "List of FHIR resources to export", allowableValues = "ExplanationOfBenefits, Coverage, Patient")
                           @QueryParam("_type") @NoHtml String resourceTypes,
                           @ApiParam(value = "Output format of requested data", allowableValues = FHIR_NDJSON, defaultValue = FHIR_NDJSON)
                           @QueryParam("_outputFormat") @NoHtml String outputFormat,
                           @ApiParam(value = "Resources will be included in the response if their state has changed after the supplied time (e.g. if Resource.meta.lastUpdated is later than the supplied _since time).")
                           @QueryParam("_since") @NoHtml String since) {
        logger.debug("Exporting data for provider: {}", rosterID);

        // Check the parameters
        checkExportRequest(outputFormat);

        // Get the attributed patients
        final List<String> attributedPatients = fetchPatientMBIs(rosterID);

        // Generate a job ID and submit it to the queue
        final UUID orgID = FHIRExtractors.getEntityUUID(organizationPrincipal.getOrganization().getId());

        // Handle the _type query parameter
        final var resources = handleTypeQueryParam(resourceTypes);
        final var sinceDate = handleSinceQueryParam(since);
        final var transactionTime = fetchTransactionTime();
        final UUID jobID = this.queue.createJob(orgID, rosterID, attributedPatients, resources, sinceDate, transactionTime);

        return Response.status(Response.Status.ACCEPTED)
                .contentLocation(URI.create(this.baseURL + "/Jobs/" + jobID)).build();
    }

    private Group executeGroupOperation(UUID rosterID, Group groupUpdate, String operationName) {
        final Parameters parameters = new Parameters();
        parameters.addParameter().setResource(groupUpdate);
        return this.client
                .operation()
                .onInstance(new IdType("Group", rosterID.toString()))
                .named(operationName)
                .withParameters(parameters)
                .returnResourceType(Group.class)
                .encodedJson()
                .execute();
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
            return JobQueueBatch.validResourceTypes;
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
     * Convert the '_since' {@link QueryParam} to a Date
     *
     * @param since - {@link String} an instant
     * @return - A {@link OffsetDateTime} for this since.
     */
    private OffsetDateTime handleSinceQueryParam(String since) {
        if (StringUtils.isEmpty(since)) {
            return null;
        }
        // check that _since is a valid time
        try {
            var dt = new DateTimeDt();
            dt.setValueAsString(since);
            return dt.getValue().toInstant().atOffset(ZoneOffset.UTC);
        } catch (DataFormatException ex) {
            throw new BadRequestException("'_since' query parameter must be a valid date time value");
        }
    }

    /**
     * Fetch the BFD database last update time. Use it as the transactionTime for a job.
     * @return transactionTime from the BFD service
     */
    private OffsetDateTime fetchTransactionTime() {
        // Every bundle has transaction time after the Since RFC has beneficiary
        final Meta meta = bfdClient.requestPatientFromServer(SYNTHETIC_BENE_ID, null).getMeta();
        return Optional.ofNullable(meta.getLastUpdated())
                .map(u -> u.toInstant().atOffset(ZoneOffset.UTC))
                .orElse(OffsetDateTime.now(ZoneOffset.UTC));
    }

    /**
     * Check the query parameters of the request. If valid, return empty. If not valid,
     * return an error response with an {@link OperationOutcome} in the body.
     *
     * @param outputFormat param to check
     */
    private static void checkExportRequest(String outputFormat) {
        // _outputFormat only supports FHIR_NDJSON
        if (StringUtils.isNotEmpty(outputFormat) && !FHIR_NDJSON.equals(outputFormat)) {
            throw new BadRequestException("'_outputFormat' query parameter must be 'application/fhir+ndjson'");
        }
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

        final Parameters parameters = new Parameters();
        parameters.addParameter().setValue(new BooleanType(true)).setName("active");

        // Get the patients, along with their MBIs
        final Bundle patients = this.client
                .operation()
                .onInstance(new IdType(attributionRoster.getId()))
                .named("patients")
                .withParameters(parameters)
                .returnResourceType(Bundle.class)
                .useHttpGet()
                .encodedJson()
                .execute();

        return patients
                .getEntry()
                .stream()
                .map(entry -> (Patient) entry.getResource())
                .map(FHIRExtractors::getPatientMBI)
                .collect(Collectors.toList());
    }

    /**
     * Log the attribution attestation, as required by Office of Civil Rights
     * Eventually, this will need to get persisted into durable storage, but for now, Splunk is fine.
     * <p>
     * We require the roster ID to be passed in from the query itself, rather than extracted from the {@link Group}, because it's possible for the {@link Group} ID to be empty, if the user doesn't manually set it before uploading.
     *
     * @param provenance        - {@link Provenance} attestation to log
     * @param rosterID          - {@link UUID} of roster being updated
     * @param attributionRoster - {@link Group} roster being attested
     */
    private void logAttestation(Provenance provenance, UUID rosterID, Group attributionRoster) {

        final String groupIDLog;
        if (rosterID == null) {
            groupIDLog = "";
        } else {
            groupIDLog = String.format(" for roster %s", new IdType("Group", rosterID.toString()));
        }

        final Coding reason = provenance.getReasonFirstRep();

        final Provenance.ProvenanceAgentComponent performer = FHIRExtractors.getProvenancePerformer(provenance);
        final List<String> attributedPatients = attributionRoster
                .getMember()
                .stream()
                .map(Group.GroupMemberComponent::getEntity)
                .map(Reference::getReference)
                .collect(Collectors.toList());

        logger.info("Organization {} is attesting a {} purpose between provider {} and patient(s) {}{}", performer.getWhoReference().getReference(),
                reason.getCode(),
                performer.getOnBehalfOfReference().getReference(), attributedPatients, groupIDLog);
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
        return JobQueueBatch.validResourceTypes.stream()
                .filter(validResource -> validResource.toString().equalsIgnoreCase(canonical))
                .findFirst();
    }
}
