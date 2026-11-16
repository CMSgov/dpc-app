package gov.cms.dpc.api.resources.r4;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import gov.cms.dpc.api.APIHelpers;
import gov.cms.dpc.api.auth.OrganizationPrincipal;
import gov.cms.dpc.api.auth.annotations.Authorizer;
import gov.cms.dpc.api.auth.annotations.PathAuthorizer;
import gov.cms.dpc.common.annotations.APIV3;
import gov.cms.dpc.common.annotations.NoHtml;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import gov.cms.dpc.fhir.DPCResourceType;
import gov.cms.dpc.fhir.FHIRHeaders;
import gov.cms.dpc.fhir.annotations.FHIR;
import gov.cms.dpc.fhir.annotations.FHIRAsync;
import gov.cms.dpc.fhir.annotations.Profiled;
import gov.cms.dpc.fhir.annotations.ProvenanceHeader;
import gov.cms.dpc.fhir.r4.FHIRExtractors;
import gov.cms.dpc.oneecosystem.client.OneEcosystemClient;
import gov.cms.dpc.oneecosystem.models.OneEcosystemExportRequest;
import gov.cms.dpc.oneecosystem.models.OneEcosystemExportResponse;
import gov.cms.dpc.queue.IJobQueue;
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
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static gov.cms.dpc.fhir.FHIRMediaTypes.FHIR_NDJSON;

@Api(value = "Group", authorizations = @Authorization(value = "access_token"))
@Path("/v3/Group")
public class GroupResource extends AbstractGroupResource {

    private static final Logger logger = LoggerFactory.getLogger(GroupResource.class);

    private final String baseURL;
    private final OneEcosystemClient oneEcosystemClient;
    private final IJobQueue queue;

    @Inject
    public GroupResource(@Named("attribution") IGenericClient client,
                         @APIV3 String baseURL,
                         OneEcosystemClient oneEcosystemClient,
                         IJobQueue queue) {
        super(client);
        this.baseURL = baseURL;
        this.oneEcosystemClient = oneEcosystemClient;
        this.queue = queue;
    }

    @POST
    @FHIR
    @Timed
    @ExceptionMetered
    @Authorizer
    @ApiOperation(value = "Create Attribution Group")
    @ApiImplicitParams(
            @ApiImplicitParam(name = "X-Provenance", required = true, paramType = "header", type = "string", dataTypeClass = Provenance.class))
    @ApiResponses({
            @ApiResponse(code = 201, message = "Successfully created Roster"),
            @ApiResponse(code = 422, message = "Provider in Provenance header does not match Provider in Roster")
    })
    @Override
    public Response createRoster(@ApiParam(hidden = true) @Auth OrganizationPrincipal organizationPrincipal,
                                 @ApiParam(hidden = true) @Valid @Profiled @ProvenanceHeader Provenance rosterAttestation,
                                 Group attributionRoster) {
        logAndVerifyAttestation(rosterAttestation, null, attributionRoster);
        APIHelpers.addOrganizationTag(attributionRoster, organizationPrincipal.getOrganization().getId());

        final MethodOutcome outcome = this.client
                .create()
                .resource(attributionRoster)
                .encodedJson()
                .execute();

        return Response.status(Response.Status.CREATED).entity(outcome.getResource()).build();
    }

    @GET
    @Path("/{rosterID}")
    @FHIR
    @PathAuthorizer(type = DPCResourceType.Group, pathParam = "rosterID")
    @Timed
    @ExceptionMetered
    @ApiOperation(value = "Fetch Attribution Group")
    @ApiResponses(@ApiResponse(code = 404, message = "Cannot find Roster with given ID"))
    @Override
    public Group getRoster(@ApiParam(value = "Attribution roster ID") @PathParam("rosterID") UUID rosterID) {
        return fetchGroup(new IdType("Group", rosterID.toString()));
    }

