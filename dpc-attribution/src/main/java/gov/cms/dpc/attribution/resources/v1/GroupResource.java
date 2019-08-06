package gov.cms.dpc.attribution.resources.v1;

import ca.uhn.fhir.rest.param.CompositeParam;
import ca.uhn.fhir.rest.param.TokenParam;
import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import gov.cms.dpc.attribution.jdbi.PatientDAO;
import gov.cms.dpc.attribution.jdbi.ProviderDAO;
import gov.cms.dpc.attribution.jdbi.RosterDAO;
import gov.cms.dpc.attribution.resources.AbstractGroupResource;
import gov.cms.dpc.attribution.utils.RESTUtils;
import gov.cms.dpc.common.entities.AttributionRelationship;
import gov.cms.dpc.common.entities.PatientEntity;
import gov.cms.dpc.common.entities.ProviderEntity;
import gov.cms.dpc.common.entities.RosterEntity;
import gov.cms.dpc.common.interfaces.AttributionEngine;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import gov.cms.dpc.fhir.FHIRExtractors;
import gov.cms.dpc.fhir.annotations.FHIR;
import io.dropwizard.hibernate.UnitOfWork;
import io.swagger.annotations.*;
import org.apache.commons.lang3.tuple.Pair;
import org.hibernate.validator.constraints.NotEmpty;
import org.hl7.fhir.dstu3.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Api(value = "Group")
public class GroupResource extends AbstractGroupResource {

    private static final Logger logger = LoggerFactory.getLogger(GroupResource.class);
    private static final WebApplicationException NOT_FOUND_EXCEPTION = new WebApplicationException("Cannot find Roster resource", Response.Status.NOT_FOUND);

    private final AttributionEngine engine;
    private final ProviderDAO providerDAO;
    private final PatientDAO patientDAO;
    private final RosterDAO rosterDAO;

    @Inject
    GroupResource(AttributionEngine engine, ProviderDAO providerDAO, RosterDAO rosterDAO, PatientDAO patientDAO) {
        this.engine = engine;
        this.rosterDAO = rosterDAO;
        this.providerDAO = providerDAO;
        this.patientDAO = patientDAO;
    }

