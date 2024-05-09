package gov.cms.dpc.api.resources.v1;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.name.Named;
import gov.cms.dpc.api.APIHelpers;
import gov.cms.dpc.api.DPCAPIConfiguration;
import gov.cms.dpc.api.auth.OrganizationPrincipal;
import gov.cms.dpc.api.auth.annotations.Authorizer;
import gov.cms.dpc.api.auth.annotations.PathAuthorizer;
import gov.cms.dpc.api.resources.AbstractGroupResource;
import gov.cms.dpc.bluebutton.client.BlueButtonClient;
import gov.cms.dpc.common.annotations.APIV1;
import gov.cms.dpc.common.annotations.NoHtml;
import gov.cms.dpc.common.logging.SplunkTimestamp;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import gov.cms.dpc.fhir.DPCResourceType;
import gov.cms.dpc.fhir.FHIRExtractors;
import gov.cms.dpc.fhir.annotations.FHIR;
import gov.cms.dpc.fhir.annotations.FHIRAsync;
import gov.cms.dpc.fhir.annotations.Profiled;
import gov.cms.dpc.fhir.annotations.ProvenanceHeader;
import gov.cms.dpc.queue.IJobQueue;
import gov.cms.dpc.queue.models.JobQueueBatch;
import io.dropwizard.auth.Auth;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.hl7.fhir.dstu3.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

