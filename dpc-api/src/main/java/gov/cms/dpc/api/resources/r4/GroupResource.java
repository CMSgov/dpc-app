package gov.cms.dpc.api.resources.r4;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import gov.cms.dpc.api.APIHelpers;
import gov.cms.dpc.api.DPCAPIConfiguration;
import gov.cms.dpc.api.auth.OrganizationPrincipal;
import gov.cms.dpc.api.auth.annotations.Authorizer;
import gov.cms.dpc.api.auth.annotations.PathAuthorizer;
import gov.cms.dpc.common.annotations.APIV3;
import gov.cms.dpc.common.annotations.NoHtml;
import gov.cms.dpc.common.logging.SplunkTimestamp;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import gov.cms.dpc.fhir.DPCResourceType;
import gov.cms.dpc.fhir.FHIRHeaders;
import gov.cms.dpc.fhir.annotations.FHIR;
import gov.cms.dpc.fhir.annotations.FHIRAsync;
import gov.cms.dpc.fhir.annotations.ProvenanceHeader;
import gov.cms.dpc.fhir.r4.FHIRExtractors;
import gov.cms.dpc.oneecosystem.client.OneEcosystemClient;
import gov.cms.dpc.oneecosystem.models.OneEcosystemExportRequest;
import gov.cms.dpc.oneecosystem.models.OneEcosystemExportResponse;
import gov.cms.dpc.queue.models.ExternalJobQueueBatch;
import gov.cms.dpc.queue.service.DataService;
import io.dropwizard.auth.Auth;
import io.swagger.annotations.*;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import org.apache.commons.collections.CollectionUtils;
import org.apache.hc.core5.http.HttpStatus;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static gov.cms.dpc.common.logging.LogSanitizer.sanitize;
import static gov.cms.dpc.fhir.FHIRMediaTypes.*;

@Api(value = "Group", authorizations = @Authorization(value = "access_token"))
@Path("/v3/Group")
public class GroupResource extends AbstractGroupResource {

    private static final Logger logger = LoggerFactory.getLogger(GroupResource.class);
    private static final String RESOURCE_TYPE = "Group";

    private final IGenericClient stu3Client;
    private final String baseURL;
    private final OneEcosystemClient oneEcosystemClient;
    private final DataService dataService;
    private final DPCAPIConfiguration config;

    @Inject
    public GroupResource(@Named("attribution-r4") IGenericClient client,
                         @Named("attribution") IGenericClient stu3Client, // Can be removed when Practitioner is on R4
                         @APIV3 String baseURL,
                         OneEcosystemClient oneEcosystemClient,
                         DataService dataService,
                         DPCAPIConfiguration config) {
        super(client);
        this.stu3Client = stu3Client;
        this.baseURL = baseURL;
        this.oneEcosystemClient = oneEcosystemClient;
        this.dataService = dataService;
        this.config = config;
    }

