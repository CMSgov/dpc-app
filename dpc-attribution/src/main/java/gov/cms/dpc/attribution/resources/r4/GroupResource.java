package gov.cms.dpc.attribution.resources.r4;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import gov.cms.dpc.attribution.jdbi.r4.PatientDAO;
import gov.cms.dpc.attribution.jdbi.r4.ProviderDAO;
import gov.cms.dpc.attribution.jdbi.r4.RelationshipDAO;
import gov.cms.dpc.attribution.jdbi.r4.RosterDAO;
import gov.cms.dpc.attribution.utils.r4.RESTUtils;
import gov.cms.dpc.common.entities.r4.AttributionRelationship;
import gov.cms.dpc.common.entities.r4.PatientEntity;
import gov.cms.dpc.common.entities.r4.ProviderEntity;
import gov.cms.dpc.common.entities.r4.RosterEntity;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import gov.cms.dpc.fhir.r4.FHIRExtractors;
import gov.cms.dpc.fhir.annotations.FHIR;
import gov.cms.dpc.fhir.annotations.r4.FHIRParameter;
import gov.cms.dpc.fhir.converters.FHIREntityConverter;
import gov.cms.dpc.common.hibernate.attribution.DPCR4HibernateBundle;
import io.dropwizard.hibernate.UnitOfWork;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import org.apache.commons.lang3.tuple.Pair;
import org.hl7.fhir.r4.model.Group;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.instance.model.api.IAnyResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@FHIR
public class GroupResource {

    private static final Logger logger = LoggerFactory.getLogger(GroupResource.class);
    private static final WebApplicationException NOT_FOUND_EXCEPTION = new WebApplicationException("Cannot find Roster resource", Response.Status.NOT_FOUND);

    private final ProviderDAO providerDAO;
    private final PatientDAO patientDAO;
    private final RosterDAO rosterDAO;
    private final RelationshipDAO relationshipDAO;
    private final FHIREntityConverter converter;

    @Inject
    GroupResource(FHIREntityConverter converter, ProviderDAO providerDAO, RosterDAO rosterDAO, PatientDAO patientDAO, RelationshipDAO relationshipDAO) {
        this.rosterDAO = rosterDAO;
        this.providerDAO = providerDAO;
        this.patientDAO = patientDAO;
        this.relationshipDAO = relationshipDAO;
        this.converter = converter;
    }

    @POST
    @FHIR
    @UnitOfWork(DPCR4HibernateBundle.BUNDLE_NAME)
    public Response createRoster(Group attributionRoster) {
        final UUID organizationID = UUID.fromString(FHIRExtractors.getOrganizationID(attributionRoster));

        // Make sure the provider exists for this org
        final String providerNPI = FHIRExtractors.getAttributedNPI(attributionRoster);
        final List<ProviderEntity> providers = this.providerDAO.getProviders(null, providerNPI, organizationID);
        if (providers.isEmpty()) {
            throw new WebApplicationException("Unable to find attributable provider", Response.Status.NOT_FOUND);
        }

        // Check and see if a roster already exists for the provider
        final List<RosterEntity> entities = this.rosterDAO.findEntities(null, organizationID, providerNPI, null);
        if (!entities.isEmpty()) {
            throw new WebApplicationException("Could not create a roster for this provider as they already have one.  Try updating it instead, or first deleting it.",
                Response.Status.FORBIDDEN);
        }

        // Verify that all patients in the roster exist
        verifyAndGetMembers(attributionRoster);

        // Add the first provider and save the persisted Roster.
        final RosterEntity rosterEntity = RosterEntity.fromFHIR(attributionRoster, providers.get(0));
        final RosterEntity persisted = this.rosterDAO.persistEntity(rosterEntity);
        final Group persistedGroup = this.converter.toFHIR(Group.class, persisted);

        return Response.status(Response.Status.CREATED).entity(persistedGroup).build();
    }

    @GET
    @FHIR
    @UnitOfWork(DPCR4HibernateBundle.BUNDLE_NAME)
    public List<Group> rosterSearch(@QueryParam(IAnyResource.SP_RES_ID) UUID rosterID,
                                    @NotEmpty @QueryParam("_tag") String organizationToken,
                                    @QueryParam(Group.SP_CHARACTERISTIC_VALUE) String providerNPI,
                                    @QueryParam(Group.SP_MEMBER) String patientID) {

        final String providerIDPart;
        if (providerNPI != null) {
            providerIDPart = parseCompositeID(providerNPI).getRight().getIdPart();
        } else {
            providerIDPart = null;
        }

        final UUID organizationID = RESTUtils.tokenTagToUUID(organizationToken);
        return this.rosterDAO.findEntities(rosterID, organizationID, providerIDPart, patientID)
                .stream()
                .map(r -> this.converter.toFHIR(Group.class, r))
                .collect(Collectors.toList());
    }