    @POST
    @FHIR
    @UnitOfWork
    @ApiOperation(value = "Create Attribution Roster", notes = "FHIR endpoint to create an Attribution roster (Group resource) associated to the provider listed in the in the Group characteristics.")
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Successfully created Roster"),
            @ApiResponse(code = 200, message = "Roster already exists")
    })
    @Override
    public Response createRoster(Group attributionRoster) {
        // Lookup the Provider by NPI
        final String providerNPI = FHIRExtractors.getAttributedNPI(attributionRoster);

        // Check and see if a roster already exists for the provider
        final UUID organizationID = UUID.fromString(FHIRExtractors.getOrganizationID(attributionRoster));
        final List<RosterEntity> entities = this.rosterDAO.findEntities(organizationID, providerNPI, null);
        if (!entities.isEmpty()) {
            return Response.status(Response.Status.OK).entity(entities.get(0).toFHIR()).build();
        }
        final List<ProviderEntity> providers = this.providerDAO.getProviders(null, providerNPI, organizationID);
        if (providers.isEmpty()) {
            throw new WebApplicationException("Unable to find attributable provider", Response.Status.NOT_FOUND);
        }

        final RosterEntity rosterEntity = RosterEntity.fromFHIR(attributionRoster, providers.get(0));
        // Add the first provider
        final RosterEntity persisted = this.rosterDAO.persistEntity(rosterEntity);
        final Group persistedGroup = persisted.toFHIR();

        return Response.status(Response.Status.CREATED).entity(persistedGroup).build();
    }

    @GET
    @FHIR
    @UnitOfWork
    @ApiOperation(value = "Search for attribution rosters", notes = "FHIR endpoint to search for Attribution Rosters." +
            "<p> You can search for Groups associated to a given provider (via the Provider NPI) and groups for which a patient is a member of (by the Patient resource ID)")
    @Override
    public Bundle rosterSearch(@ApiParam(value = "Organization ID")
                               @NotEmpty @QueryParam("_tag") String organizationToken,
                               @ApiParam(value = "Provider NPI")
                               @QueryParam(Group.SP_CHARACTERISTIC_VALUE) String providerNPI,
                               @ApiParam(value = "Patient ID")
                               @QueryParam(Group.SP_MEMBER) String patientID) {

        final Pair<IdType, IdType> idPair = parseCompositeID(providerNPI);
        final Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.SEARCHSET);

        final UUID organizationID = RESTUtils.parseTokenTag(organizationToken);
        this.rosterDAO.findEntities(organizationID, idPair.getRight().getIdPart(), patientID)
                .stream()
                .map(RosterEntity::toFHIR)
                .forEach(entity -> bundle.addEntry().setResource(entity));

        bundle.setTotal(bundle.getEntry().size());
        return bundle;
    }

    @GET
    @Path("/{rosterID}/$patients")
    @FHIR
    @UnitOfWork
    @ApiOperation(value = "Get attributed patient IDs", notes = "FHIR endpoint to retrieve the Patient MBIs for roster entities." +
            "<p> This is an operation optimized for returning only the MBIs for the Patient resources linked to the Roster. " +
            "It returns empty Patient resources with only the MBI added as an identifier.")
    @ApiResponses(@ApiResponse(code = 404, message = "Cannot find attribution roster"))
    @Override
    public Bundle getAttributedPatients(@NotNull @PathParam("rosterID") UUID rosterID) {
        final RosterEntity existingRoster = this.rosterDAO.getEntity(rosterID)
                .orElseThrow(() -> NOT_FOUND_EXCEPTION);

        final Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.SEARCHSET);

        // We have to do this because Hibernate/Dropwizard get confused when returning a single type (link String)
        @SuppressWarnings("unchecked") final List<String> patientMBIs = this.patientDAO.fetchPatientMBIByRosterID(existingRoster.getId());

        final List<Bundle.BundleEntryComponent> patients = patientMBIs
                .stream()
                .map(mbi -> {
                    // Generate a fake patient, with only the ID set
                    final Patient p = new Patient();
                    p.addIdentifier().setSystem(DPCIdentifierSystem.MBI.getSystem()).setValue(mbi);
                    return new Bundle.BundleEntryComponent().setResource(p);
                })
                .collect(Collectors.toList());

        bundle.setTotal(patientMBIs.size());
        bundle.setEntry(patients);

        return bundle;
    }


    @PUT
    @Path("/{rosterID}")
    @FHIR
    @UnitOfWork
    @ApiOperation(value = "Update roster", notes = "FHIR endpoint to update the given Group resource with members to add or remove.")
    @ApiResponses(@ApiResponse(code = 404, message = "Cannot find attribution roster"))
    @Override
    public Group updateRoster(@PathParam("rosterID") UUID rosterID, Group groupUpdate) {
        final RosterEntity existingRoster = this.rosterDAO.getEntity(rosterID)
                .orElseThrow(() -> NOT_FOUND_EXCEPTION);

        // Verify that we don't have any duplicated patient references, which causes havoc with the merge logic.
        final Set<Reference> memberReferences = groupUpdate
                .getMember()
                .stream()
                .map(Group.GroupMemberComponent::getEntity)
                .filter(distinctByKey(Reference::getReference))
                .collect(Collectors.toSet());

        if (memberReferences.size() < groupUpdate.getMember().size()) {
            throw new WebApplicationException("Cannot have a Patient listed twice in Group update", Response.Status.BAD_REQUEST);
        }

        // Do we really have to do a linear search to figure out who to add/remove?
        // This should not be here for long
        final List<AttributionRelationship> existingAttributions = existingRoster.getAttributions();

        // Remove patients first
        groupUpdate
                .getMember()
                .stream()
                .filter(Group.GroupMemberComponent::getInactive)
                .map(Group.GroupMemberComponent::getEntity)
                .forEach(entity -> removeAttributedPatients(existingAttributions, entity));

        // Now, add all the new ones
        groupUpdate
                .getMember()
                .stream()
                .filter(member -> !member.getInactive())
                .map(Group.GroupMemberComponent::getEntity)
                .map(ref -> {
                    final PatientEntity pe = new PatientEntity();
                    pe.setPatientID(UUID.fromString(new IdType(ref.getReference()).getIdPart()));
                    return pe;
                })
                .map(pe -> new AttributionRelationship(existingRoster, pe))
                .forEach(relationship -> {
                    final Optional<AttributionRelationship> found = findAttributionRelationship(existingAttributions, relationship);
                    if (found.isEmpty()) {
                        existingAttributions.add(relationship);
                    }
                });

        existingRoster.setAttributions(existingAttributions);

        return this.rosterDAO.updateRoster(existingRoster).toFHIR();
    }

    @DELETE
    @Path("/{rosterID}")
    @FHIR
    @UnitOfWork
    @ApiOperation(value = "Delete roster", notes = "FHIR Endpoint to delete attribution roster")
    @ApiResponses(@ApiResponse(code = 404, message = "Cannot find attribution roster"))
    @Override
    public Response deleteRoster(@PathParam("rosterID") UUID rosterID) {
        final RosterEntity rosterEntity = this.rosterDAO.getEntity(rosterID)
                .orElseThrow(() -> NOT_FOUND_EXCEPTION);

        this.rosterDAO.delete(rosterEntity);
        return Response.ok().build();
    }

    //    @POST