    @POST
    @FHIR
    @Timed
    @ExceptionMetered
    @Authorizer
    @ApiOperation(value = "Create Attribution Group", notes = "FHIR endpoint to create an Attribution Group resource associated to the provider listed in the Group characteristics.")
    @ApiImplicitParams(
            @ApiImplicitParam(name = "X-Provenance", required = true, paramType = "header", type = "string", dataTypeClass = Provenance.class))
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Successfully created Roster"),
            @ApiResponse(code = 200, message = "Roster already exists"),
            @ApiResponse(code = 422, message = "Provider in Provenance header does not match Provider in Roster")
    })
    @Override
    public Response createRoster(@ApiParam(hidden = true) @Auth OrganizationPrincipal organizationPrincipal,
                                 @ApiParam(hidden = true) @Valid @ProvenanceHeader Provenance rosterAttestation, // readd @Profiled when supported for V3
                                 Group attributionRoster) {
        // Log attestation
        logAndVerifyAttestation(rosterAttestation, null, attributionRoster);
        APIHelpers.addOrganizationTag(attributionRoster, organizationPrincipal.getOrganization().getId());

        final MethodOutcome outcome = this.client
                .create()
                .resource(attributionRoster)
                .encodedJson()
                .execute();

        return buildCreateResponse(outcome);
    }

    @GET
    @FHIR
    @Path("/{rosterID}")
    @PathAuthorizer(type = DPCResourceType.Group, pathParam = "rosterID")
    @Timed
    @ExceptionMetered
    @ApiOperation(value = "Fetch Attribution Roster", notes = "Fetch specific Attribution roster.")
    @ApiResponses(@ApiResponse(code = 404, message = "Cannot find Roster with given ID"))
    @Override
    public Group getRoster(@ApiParam(value = "Attribution roster ID") @PathParam("rosterID") UUID rosterID) {
        requireRosterId(rosterID);
        return fetchGroup(new IdType(RESOURCE_TYPE, rosterID.toString()));
    }

    @PUT
    @Path("/{rosterID}")
    @PathAuthorizer(type = DPCResourceType.Group, pathParam = "rosterID")
    @FHIR
    @Timed
    @ExceptionMetered
    @ApiOperation(value = "Update Attribution Group", notes = "Update specific Attribution Group." +
            "<p>Updates allow for adding or removing patients as members of an Attribution Group.")
    @ApiImplicitParams(
            @ApiImplicitParam(name = "X-Provenance", value = "Provenance Resource attesting to Group attribution", required = true, paramType = "header", type = "string", dataTypeClass = Provenance.class))
    @ApiResponses({
            @ApiResponse(code = 404, message = "Cannot find Roster with given ID"),
            @ApiResponse(code = 422, message = "Provider in Provenance header does not match Provider in Roster")
    })
    @Override
    public Group updateRoster(@ApiParam(hidden = true) @Auth OrganizationPrincipal organizationPrincipal,
                              @ApiParam(value = "Attribution Group ID") @PathParam("rosterID") UUID rosterID,
                              @ApiParam(hidden = true) @Valid @ProvenanceHeader Provenance rosterAttestation, // readd @Profiled when supported for V3
                              Group rosterUpdate) {
        logAndVerifyAttestation(rosterAttestation, rosterID, rosterUpdate);
        APIHelpers.addOrganizationTag(rosterUpdate, organizationPrincipal.getOrganization().getId());

        requireRosterId(rosterID);
        final MethodOutcome outcome = this.client
                .update()
                .resource(rosterUpdate)
                .withId(new IdType(RESOURCE_TYPE, rosterID.toString()))
                .encodedJson()
                .execute();

        return (Group) outcome.getResource();
    }

    @POST
    @Path("/{rosterID}/$member-add")
    @PathAuthorizer(type = DPCResourceType.Group, pathParam = "rosterID")
    @FHIR
    @Timed
    @ExceptionMetered
    @ApiOperation(value = "Add Group Members (Patients)", notes = "Update specific Attribution Group by adding Patient members given in the provided resource.")
    @ApiImplicitParams(
            @ApiImplicitParam(name = "X-Provenance", required = true, paramType = "header", type = "string", dataTypeClass = Provenance.class))
    @ApiResponses(@ApiResponse(code = 404, message = "Cannot find Roster with given ID"))
    @Override
    public Group addRosterMembers(@ApiParam(hidden = true) @Auth OrganizationPrincipal organizationPrincipal,
                                  @ApiParam(value = "Attribution roster ID") @PathParam("rosterID") UUID rosterID,
                                  @ApiParam(hidden = true) @Valid @ProvenanceHeader Provenance rosterAttestation, // readd @Profiled when supported for V3
                                  Group groupUpdate) {
        logAndVerifyAttestation(rosterAttestation, rosterID, groupUpdate);
        APIHelpers.addOrganizationTag(groupUpdate, organizationPrincipal.getOrganization().getId());
        return executeGroupOperation(rosterID, groupUpdate, "add");
    }

    @POST
    @Path("/{rosterID}/$member-remove")
    @PathAuthorizer(type = DPCResourceType.Group, pathParam = "rosterID")
    @FHIR
    @Timed
    @ExceptionMetered
    @ApiOperation(value = "Remove Group members (Patients)", notes = "Update specific Attribution Group by removing members (Patients) given in the provided resource.")
    @ApiResponses(@ApiResponse(code = 404, message = "Cannot find Roster with given ID"))
    @Override
    public Group removeRosterMembers(@ApiParam(value = "Attribution roster ID") @PathParam("rosterID") UUID rosterID,
                                     @ApiParam Group groupUpdate) {
        return executeGroupOperation(rosterID, groupUpdate, "remove");
    }

    /**
     * Begin export process for the given roster.
     * On success, returns a {@link org.eclipse.jetty.http.HttpStatus#ACCEPTED_202} response with no content in the result.
     * The `Content-Location` header contains the URI to call when checking job status.
     *
     * @param rosterID      {@link String} ID of the Group to retrieve data for
     * @param resourceTypes - {@link String} of comma separated values corresponding to FHIR {@link DPCResourceType}
     * @param outputFormat  - Optional outputFormats parameter
     * @param sinceParam    - Optional since parameter
     * @return - {@link Response} specifying whether the request was successful.
     */
    @Override
    @GET
    @Path("/{rosterID}/$export")
    @PathAuthorizer(type = DPCResourceType.Group, pathParam = "rosterID")
    @Timed
    @ExceptionMetered
    @FHIRAsync
    @ApiOperation(value = "Begin Group export request", tags = {"Group", "Bulk Data"},
            notes = "FHIR export operation which initiates a bulk data export for the given Group, " +
                    "routed through the shared export backend")
    @ApiImplicitParams(
            @ApiImplicitParam(name = "Prefer", required = true, paramType = "header", type = "string", value = "respond-async", dataTypeClass = String.class))
    @ApiResponses(
            @ApiResponse(code = 202, message = "Export request has started", responseHeaders = @ResponseHeader(name = "Content-Location", description = "URL to query job status", response = UUID.class)))
    public Response export(@ApiParam(hidden = true) @Auth OrganizationPrincipal organizationPrincipal,
                           @ApiParam(value = "Roster ID", required = true)
                           @PathParam("rosterID") @NoHtml String rosterID,
                           @ApiParam(value = "List of FHIR resources to export", allowableValues = "ExplanationOfBenefits, Coverage, Patient")
                           @QueryParam("_type") @NoHtml String resourceTypes,
                           @ApiParam(value = "Output format of requested data", allowableValues = FHIR_NDJSON + "," + APPLICATION_NDJSON + "," + NDJSON, defaultValue = FHIR_NDJSON)
                           @DefaultValue(FHIR_NDJSON) @QueryParam("_outputFormat") @NoHtml String outputFormat,
                           @ApiParam(value = "Resources will be included in the response if their state has changed after the supplied time.")
                           @QueryParam("_since") @NoHtml String sinceParam,
                           @QueryParam("_typeFilter") @NoHtml String typeFilterParam,
                           @ApiParam(hidden = true) @HeaderParam(FHIRHeaders.PREFER_HEADER) @Valid String prefer,
                           @Context HttpServletRequest request) {
        rosterID = sanitize(rosterID);
        sinceParam = sanitize(sinceParam);
        logger.info("Exporting data for roster: {} _since: {}", rosterID, sinceParam);
        final String eventTime = SplunkTimestamp.getSplunkTimestamp();

        // Check the parameters
        checkExportRequest(outputFormat, prefer);

        final Group group = fetchGroup(new IdType(RESOURCE_TYPE, rosterID));

        // Get the attributed patients
        final List<String> attributedMbis = fetchPatientMBIs(group);
        if (CollectionUtils.isEmpty(attributedMbis)) {
            throw new WebApplicationException("No active attributed patients found for the group", Response.Status.NOT_ACCEPTABLE);
        }

        final UUID orgID = gov.cms.dpc.fhir.stu3.FHIRExtractors.getEntityUUID(organizationPrincipal.getOrganization().getId());
        final UUID groupID = UUID.fromString(rosterID);
        final String orgNPI = this.getOrganizationNPI(organizationPrincipal);
        final String providerNPI = FHIRExtractors.getAttributedNPI(group);

        // Handle the _type and since query parameters
        final List<DPCResourceType> resources = this.handleTypeQueryParam(resourceTypes);
        final List<String> resourceNames = resources.stream().map(DPCResourceType::name).toList();
        final OffsetDateTime since = this.handleSinceQueryParam(sinceParam);
        final Instant sinceInstant = since == null ? Instant.EPOCH : since.toInstant();
        final String typeFilter = typeFilterParam == null ? "" : typeFilterParam;

        final String requestingIP = APIHelpers.fetchRequestingIP(request);
        final String requestUrl = APIHelpers.fetchRequestUrl(request);

        // Send export request to shared backend
        final OneEcosystemExportRequest exportRequest =
                new OneEcosystemExportRequest(attributedMbis, sinceInstant, resourceNames, typeFilter);
        final OneEcosystemExportResponse exportResponse = this.oneEcosystemClient.export(exportRequest);
        if (exportResponse.statusCode() != 202) {
            throw new WebApplicationException("Unable to start export job", HttpStatus.SC_BAD_GATEWAY);
        }
        final UUID backendJobID = extractJobId(exportResponse.contentLocation());

        final boolean isSmoke = config.getLookBackExemptOrgs().contains(orgID.toString());
        final ExternalJobQueueBatch externalBatch = new ExternalJobQueueBatch.Builder()
                .jobID(backendJobID)
                .orgID(orgID)
                .orgNPI(orgNPI)
                .providerNPI(providerNPI)
                .mbis(attributedMbis)
                .resourceTypes(resources)
                .since(since)
                .requestingIP(requestingIP)
                .requestUrl(requestUrl)
                .groupID(groupID)
                .isBulk(true)
                .isSmoke(isSmoke)
                .build();
        this.dataService.createExternalJob(externalBatch);

        final int totalPatients = attributedMbis.size();
        final String resourcesRequested = String.join(";", resourceNames);
        logger.info("dpcMetric=queueSubmitted,operation=/v3/Group/$export,jobID={},orgId={},orgNpi={},totalPatients={},resourcesRequested={},queueSubmitTime={}",
                backendJobID, orgID, orgNPI, totalPatients, resourcesRequested, eventTime);

        return Response.status(Response.Status.ACCEPTED)
                .contentLocation(URI.create(this.baseURL + "/Jobs/" + backendJobID)).build();
    }

    private Group executeGroupOperation(UUID rosterID, Group groupUpdate, String operationName) {
        requireRosterId(rosterID);
        final Parameters parameters = new Parameters();
        parameters.addParameter().setResource(groupUpdate);
        return this.client
                .operation()
                .onInstance(new IdType(RESOURCE_TYPE, rosterID.toString()))
                .named(operationName)
                .withParameters(parameters)
                .returnResourceType(Group.class)
                .encodedJson()
                .execute();
    }

    private List<String> fetchPatientMBIs(Group group) {
        if (group.getMember().isEmpty()) {
            throw new WebApplicationException("Cannot perform export with no beneficiaries", Response.Status.NOT_ACCEPTABLE);
        }

        final Parameters parameters = new Parameters();
        parameters.addParameter().setValue(new BooleanType(true)).setName("active");

        final Bundle patients = this.client
                .operation()
                .onInstance(new IdType(group.getId()))
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
                .toList();
    }

    private Group fetchGroup(IdType groupID) {
        return this.client
                .read()
                .resource(Group.class)
                .withId(groupID)
                .encodedJson()
                .execute();
    }

    private UUID extractJobId(String contentLocation) {
        final String[] segments = contentLocation.split("/");
        return UUID.fromString(segments[segments.length - 1]);
    }

    /**
     * Log the attribution attestation, as required by Office of Civil Rights.
     * <p>
     * We require the roster ID to be passed in from the query itself, rather than extracted from the {@link Group},
     * because it's possible for the {@link Group} ID to be empty if the user doesn't manually set it before uploading.
     *
     * @param provenance        - {@link Provenance} attestation to log
     * @param rosterID          - {@link UUID} of roster being updated
     * @param attributionRoster - {@link Group} roster being attested
     */
    private void logAndVerifyAttestation(Provenance provenance, UUID rosterID, Group attributionRoster) {
        final String groupIDLog;
        if (rosterID == null) {
            groupIDLog = "";
        } else {
            groupIDLog = String.format(" for roster %s", new IdType(RESOURCE_TYPE, rosterID.toString()));
        }

        final CodeableConcept reason = provenance.getReasonFirstRep();

        final Provenance.ProvenanceAgentComponent performer = FHIRExtractors.getProvenancePerformer(provenance);
        final String practitionerUUID = performer.getOnBehalfOf().getReference();
        final List<String> attributedPatients = attributionRoster
                .getMember()
                .stream()
                .map(Group.GroupMemberComponent::getEntity)
                .map(Reference::getReference)
                .toList();

        logger.info("Organization {} is attesting a {} purpose between provider {} and patient(s) {}{}", performer.getWho().getReference(),
                reason.getCodingFirstRep().getCode(),
                practitionerUUID, attributedPatients, groupIDLog);

        verifyHeader(practitionerUUID, attributionRoster);
    }

    private void verifyHeader(String practitionerUUID, Group attributionRoster) {
        try {
            org.hl7.fhir.dstu3.model.Practitioner practitioner = stu3Client.read()
                    .resource(org.hl7.fhir.dstu3.model.Practitioner.class)
                    .withId(gov.cms.dpc.fhir.stu3.FHIRExtractors.getEntityUUID(practitionerUUID).toString())
                    .encodedJson()
                    .execute();

            org.hl7.fhir.dstu3.model.Identifier provenancePractitionerNPI = gov.cms.dpc.fhir.stu3.FHIRExtractors.findMatchingIdentifier(practitioner.getIdentifier(), DPCIdentifierSystem.NPPES);
            String groupPractitionerNPI = FHIRExtractors.getAttributedNPI(attributionRoster);

            if (!provenancePractitionerNPI.getValue().equals(groupPractitionerNPI)) {
                throw new WebApplicationException("Provenance header's provider does not match group provider", HttpStatus.SC_UNPROCESSABLE_CONTENT);
            }
        } catch (ResourceNotFoundException e) {
            throw new WebApplicationException("Could not find provider defined in provenance header", HttpStatus.SC_UNPROCESSABLE_CONTENT);
        }
    }

    private void requireRosterId(UUID rosterID) {
        if (rosterID == null) {
            throw new BadRequestException("rosterID is required");
        }
    }

    /**
     * Handle {@link MethodOutcome} for a v3 Group.
     *
     * @param outcome - the result of the create call against attribution
     * @return the Create response, with a v3-correct Location header
     */
    private Response buildCreateResponse(MethodOutcome outcome) {
        final IBaseResource resource = outcome.getResource();
        if (resource == null) {
            throw new WebApplicationException("Unable to get resource.", Response.Status.INTERNAL_SERVER_ERROR);
        }

        final Response.Status status = outcome.getCreated() != null ? Response.Status.CREATED : Response.Status.OK;
        final Response.ResponseBuilder builder = Response.status(status).entity(resource);

        if (outcome.getCreated() != null) {
            final IIdType createdId = outcome.getId() != null ? outcome.getId() : resource.getIdElement();
            builder.location(URI.create(this.baseURL + "/Group/" + createdId.getIdPart()));
            builder.lastModified(resource.getMeta().getLastUpdated());
        }

        return builder.build();
    }
}