    @GET
    @Path("/{rosterID}/$patients")
    @FHIR
    @UnitOfWork(DPCR4HibernateBundle.BUNDLE_NAME)
    public List<Patient> getAttributedPatients(@NotNull @PathParam("rosterID") UUID rosterID, @QueryParam(value = "active") boolean activeOnly) {
        if (!this.rosterDAO.rosterExists(rosterID)) {
            throw new WebApplicationException(NOT_FOUND_EXCEPTION, Response.Status.NOT_FOUND);
        }

        // We have to do this because Hibernate/Dropwizard gets confused when returning a single type (like String)
        @SuppressWarnings("unchecked") final List<String> patientMBIs = this.patientDAO.fetchPatientMBIByRosterID(rosterID, activeOnly);

        return patientMBIs
                .stream()
                .map(mbi -> {
                    // Generate a fake patient, with only the ID set
                    final Patient p = new Patient();
                    p.addIdentifier().setSystem(DPCIdentifierSystem.MBI.getSystem()).setValue(mbi);
                    return p;
                })
                .toList();
    }


    @PUT
    @Path("/{rosterID}")
    @FHIR
    @UnitOfWork(DPCR4HibernateBundle.BUNDLE_NAME)
    public Group replaceRoster(@PathParam("rosterID") UUID rosterID, Group groupUpdate) {
        // Check that the roster exists and that all patients exist
        if (!this.rosterDAO.rosterExists(rosterID)) {
            throw new WebApplicationException(NOT_FOUND_EXCEPTION, Response.Status.NOT_FOUND);
        }
        List<PatientEntity> patientEntities = verifyAndGetMembers(groupUpdate);

        final RosterEntity rosterEntity = new RosterEntity();
        rosterEntity.setId(rosterID);

        // Remove all roster relationships
        this.relationshipDAO.removeRosterAttributions(rosterID);

        // Build an attribution for each patient and add them to the roster
        patientEntities.stream()
            .map( pe -> new AttributionRelationship(rosterEntity, pe, OffsetDateTime.now(ZoneOffset.UTC)))
            .forEach(relationshipDAO::addAttributionRelationship);

        final RosterEntity rosterEntity1 = rosterDAO.getEntity(rosterID)
                .orElseThrow(() -> NOT_FOUND_EXCEPTION);
        this.rosterDAO.refresh(rosterEntity1);

        return converter.toFHIR(Group.class, rosterEntity1);
    }

    @POST
    @Path("/{rosterID}/$add")
    @FHIR
    @UnitOfWork(DPCR4HibernateBundle.BUNDLE_NAME)
    public Group addRosterMembers(@PathParam("rosterID") UUID rosterID, @FHIRParameter Group groupUpdate) {
        // Get the roster if it exists, if not return NOT_FOUND
        final RosterEntity rosterEntity = this.rosterDAO.getEntity(rosterID)
                .orElseThrow(() -> new WebApplicationException(NOT_FOUND_EXCEPTION, Response.Status.NOT_FOUND));

        // Verify that all patients in the update exist
        List<PatientEntity> patientEntities = verifyAndGetMembers(groupUpdate);
        List<UUID> patientIds = patientEntities.stream()
            .map(PatientEntity::getID)
            .collect(Collectors.toList());

        // Check which patients are already part of the roster and mark them active as of today
        OffsetDateTime periodBegin = OffsetDateTime.now(ZoneOffset.UTC);
        List<AttributionRelationship> existingAttributions = relationshipDAO.lookupAttributionRelationships(rosterID, patientIds);
        existingAttributions.forEach(attribution -> {
            if (attribution.isInactive()) {
                attribution.setPeriodBegin(periodBegin);
            }
            attribution.setInactive(false);
            attribution.setPeriodEnd(null);
        });

        // Build attributions for the new patients
        List<UUID> existingPatientIds = existingAttributions.stream()
            .map(AttributionRelationship::getPatient)
            .map(PatientEntity::getID)
            .collect(Collectors.toList());

        List<AttributionRelationship> newAttributions = patientEntities.stream()
            .filter( patient -> !existingPatientIds.contains(patient.getID()))
            .map( patient -> {
                return new AttributionRelationship(rosterEntity, patient, periodBegin);
            })
            .collect(Collectors.toList());

        // Last but not least, save our changes
        Stream.concat(existingAttributions.stream(), newAttributions.stream())
            .forEach(relationshipDAO::addAttributionRelationship);

        this.rosterDAO.refresh(rosterEntity);
        final RosterEntity rosterEntity1 = this.rosterDAO.getEntity(rosterID)
                .orElseThrow(() -> NOT_FOUND_EXCEPTION);

        return converter.toFHIR(Group.class, rosterEntity1);
    }