//    @Path("/$submit")
//    @FHIR
//    @Override
//    @Timed
//    @ExceptionMetered
//    @ApiOperation(value = "Submit Attribution Bundle", notes = "FHIR endpoint that accepts a Bundle resource, corresponding to an Attribution set.")
//    @ApiResponses(
//            @ApiResponse(code = 201, message = "Attribution Bundle was successfully added")
//    )
//    public Response submitRoster(Bundle providerBundle) {
//        logger.debug("API request to submit roster");
//        // FIXME(nickrobison): Remove this! This is really gross, but will come out as part of our Group refactoring
//        final UUID organizationID = providerBundle.getEntryFirstRep()
//                .getResource()
//                .getMeta()
//                .getTag()
//                .stream()
//                .filter(tag -> tag.getSystem().equals(DPCIdentifierSystem.DPC.getSystem()))
//                .map(Coding::getCode)
//                .map(UUID::fromString)
//                .findFirst()
//                .orElseThrow(() -> new WebApplicationException("Must have Metadata identifier", Response.Status.INTERNAL_SERVER_ERROR));
//
//        this.engine.addAttributionRelationships(providerBundle, organizationID);
//
//        return Response.status(Response.Status.CREATED).build();
//    }


    @Path("/{rosterID}")
    @GET
    @UnitOfWork
    @Timed
    @ExceptionMetered
    @ApiOperation(value = "Get attributed patients", notes = "Returns a list of Patient MBIs that are attributed to the given Provider.")
    @ApiResponses(value = {
            @ApiResponse(code = 500, message = "Internal server error that prevented the service from looking up the attributed patients"),
            @ApiResponse(code = 404, message = "No provider exists with the given NPI")
    })
    @Override
    public Group getRoster(
            @ApiParam(value = "Provider NPI", required = true)
            @PathParam("rosterID") UUID rosterID) {
        logger.debug("API request to retrieve attributed patients for {}", rosterID);

        return this.rosterDAO.getEntity(rosterID)
                .orElseThrow(() -> NOT_FOUND_EXCEPTION)
                .toFHIR();

//        Optional<List<String>> attributedBeneficiaries;
//        try {
//            // Create a practitioner resource for retrieval
//            attributedBeneficiaries = engine.getAttributedPatientIDs(FHIRBuilders.buildPractitionerFromNPI(groupID));
//        } catch (Exception e) {
//            logger.error("Cannot get attributed patients for {}", groupID, e);
//            throw new WebApplicationException(String.format("Unable to retrieve attributed patients for: %s", groupID), Response.Status.INTERNAL_SERVER_ERROR);
//        }
//
//        if (attributedBeneficiaries.isEmpty()) {
//            throw new WebApplicationException(String.format("Unable to find provider: %s", groupID), Response.Status.NOT_FOUND);
//        }
//        return attributedBeneficiaries.get();
    }