    @PUT
    @Path("/{rosterID}")
    @FHIR
    @PathAuthorizer(type = DPCResourceType.Group, pathParam = "rosterID")
    @Timed
    @ExceptionMetered
    @ApiOperation(value = "Create or update Attribution Group")
    @ApiImplicitParams(
            @ApiImplicitParam(name = "X-Provenance", required = true, paramType = "header", type = "string", dataTypeClass = Provenance.class))
    @ApiResponses({
            @ApiResponse(code = 404, message = "Cannot find Roster with given ID"),
            @ApiResponse(code = 422, message = "Provider in Provenance header does not match Provider in Roster")
    })
    @Override
    public Group updateRoster(@ApiParam(hidden = true) @Auth OrganizationPrincipal organizationPrincipal,
                              @PathParam("rosterID") UUID rosterID,
                              @ApiParam(hidden = true) @Valid @Profiled @ProvenanceHeader Provenance rosterAttestation,
                              Group rosterUpdate) {
        logAndVerifyAttestation(rosterAttestation, rosterID, rosterUpdate);
        APIHelpers.addOrganizationTag(rosterUpdate, organizationPrincipal.getOrganization().getId());

        final MethodOutcome outcome = this.client
                .update()
                .resource(rosterUpdate)
                .withId(new IdType("Group", rosterID.toString()))
                .encodedJson()
                .execute();

        return (Group) outcome.getResource();
    }

    @POST
    @Path("/{rosterID}/$member-add")
    @FHIR
    @PathAuthorizer(type = DPCResourceType.Group, pathParam = "rosterID")
    @Timed
    @ExceptionMetered
    @ApiOperation(value = "Add a member to an Attribution Group")
    @ApiImplicitParams(
            @ApiImplicitParam(name = "X-Provenance", required = true, paramType = "header", type = "string", dataTypeClass = Provenance.class))
    @ApiResponses(@ApiResponse(code = 404, message = "Cannot find Roster with given ID"))
    @Override
    public Group memberAdd(@ApiParam(hidden = true) @Auth OrganizationPrincipal organizationPrincipal,
                           @PathParam("rosterID") UUID rosterID,
                           @ApiParam(hidden = true) @Valid @Profiled @ProvenanceHeader Provenance rosterAttestation,
                           Group groupUpdate) {
        logAndVerifyAttestation(rosterAttestation, rosterID, groupUpdate);
        APIHelpers.addOrganizationTag(groupUpdate, organizationPrincipal.getOrganization().getId());
        return executeGroupOperation(rosterID, groupUpdate, "add");
    }

    @POST
    @Path("/{rosterID}/$member-remove")
    @FHIR
    @PathAuthorizer(type = DPCResourceType.Group, pathParam = "rosterID")
    @Timed
    @ExceptionMetered
    @ApiOperation(value = "Remove a member from an Attribution Group")
    @ApiResponses(@ApiResponse(code = 404, message = "Cannot find Roster with given ID"))
    @Override
    public Group memberRemove(@PathParam("rosterID") UUID rosterID, Group groupUpdate) {
        return executeGroupOperation(rosterID, groupUpdate, "remove");
    }