    @POST
    @Path("/{rosterID}/$remove")
    @FHIR
    @UnitOfWork(DPCR4HibernateBundle.BUNDLE_NAME)
    public Group removeRosterMembers(@PathParam("rosterID") UUID rosterID, @FHIRParameter Group groupUpdate) {
        if (!this.rosterDAO.rosterExists(rosterID)) {
            throw new WebApplicationException(NOT_FOUND_EXCEPTION, Response.Status.NOT_FOUND);
        }

        groupUpdate
                .getMember()
                .stream()
                .map(Group.GroupMemberComponent::getEntity)
                .map(entity -> {
                    final PatientEntity patientEntity = new PatientEntity();
                    final UUID patientID = UUID.fromString(new IdType(entity.getReference()).getIdPart());
                    patientEntity.setID(patientID);
                    return this.relationshipDAO.lookupAttributionRelationship(rosterID, patientID);
                })
                .map(rOptional -> rOptional.orElseThrow(() -> new WebApplicationException("Cannot find attribution relationship.", Response.Status.BAD_REQUEST)))
                .peek(relationship -> {
                    relationship.setInactive(true);
                    relationship.setPeriodEnd(OffsetDateTime.now(ZoneOffset.UTC));
                })
                .forEach(this.relationshipDAO::updateAttributionRelationship);

        final RosterEntity rosterEntity = this.rosterDAO.getEntity(rosterID)
                .orElseThrow(() -> NOT_FOUND_EXCEPTION);

        return this.converter.toFHIR(Group.class, rosterEntity);
    }

    @DELETE
    @Path("/{rosterID}")
    @FHIR
    @UnitOfWork(DPCR4HibernateBundle.BUNDLE_NAME)
    public Response deleteRoster(@PathParam("rosterID") UUID rosterID) {
        final RosterEntity rosterEntity = this.rosterDAO.getEntity(rosterID)
                .orElseThrow(() -> NOT_FOUND_EXCEPTION);

        this.rosterDAO.delete(rosterEntity);
        return Response.ok().build();
    }


    @Path("/{rosterID}")
    @GET
    @UnitOfWork(DPCR4HibernateBundle.BUNDLE_NAME)
    @Timed
    @ExceptionMetered
    public Group getRoster(
            @PathParam("rosterID") UUID rosterID) {
        logger.debug("API request to retrieve attributed patients for {}", rosterID);

        final RosterEntity rosterEntity = this.rosterDAO.getEntity(rosterID)
                .orElseThrow(() -> NOT_FOUND_EXCEPTION);

        return converter.toFHIR(Group.class, rosterEntity);
    }

    private static Pair<IdType, IdType> parseCompositeID(String queryParam) {
        final String[] split = queryParam.split("\\$", -1);
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

    /**
     * Verifies that all patients referenced in the {@link Group} exist, then returns their {@link PatientEntity}s.
     * If any of the patients don't exist it throws a {@link WebApplicationException}.
     * @param group A {@link Group} whose patient references need to be verified.
     * @return List of {@link PatientEntity}s
     */
    private List<PatientEntity> verifyAndGetMembers(Group group) {
        final UUID orgId = UUID.fromString(FHIRExtractors.getOrganizationID(group));

        // Get list of patient Ids
        List<UUID> patientIds = group
            .getMember()
            .stream()
            .map(Group.GroupMemberComponent::getEntity)
            .map(ref -> UUID.fromString(new IdType(ref.getReference()).getIdPart()))
            .distinct()
            .toList();

        // Get corresponding PatientEntities
        List<PatientEntity> patientEntities = patientDAO.bulkPatientSearchById(orgId, patientIds);

        // Make sure we have the same number of Ids and entities
        if(patientIds.size() != patientEntities.size()) {
            throw new WebApplicationException(
                String.format("All patients in group must exist. Cannot find %d patient(s).", patientIds.size() - patientEntities.size()),
                Response.Status.BAD_REQUEST
            );
        } else {
            return patientEntities;
        }
    }
}