//
//    @Path("/{groupID}/{patientID}")
//    @GET
//    @Override
//    @Timed
//    @ExceptionMetered
//    @ApiOperation(value = "Verify attribution relationship", notes = "Returns whether or not the Patient (identified by MBI) is attributed to the given Provider (identified by NPI)")
//    @ApiResponses(value = {
//            @ApiResponse(code = 200, message = "Patient is attributed to the given provider"),
//            @ApiResponse(code = 406, message = "Patient is not attributed to the given provider")
//    })
//    public boolean isAttributed(@ApiParam(value = "Provider NPI", required = true)
//                                @PathParam("groupID") String groupID,
//                                @ApiParam(value = "Patient MBI", required = true)
//                                @PathParam("patientID") String patientID) {
//        logger.debug("API request to determine attribution between {} and {}", groupID, patientID);
//        final boolean attributed = engine.isAttributed(
//                FHIRBuilders.buildPractitionerFromNPI(groupID),
//                FHIRBuilders.buildPatientFromMBI(patientID));
//        if (!attributed) {
//            throw new WebApplicationException(HttpStatus.NOT_ACCEPTABLE_406);
//        }
//        return true;
//    }
//
//    @Path("/{groupID}/{patientID}")
//    @PUT
//    @Override
//    @Timed
//    @ExceptionMetered
//    @ApiOperation(value = "Attribute patient to provider", notes = "Method to attributed a patient (identified by MBI) to a given provider (identified by NPI)")
//    @ApiResponses(value = @ApiResponse(code = 500, message = "Service was unable to attribute patient to provider"))
//    public void attributePatient(@ApiParam(value = "Provider NPI", required = true)
//                                 @PathParam("groupID") String groupID,
//                                 @ApiParam(value = "Patient MBI", required = true)
//                                 @PathParam("patientID") String patientID) {
//        logger.debug("API request to add attribution between {} and {}", groupID, patientID);
//        try {
//            this.engine.addAttributionRelationship(
//                    FHIRBuilders.buildPractitionerFromNPI(groupID),
//                    FHIRBuilders.buildPatientFromMBI(patientID));
//        } catch (Exception e) {
//            logger.error("Error attributing patient", e);
//            throw new WebApplicationException("Cannot attribute patients", HttpStatus.INTERNAL_SERVER_ERROR_500);
//        }
//    }
//
//    @Path("/{groupID}/{patientID}")
//    @Override
//    @DELETE
//    @Timed
//    @ExceptionMetered
//    @ApiOperation(value = "Remove patient attribution", notes = "Method to remove an attributed patient (identified by MBI) from a given provider (identified by NPI)")
//    @ApiResponses(value = @ApiResponse(code = 500, message = "Service was unable to remove attribution between patient and provider"))
//    public void removeAttribution(@ApiParam(value = "Provider NPI", required = true)
//                                  @PathParam("groupID") String groupID,
//                                  @ApiParam(value = "Patient MBI", required = true)
//                                  @PathParam("patientID") String patientID) {
//        logger.debug("API request to remove attribution between {} and {}", groupID, patientID);
//        try {
//            this.engine.removeAttributionRelationship(
//                    FHIRBuilders.buildPractitionerFromNPI(groupID),
//                    FHIRBuilders.buildPatientFromMBI(patientID));
//        } catch (Exception e) {
//            logger.error("Error removing patient", e);
//            throw new WebApplicationException("Cannot remove attributed patients", HttpStatus.INTERNAL_SERVER_ERROR_500);
//        }
//    }

    /**
     * Remove {@link AttributionRelationship} from the given {@link List} of {@link AttributionRelationship}, if it matches
     *
     * @param relationships   - {@link List} of {@link AttributionRelationship} entities to find in list
     * @param entityReference - {{@link AttributionRelationship} to find in list
     */
    private static void removeAttributedPatients(List<AttributionRelationship> relationships, Reference entityReference) {
        final IdType idType = new IdType(entityReference.getReference());
        final Optional<AttributionRelationship> maybeAttributed = relationships
                .stream()
                .filter(relationship -> relationship.getPatient().getPatientID().toString().equals(idType.getIdPart()))
                .findAny();

        maybeAttributed.ifPresent(relationships::remove);
    }

    /**
     * Find a matching {@link AttributionRelationship} from a {@link List} of {@link AttributionRelationship} entities
     *
     * @param relationships - {@link List} of {@link AttributionRelationship} entities to match against
     * @param relationship  - {@link AttributionRelationship} to compare against the list
     * @return - {@link Optional} if {@link AttributionRelationship} is in the list
     */
    private static Optional<AttributionRelationship> findAttributionRelationship(List<AttributionRelationship> relationships, AttributionRelationship relationship) {
        return relationships
                .stream()
                .filter(r1 -> matchAttribution(r1, relationship))
                .findAny();
    }

    /**
     * Determines if two {@link AttributionRelationship} entity are equal, by looking at the {@link RosterEntity} and {@link PatientEntity} in both entities.
     *
     * @param r1 - {@link AttributionRelationship} left side of the comparison
     * @param r2 - {@link AttributionRelationship} right side of the comparison
     * @return - {@code true} relationships are equal. {@code false} relationships are not equal
     */
    private static boolean matchAttribution(AttributionRelationship r1, AttributionRelationship r2) {
        return r1.getRoster().getId().equals(r2.getRoster().getId()) && r1.getPatient().getPatientID().equals(r2.getPatient().getPatientID());
    }

    /**
     * Stateful {@link Predicate} filter that allows us to verify if we've seen a {@link Reference} before, since HAPI doesn't let us do directly object equality.
     *
     * @param keyExtractor - {@link Function} for extracting the value to compare
     * @param <T>          - {@link T} type of comparison value
     * @return - {@link Predicate} for use with streams.
     */
    private static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply(t));
    }

    private static Pair<IdType, IdType> parseCompositeID(String queryParam) {
        final String[] split = queryParam.split("\\$");
        if (split.length != 2) {
            throw new IllegalArgumentException("Cannot parse query param: " + queryParam);
        }
        // Left tag
        final Pair<String, String> leftPair = FHIRExtractors.parseTag(split[0]);
        final IdType leftID = new IdType(leftPair.getLeft(), leftPair.getRight());

        // Right tag
        final Pair<String, String> rightPair = FHIRExtractors.parseTag(split[1]);
        final IdType rightID = new IdType(rightPair.getLeft(), rightPair.getRight());

        return Pair.of(leftID, rightID);
    }
}
