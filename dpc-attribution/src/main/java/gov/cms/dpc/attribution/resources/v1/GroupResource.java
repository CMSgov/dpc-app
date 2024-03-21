package gov.cms.dpc.attribution.resources.v1;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import gov.cms.dpc.attribution.DPCAttributionConfiguration;
import gov.cms.dpc.attribution.jdbi.PatientDAO;
import gov.cms.dpc.attribution.jdbi.ProviderDAO;
import gov.cms.dpc.attribution.jdbi.RelationshipDAO;
import gov.cms.dpc.attribution.jdbi.RosterDAO;
import gov.cms.dpc.attribution.resources.AbstractGroupResource;
import gov.cms.dpc.attribution.utils.RESTUtils;
import gov.cms.dpc.common.entities.AttributionRelationship;
import gov.cms.dpc.common.entities.PatientEntity;
import gov.cms.dpc.common.entities.ProviderEntity;
import gov.cms.dpc.common.entities.RosterEntity;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import gov.cms.dpc.fhir.FHIRExtractors;
import gov.cms.dpc.fhir.annotations.FHIR;
import gov.cms.dpc.fhir.annotations.FHIRParameter;
import gov.cms.dpc.fhir.converters.FHIREntityConverter;
import io.dropwizard.hibernate.UnitOfWork;
import org.apache.commons.lang3.tuple.Pair;
import org.hl7.fhir.dstu3.model.Group;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Patient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class GroupResource extends AbstractGroupResource {

    private static final Logger logger = LoggerFactory.getLogger(GroupResource.class);
    private static final WebApplicationException NOT_FOUND_EXCEPTION = new WebApplicationException("Cannot find Roster resource", Response.Status.NOT_FOUND);
    private static final WebApplicationException TOO_MANY_MEMBERS_EXCEPTION = new WebApplicationException("Roster limit reached", Response.Status.BAD_REQUEST);

    private final ProviderDAO providerDAO;
    private final PatientDAO patientDAO;
    private final RosterDAO rosterDAO;
    private final RelationshipDAO relationshipDAO;
    private final DPCAttributionConfiguration config;
    private final FHIREntityConverter converter;

    @Inject
    GroupResource(FHIREntityConverter converter, ProviderDAO providerDAO, RosterDAO rosterDAO, PatientDAO patientDAO, RelationshipDAO relationshipDAO, DPCAttributionConfiguration config) {
        this.rosterDAO = rosterDAO;
        this.providerDAO = providerDAO;
        this.patientDAO = patientDAO;
        this.relationshipDAO = relationshipDAO;
        this.config = config;
        this.converter = converter;
    }

    @POST
    @FHIR
    @UnitOfWork
    @Override
    public Response createRoster(Group attributionRoster) {
        if (rosterSizeTooBig(config.getPatientLimit(), attributionRoster)) {
            throw TOO_MANY_MEMBERS_EXCEPTION;
        }

        final String providerNPI = FHIRExtractors.getAttributedNPI(attributionRoster);

        // Check and see if a roster already exists for the provider
        final UUID organizationID = UUID.fromString(FHIRExtractors.getOrganizationID(attributionRoster));
        final List<RosterEntity> entities = this.rosterDAO.findEntities(null, organizationID, providerNPI, null);
        if (!entities.isEmpty()) {
            final RosterEntity rosterEntity = entities.get(0);
            return Response.status(Response.Status.OK).entity(this.converter.toFHIR(Group.class, rosterEntity)).build();
        }
        final List<ProviderEntity> providers = this.providerDAO.getProviders(null, providerNPI, organizationID);
        if (providers.isEmpty()) {
            throw new WebApplicationException("Unable to find attributable provider", Response.Status.NOT_FOUND);
        }

        verifyMembers(attributionRoster, organizationID);

        final RosterEntity rosterEntity = RosterEntity.fromFHIR(attributionRoster, providers.get(0), generateExpirationTime());

        // Add the first provider
        final RosterEntity persisted = this.rosterDAO.persistEntity(rosterEntity);
        final Group persistedGroup = this.converter.toFHIR(Group.class, persisted);

        return Response.status(Response.Status.CREATED).entity(persistedGroup).build();
    }

    @GET
    @FHIR
    @UnitOfWork
    @Override
    public List<Group> rosterSearch(@QueryParam(Group.SP_RES_ID) UUID rosterID,
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
    @UnitOfWork
    @Override
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
                .collect(Collectors.toList());
    }


    @PUT
    @Path("/{rosterID}")
    @FHIR
    @UnitOfWork
    @Override
    public Group replaceRoster(@PathParam("rosterID") UUID rosterID, Group groupUpdate) {
        if (!this.rosterDAO.rosterExists(rosterID)) {
            throw new WebApplicationException(NOT_FOUND_EXCEPTION, Response.Status.NOT_FOUND);
        }

        if (rosterSizeTooBig(config.getPatientLimit(), groupUpdate)) {
            throw TOO_MANY_MEMBERS_EXCEPTION;
        }
        final UUID organizationID = UUID.fromString(FHIRExtractors.getOrganizationID(groupUpdate));
        verifyMembers(groupUpdate, organizationID);

        final RosterEntity rosterEntity = new RosterEntity();
        rosterEntity.setId(rosterID);
        final OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        // Remove all roster relationships
        this.relationshipDAO.removeRosterAttributions(rosterID);

        groupUpdate
                .getMember()
                .stream()
                .map(Group.GroupMemberComponent::getEntity)
                .map(ref -> {
                    final PatientEntity pe = new PatientEntity();
                    pe.setID(UUID.fromString(new IdType(ref.getReference()).getIdPart()));
                    return pe;
                })
                .map(pe -> new AttributionRelationship(rosterEntity, pe))
                .peek(relationship -> relationship.setPeriodEnd(generateExpirationTime()))
                .peek(relationship -> relationship.setPeriodBegin(now))
                .forEach(relationshipDAO::addAttributionRelationship);

        final RosterEntity rosterEntity1 = rosterDAO.getEntity(rosterID)
                .orElseThrow(() -> NOT_FOUND_EXCEPTION);

        return converter.toFHIR(Group.class, rosterEntity1);
    }

    @POST
    @Path("/{rosterID}/$add")
    @FHIR
    @UnitOfWork
    @Override
    public Group addRosterMembers(@PathParam("rosterID") UUID rosterID, @FHIRParameter Group groupUpdate) {
        if (!this.rosterDAO.rosterExists(rosterID)) {
            throw new WebApplicationException(NOT_FOUND_EXCEPTION, Response.Status.NOT_FOUND);
        }

        final RosterEntity rosterEntity = this.rosterDAO.getEntity(rosterID)
                .orElseThrow(() -> NOT_FOUND_EXCEPTION);


        if (rosterSizeTooBig(config.getPatientLimit(), converter.toFHIR(Group.class, rosterEntity), groupUpdate)) {
            throw TOO_MANY_MEMBERS_EXCEPTION;
        }

        final UUID orgId = UUID.fromString(FHIRExtractors.getOrganizationID(groupUpdate));

        final OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        // For each group member, check to see if the patient exists, if not, throw an exception
        // Check to see if they're already rostered, if so, ignore
        groupUpdate
                .getMember()
                .stream()
                .map(Group.GroupMemberComponent::getEntity)
                // Check to see if patient exists, if not, throw an exception
                .map(entity -> {
                    final UUID patientID = UUID.fromString(new IdType(entity.getReference()).getIdPart());
                    List<PatientEntity> patientEntities = this.patientDAO.patientSearch(patientID, null, orgId);
                    if (patientEntities == null || patientEntities.isEmpty()) {
                        throw new WebApplicationException(String.format("Cannot find patient with ID %s", patientID.toString()), Response.Status.BAD_REQUEST);
                    }
                    return patientEntities.get(0);
                })
                .map(patient -> {
                    // Check to see if the attribution already exists, if so, re-extend the expiration time
                    final AttributionRelationship relationship = this.relationshipDAO.lookupAttributionRelationship(rosterID, patient.getID())
                            .orElse(new AttributionRelationship(rosterEntity, patient, now));
                    // If the relationship is inactive, then we need to update the period begin for the new membership span
                    if (relationship.isInactive()) {
                        relationship.setPeriodBegin(now);
                    }
                    relationship.setInactive(false);
                    relationship.setPeriodEnd(generateExpirationTime());
                    return relationship;
                })
                .forEach(this.relationshipDAO::addAttributionRelationship);

        //Getting it again to access the latest updates from above code
        final RosterEntity rosterEntity1 = this.rosterDAO.getEntity(rosterID)
                .orElseThrow(() -> NOT_FOUND_EXCEPTION);

        return converter.toFHIR(Group.class, rosterEntity1);
    }

    @POST
    @Path("/{rosterID}/$remove")
    @FHIR
    @UnitOfWork
    @Override
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
    @UnitOfWork
    @Override
    public Response deleteRoster(@PathParam("rosterID") UUID rosterID) {
        final RosterEntity rosterEntity = this.rosterDAO.getEntity(rosterID)
                .orElseThrow(() -> NOT_FOUND_EXCEPTION);

        this.rosterDAO.delete(rosterEntity);
        return Response.ok().build();
    }


    @Path("/{rosterID}")
    @GET
    @UnitOfWork
    @Timed
    @ExceptionMetered
    @Override
    public Group getRoster(
            @PathParam("rosterID") UUID rosterID) {
        logger.debug("API request to retrieve attributed patients for {}", rosterID);

        final RosterEntity rosterEntity = this.rosterDAO.getEntity(rosterID)
                .orElseThrow(() -> NOT_FOUND_EXCEPTION);

        return converter.toFHIR(Group.class, rosterEntity);
    }

    private OffsetDateTime generateExpirationTime() {
        return OffsetDateTime.now(ZoneOffset.UTC).plus(config.getExpirationThreshold());
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

    private void verifyMembers(Group group, UUID orgId) {
        for (Group.GroupMemberComponent member : group.getMember()) {
            final UUID patientID = UUID.fromString(new IdType(member.getEntity().getReference()).getIdPart());
            List<PatientEntity> patientEntities = patientDAO.patientSearch(patientID, null, orgId);
            if (patientEntities.isEmpty()) {
                throw new WebApplicationException(String.format("Cannot find patient with ID %s", patientID.toString()), Response.Status.BAD_REQUEST);
            }
        }
    }
}