import static gov.cms.dpc.api.APIHelpers.addOrganizationTag;
import static gov.cms.dpc.fhir.FHIRMediaTypes.*;
import static gov.cms.dpc.fhir.helpers.FHIRHelpers.handleMethodOutcome;


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
    private final DPCAPIConfiguration config;

    @Inject
    public GroupResource(IJobQueue queue, @Named("attribution") IGenericClient client, @APIV1 String baseURL, BlueButtonClient bfdClient, DPCAPIConfiguration config) {
        this.queue = queue;
        this.client = client;
        this.baseURL = baseURL;
        this.bfdClient = bfdClient;
        this.config = config;
    }

    @POST
    @FHIR
    @Timed
    @ExceptionMetered
    @Authorizer
    @Override
    public Response createRoster(@Auth OrganizationPrincipal organizationPrincipal,
                                 @Valid @Profiled @ProvenanceHeader Provenance rosterAttestation,
                                 Group attributionRoster) {
        // Log attestation
        logAndVerifyAttestation(rosterAttestation, null, attributionRoster);
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
    @Authorizer
    @Override
    public Bundle rosterSearch(@Auth OrganizationPrincipal organizationPrincipal,
                               @QueryParam(value = Group.SP_CHARACTERISTIC_VALUE) @NoHtml String providerNPI,
                               @QueryParam(value = Group.SP_MEMBER) @NoHtml String patientID) {

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
    @PathAuthorizer(type = DPCResourceType.Group, pathParam = "rosterID")
    @Timed
    @ExceptionMetered
    @Override
    public Group getRoster(@PathParam("rosterID") UUID rosterID) {
        return this.client
                .read()
                .resource(Group.class)
                .withId(new IdType("Group", rosterID.toString()))
                .encodedJson()
                .execute();
    }

    @PUT
    @Path("/{rosterID}")
    @PathAuthorizer(type = DPCResourceType.Group, pathParam = "rosterID")
    @FHIR
    @Timed
    @ExceptionMetered
    @Override
    public Group updateRoster(@Auth OrganizationPrincipal organizationPrincipal, @PathParam("rosterID") UUID rosterID,
                              @Valid @Profiled @ProvenanceHeader Provenance rosterAttestation, Group rosterUpdate) {

        logAndVerifyAttestation(rosterAttestation, rosterID, rosterUpdate);
        addOrganizationTag(rosterUpdate, organizationPrincipal.getID().toString());
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
    @PathAuthorizer(type = DPCResourceType.Group, pathParam = "rosterID")
    @FHIR
    @Timed
    @ExceptionMetered
    @Override
    public Group addRosterMembers(@Auth OrganizationPrincipal organizationPrincipal, @PathParam("rosterID") UUID rosterID,
                                  @Valid @Profiled @ProvenanceHeader Provenance rosterAttestation, Group groupUpdate) {
        logAndVerifyAttestation(rosterAttestation, rosterID, groupUpdate);
        addOrganizationTag(groupUpdate, organizationPrincipal.getID().toString());
        return this.executeGroupOperation(rosterID, groupUpdate, "add");
    }

    @POST
    @Path("/{rosterID}/$remove")
    @PathAuthorizer(type = DPCResourceType.Group, pathParam = "rosterID")
    @FHIR
    @Timed
    @ExceptionMetered
    @Override
    public Group removeRosterMembers(@PathParam("rosterID") UUID rosterID, Group groupUpdate) {
        return this.executeGroupOperation(rosterID, groupUpdate, "remove");
    }

    @DELETE
    @FHIR
    @Path("/{rosterID}")
    @PathAuthorizer(type = DPCResourceType.Group, pathParam = "rosterID")
    @Timed
    @ExceptionMetered
    @Override
    public Response deleteRoster(@PathParam("rosterID") UUID rosterID) {
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
     * @param resourceTypes - {@link String} of comma separated values corresponding to FHIR {@link DPCResourceType}
     * @param outputFormat  - Optional outputFormats parameter
     * @param sinceParam    - Optional since parameter
     * @return - {@link OperationOutcome} specifying whether or not the request was successful.
     */
    @Override
    @GET // Need this here, since we're using a path param
    @Path("/{rosterID}/$export")
    @PathAuthorizer(type = DPCResourceType.Group, pathParam = "rosterID")
    @Timed
    @ExceptionMetered
    @FHIRAsync
    public Response export(@Auth OrganizationPrincipal organizationPrincipal,
                           @PathParam("rosterID") @NoHtml String rosterID,
                           @QueryParam("_type") @NoHtml String resourceTypes,
                           @DefaultValue(FHIR_NDJSON) @QueryParam("_outputFormat") @NoHtml String outputFormat,
                           @QueryParam("_since") @NoHtml String sinceParam,
                           @HeaderParam("Prefer") @Valid String Prefer,
                           @Context HttpServletRequest request) {
        logger.info("Exporting data for provider: {} _since: {}", rosterID, sinceParam);

        final String eventTime = SplunkTimestamp.getSplunkTimestamp();
        // Check the parameters
        checkExportRequest(outputFormat, Prefer);

        final Group group = fetchGroup(new IdType("Group", rosterID));

        // Get the attributed patients
        final List<String> attributedPatients = fetchPatientMBIs(group);
        if (CollectionUtils.isEmpty(attributedPatients)) {
            throw new WebApplicationException("No active attributed patients found for the group", HttpStatus.SC_NOT_ACCEPTABLE);
        }

        // Generate a job ID and submit it to the queue
        final UUID orgID = FHIRExtractors.getEntityUUID(organizationPrincipal.getOrganization().getId());

        // Grab org and provider NPIs
        final String orgNPI = fetchOrganizationNPI(new IdType("Organization", orgID.toString()));
        final String providerNPI = FHIRExtractors.getAttributedNPI(group);

        // Handle the _type and since query parameters
        final var resources = handleTypeQueryParam(resourceTypes);
        final var since = handleSinceQueryParam(sinceParam);

        final var transactionTime = APIHelpers.fetchTransactionTime(bfdClient);
        final var requestingIP = APIHelpers.fetchRequestingIP(request);
        final String requestUrl = APIHelpers.fetchRequestUrl(request);

        final boolean isSmoke = config.getLookBackExemptOrgs().contains(orgID.toString());
        final UUID jobID = this.queue.createJob(orgID, orgNPI, providerNPI, attributedPatients, resources, since, transactionTime, requestingIP, requestUrl, true, isSmoke);
        final int totalPatients = attributedPatients == null ? 0 : attributedPatients.size();
        final String resourcesRequested = resources.stream().map(DPCResourceType::getPath).filter(Objects::nonNull).collect(Collectors.joining(";"));
        logger.info("dpcMetric=queueSubmitted,requestUrl={},jobID={},orgId={},totalPatients={},resourcesRequested={},queueSubmitTime={}", "/Group/$export",jobID, orgID, totalPatients, resourcesRequested, eventTime);
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
     * @param resourcesListParam - {@link String} of comma separated values corresponding to FHIR {@link DPCResourceType}s
     * @return - A list of {@link DPCResourceType} to return for this request.
     */
    private List<DPCResourceType> handleTypeQueryParam(String resourcesListParam) {
        // If the query param is omitted, the FHIR spec states that all resources should be returned
        if (resourcesListParam == null || resourcesListParam.isEmpty()) {
            return JobQueueBatch.validResourceTypes;
        }

        final var resources = new ArrayList<DPCResourceType>();
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
     */
    private static void checkExportRequest(String outputFormat, String headerPrefer) {
        // _outputFormat only supports FHIR_NDJSON, APPLICATION_NDJSON, NDJSON
        if (!StringUtils.equalsAnyIgnoreCase(outputFormat, FHIR_NDJSON, APPLICATION_NDJSON, NDJSON)) {
            throw new BadRequestException("'_outputFormat' query parameter must be 'application/fhir+ndjson', 'application/ndjson', or 'ndjson' ");
        }
        if (headerPrefer == null || StringUtils.isEmpty(headerPrefer)) {
            throw new BadRequestException("The 'Prefer' header must be 'respond-async'");
        }
        if (StringUtils.isNotEmpty(headerPrefer) && !headerPrefer.equals("respond-async")) {
            throw new BadRequestException("The 'Prefer' header must be 'respond-async'");
        }

    }

    private List<String> fetchPatientMBIs(Group group) {
        if (group.getMember().isEmpty()) {
            throw new WebApplicationException("Cannot perform export with no beneficiaries", Response.Status.NOT_ACCEPTABLE);
        }

        final Parameters parameters = new Parameters();
        parameters.addParameter().setValue(new BooleanType(true)).setName("active");

        // Get the patients, along with their MBIs
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
                .collect(Collectors.toList());
    }

    private String fetchOrganizationNPI(IdType orgID) {
        Organization organization = this.client
                .read()
                .resource(Organization.class)
                .withId(orgID)
                .encodedJson()
                .execute();
        return FHIRExtractors.findMatchingIdentifier(organization.getIdentifier(), DPCIdentifierSystem.NPPES).getValue();
    }

    private Group fetchGroup(IdType groupID) {
        return this.client
                .read()
                .resource(Group.class)
                .withId(groupID)
                .encodedJson()
                .execute();
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
    private void logAndVerifyAttestation(Provenance provenance, UUID rosterID, Group attributionRoster) {

        final String groupIDLog;
        if (rosterID == null) {
            groupIDLog = "";
        } else {
            groupIDLog = String.format(" for roster %s", new IdType("Group", rosterID.toString()));
        }

        final Coding reason = provenance.getReasonFirstRep();

        final Provenance.ProvenanceAgentComponent performer = FHIRExtractors.getProvenancePerformer(provenance);
        final String practitionerUUID = performer.getOnBehalfOfReference().getReference();
        final List<String> attributedPatients = attributionRoster
                .getMember()
                .stream()
                .map(Group.GroupMemberComponent::getEntity)
                .map(Reference::getReference)
                .collect(Collectors.toList());

        logger.info("Organization {} is attesting a {} purpose between provider {} and patient(s) {}{}", performer.getWhoReference().getReference(),
                reason.getCode(),
                practitionerUUID, attributedPatients, groupIDLog);

        verifyHeader(practitionerUUID, attributionRoster);
    }

    private void verifyHeader(String practitionerUUID, Group attributionRoster) {
        try {
            Practitioner practitioner = client.read()
                    .resource(Practitioner.class)
                    .withId(FHIRExtractors.getEntityUUID(practitionerUUID).toString())
                    .encodedJson()
                    .execute();

            Identifier provenancePractitionerNPI = FHIRExtractors.findMatchingIdentifier(practitioner.getIdentifier(), DPCIdentifierSystem.NPPES);
            String groupPractitionerNPI = FHIRExtractors.getAttributedNPI(attributionRoster);

            if (!provenancePractitionerNPI.getValue().equals(groupPractitionerNPI)) {
                throw new WebApplicationException("Provenance header's provider does not match group provider", HttpStatus.SC_UNPROCESSABLE_ENTITY);
            }
        } catch (ResourceNotFoundException e) {
            throw new WebApplicationException("Could not find provider defined in provenance header", HttpStatus.SC_UNPROCESSABLE_ENTITY);
        }

    }

    /**
     * Convert a single resource type in a query param into a {@link DPCResourceType}.
     *
     * @param queryResourceType - The text from the query param
     * @return If match is found a {@link DPCResourceType}
     */
    private static Optional<DPCResourceType> matchResourceType(String queryResourceType) {
        final var canonical = queryResourceType.trim().toUpperCase();
        // Implementation Note: resourceTypeMap is a small list <3 so hashing isn't faster
        return JobQueueBatch.validResourceTypes.stream()
                .filter(validResource -> validResource.toString().equalsIgnoreCase(canonical))
                .findFirst();
    }
}