    @GET
    @Path("/{rosterID}/$export")
    @PathAuthorizer(type = DPCResourceType.Group, pathParam = "rosterID")
    @Timed
    @ExceptionMetered
    @FHIRAsync
    @ApiOperation(value = "Begin Group export request", tags = {"Group", "Bulk Data"})
    @ApiImplicitParams(
            @ApiImplicitParam(name = "Prefer", required = true, paramType = "header", type = "string", value = "respond-async", dataTypeClass = String.class))
    @ApiResponses(
            @ApiResponse(code = 202, message = "Export request has started", responseHeaders = @ResponseHeader(name = "Content-Location", description = "URL to query job status", response = UUID.class)))
    @Override
    public Response export(@ApiParam(hidden = true) @Auth OrganizationPrincipal organizationPrincipal,
                           @PathParam("rosterID") @NoHtml String rosterID,
                           @QueryParam("_type") @NoHtml String resourceTypes,
                           @DefaultValue(FHIR_NDJSON) @QueryParam("_outputFormat") @NoHtml String outputFormat,
                           @QueryParam("_since") @NoHtml String sinceParam,
                           @QueryParam("_typeFilter") @NoHtml String typeFilterParam,
                           @HeaderParam(FHIRHeaders.PREFER_HEADER) @Valid String prefer,
                           @Context HttpServletRequest request) {
        checkExportRequest(outputFormat, prefer);

        final Group group = fetchGroup(new IdType("Group", rosterID));
        final List<String> attributedMbis = fetchPatientMBIs(group);
        if (CollectionUtils.isEmpty(attributedMbis)) {
            throw new WebApplicationException("No active attributed patients found for the group", Response.Status.NOT_ACCEPTABLE);
        }

        final UUID orgID = gov.cms.dpc.fhir.stu3.FHIRExtractors.getEntityUUID(organizationPrincipal.getOrganization().getId());
        final UUID groupUUID = UUID.fromString(rosterID);

        final List<DPCResourceType> resourceTypeList = this.handleTypeQueryParam(resourceTypes);
        final List<String> resourcePaths = resourceTypeList.stream().map(DPCResourceType::getPath).collect(Collectors.toList());

        final OffsetDateTime sinceDate = this.handleSinceQueryParam(sinceParam);
        // AbstractResourceWithExport returns null when _since is omitted; NEWBE's request
        // model requires a non-null `since`, so default to EPOCH here rather than upstream.
        final Instant since = (sinceDate == null) ? Instant.EPOCH : sinceDate.toInstant();

        final String typeFilter = typeFilterParam == null ? "" : typeFilterParam;
        final String requestingIP = APIHelpers.fetchRequestingIP(request);
        final String requestUrl = APIHelpers.fetchRequestUrl(request);

        final OneEcosystemExportRequest exportRequest =
                new OneEcosystemExportRequest(attributedMbis, since, resourcePaths, typeFilter);

        final OneEcosystemExportResponse exportResponse = this.oneEcosystemClient.export(exportRequest);
        if (exportResponse.statusCode() != 202) {
            throw new WebApplicationException("Unable to start export job", HttpStatus.SC_BAD_GATEWAY);
        }
        final UUID backendJobId = extractJobId(exportResponse.contentLocation());

        this.queue.createExternalJob(
                backendJobId,
                orgID,
                attributedMbis,
                resourceTypeList,
                sinceDate == null ? null : sinceDate,
                requestingIP,
                requestUrl,
                groupUUID,
                true);

        logger.info("dpcMetric=queueSubmitted,requestUrl={},jobID={},orgId={},totalPatients={}",
                "/v3/Group/$export", backendJobId, orgID, attributedMbis.size());

        return Response.status(Response.Status.ACCEPTED)
                .contentLocation(URI.create(this.baseURL + "/Jobs/" + backendJobId)).build();
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

        return patients.getEntry().stream()
                .map(entry -> (Patient) entry.getResource())
                .map(FHIRExtractors::getPatientMBI)
                .collect(Collectors.toList());
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

    private void logAndVerifyAttestation(Provenance provenance, UUID rosterID, Group attributionRoster) {
        final String groupIDLog = rosterID == null ? "" : String.format(" for roster %s", new IdType("Group", rosterID.toString()));
        final CodeableConcept reason = provenance.getReasonFirstRep();
        final Provenance.ProvenanceAgentComponent performer = FHIRExtractors.getProvenancePerformer(provenance);
        final String practitionerUUID = performer.getOnBehalfOf().getReference();
        final List<String> attributedPatients = attributionRoster.getMember().stream()
                .map(Group.GroupMemberComponent::getEntity)
                .map(Reference::getReference)
                .collect(Collectors.toList());

        logger.info("Organization {} is attesting a {} purpose between provider {} and patient(s) {}{}",
                performer.getWho().getReference(), reason.getCodingFirstRep().getCode(), practitionerUUID, attributedPatients, groupIDLog);

        verifyHeader(practitionerUUID, attributionRoster);
    }

    private void verifyHeader(String practitionerUUID, Group attributionRoster) {
        try {
            final Practitioner practitioner = this.client.read()
                    .resource(Practitioner.class)
                    .withId(gov.cms.dpc.fhir.stu3.FHIRExtractors.getEntityUUID(practitionerUUID).toString())
                    .encodedJson()
                    .execute();

            final Identifier provenancePractitionerNPI = FHIRExtractors.findMatchingIdentifier(practitioner.getIdentifier(), DPCIdentifierSystem.NPPES);
            final String groupPractitionerNPI = FHIRExtractors.getAttributedNPI(attributionRoster);

            if (!provenancePractitionerNPI.getValue().equals(groupPractitionerNPI)) {
                throw new WebApplicationException("Provenance header's provider does not match group provider", HttpStatus.SC_UNPROCESSABLE_CONTENT);
            }
        } catch (ResourceNotFoundException e) {
            throw new WebApplicationException("Could not find provider defined in provenance header", HttpStatus.SC_UNPROCESSABLE_CONTENT);
        }
    }
}
