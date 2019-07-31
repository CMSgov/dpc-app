package gov.cms.dpc.attribution.resources.v1;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import gov.cms.dpc.attribution.jdbi.ProviderDAO;
import gov.cms.dpc.attribution.jdbi.RosterDAO;
import gov.cms.dpc.attribution.resources.AbstractGroupResource;
import gov.cms.dpc.common.entities.ProviderEntity;
import gov.cms.dpc.common.entities.RosterEntity;
import gov.cms.dpc.common.interfaces.AttributionEngine;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import gov.cms.dpc.fhir.FHIRBuilders;
import gov.cms.dpc.fhir.FHIRExtractors;
import gov.cms.dpc.fhir.annotations.FHIR;
import io.dropwizard.hibernate.UnitOfWork;
import io.swagger.annotations.*;
import org.eclipse.jetty.http.HttpStatus;
import org.hibernate.validator.constraints.NotEmpty;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Group;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.UUID;

@Api(value = "Group")
public class GroupResource extends AbstractGroupResource {

    private static final Logger logger = LoggerFactory.getLogger(GroupResource.class);

    private final AttributionEngine engine;
    private final ProviderDAO providerDAO;
    private final RosterDAO rosterDAO;

    @Inject
    GroupResource(AttributionEngine engine, ProviderDAO providerDAO, RosterDAO rosterDAO) {
        this.engine = engine;
        this.rosterDAO = rosterDAO;
        this.providerDAO = providerDAO;
    }

    @POST
    @FHIR
    @UnitOfWork
    @Override
    public Response createRoster(Group attributionRoster) {
        // Lookup the Provider by NPI

        final String providerNPI = attributionRoster
                .getCharacteristic()
                .stream()
                .filter(concept -> concept.getCode().getText().equals("attributed-to"))
                .map(concept -> ((CodeableConcept) concept.getValue()).getText())
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Roster MUST have attributed Provider"));

        final List<ProviderEntity> providers = this.providerDAO.getProviders(null, providerNPI, UUID.fromString(FHIRExtractors.getOrganizationID(attributionRoster)));
        if (providers.isEmpty()) {
            throw new WebApplicationException("Unable to finding attributable provider", Response.Status.NOT_FOUND);
        }

        final RosterEntity rosterEntity = RosterEntity.fromFHIR(attributionRoster, providers.get(0));
        // Add the first provider
        final RosterEntity persisted = this.rosterDAO.persistEntity(rosterEntity);
        final Group persistedGroup = persisted.toFHIR();

        return Response.status(Response.Status.CREATED).entity(persistedGroup).build();
    }

    @Override
    public Bundle rosterSearch(@NotEmpty @QueryParam("_tag") UUID organizationID, @QueryParam("npi") String providerNPI, @QueryParam("member") String patientID) {
        return null;
    }

    @POST
    @Path("/$submit")
    @FHIR
    @Override
    @Timed
    @ExceptionMetered
    @ApiOperation(value = "Submit Attribution Bundle", notes = "FHIR endpoint that accepts a Bundle resource, corresponding to an Attribution set.")
    @ApiResponses(
            @ApiResponse(code = 201, message = "Attribution Bundle was successfully added")
    )
    public Response submitRoster(Bundle providerBundle) {
        logger.debug("API request to submit roster");
        // FIXME(nickrobison): Remove this! This is really gross, but will come out as part of our Group refactoring
        final UUID organizationID = providerBundle.getEntryFirstRep()
                .getResource()
                .getMeta()
                .getTag()
                .stream()
                .filter(tag -> tag.getSystem().equals(DPCIdentifierSystem.DPC.getSystem()))
                .map(Coding::getCode)
                .map(UUID::fromString)
                .findFirst()
                .orElseThrow(() -> new WebApplicationException("Must have Metadata identifier", Response.Status.INTERNAL_SERVER_ERROR));

        this.engine.addAttributionRelationships(providerBundle, organizationID);

        return Response.status(Response.Status.CREATED).build();
    }


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
                .orElseThrow(() -> new WebApplicationException("Cannot find Roster", Response.Status.NOT_FOUND))
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

    @Path("/{groupID}/{patientID}")
    @GET
    @Override
    @Timed
    @ExceptionMetered
    @ApiOperation(value = "Verify attribution relationship", notes = "Returns whether or not the Patient (identified by MBI) is attributed to the given Provider (identified by NPI)")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Patient is attributed to the given provider"),
            @ApiResponse(code = 406, message = "Patient is not attributed to the given provider")
    })
    public boolean isAttributed(@ApiParam(value = "Provider NPI", required = true)
                                @PathParam("groupID") String groupID,
                                @ApiParam(value = "Patient MBI", required = true)
                                @PathParam("patientID") String patientID) {
        logger.debug("API request to determine attribution between {} and {}", groupID, patientID);
        final boolean attributed = engine.isAttributed(
                FHIRBuilders.buildPractitionerFromNPI(groupID),
                FHIRBuilders.buildPatientFromMBI(patientID));
        if (!attributed) {
            throw new WebApplicationException(HttpStatus.NOT_ACCEPTABLE_406);
        }
        return true;
    }

    @Path("/{groupID}/{patientID}")
    @PUT
    @Override
    @Timed
    @ExceptionMetered
    @ApiOperation(value = "Attribute patient to provider", notes = "Method to attributed a patient (identified by MBI) to a given provider (identified by NPI)")
    @ApiResponses(value = @ApiResponse(code = 500, message = "Service was unable to attribute patient to provider"))
    public void attributePatient(@ApiParam(value = "Provider NPI", required = true)
                                 @PathParam("groupID") String groupID,
                                 @ApiParam(value = "Patient MBI", required = true)
                                 @PathParam("patientID") String patientID) {
        logger.debug("API request to add attribution between {} and {}", groupID, patientID);
        try {
            this.engine.addAttributionRelationship(
                    FHIRBuilders.buildPractitionerFromNPI(groupID),
                    FHIRBuilders.buildPatientFromMBI(patientID));
        } catch (Exception e) {
            logger.error("Error attributing patient", e);
            throw new WebApplicationException("Cannot attribute patients", HttpStatus.INTERNAL_SERVER_ERROR_500);
        }
    }

    @Path("/{groupID}/{patientID}")
    @Override
    @DELETE
    @Timed
    @ExceptionMetered
    @ApiOperation(value = "Remove patient attribution", notes = "Method to remove an attributed patient (identified by MBI) from a given provider (identified by NPI)")
    @ApiResponses(value = @ApiResponse(code = 500, message = "Service was unable to remove attribution between patient and provider"))
    public void removeAttribution(@ApiParam(value = "Provider NPI", required = true)
                                  @PathParam("groupID") String groupID,
                                  @ApiParam(value = "Patient MBI", required = true)
                                  @PathParam("patientID") String patientID) {
        logger.debug("API request to remove attribution between {} and {}", groupID, patientID);
        try {
            this.engine.removeAttributionRelationship(
                    FHIRBuilders.buildPractitionerFromNPI(groupID),
                    FHIRBuilders.buildPatientFromMBI(patientID));
        } catch (Exception e) {
            logger.error("Error removing patient", e);
            throw new WebApplicationException("Cannot remove attributed patients", HttpStatus.INTERNAL_SERVER_ERROR_500);
        }
    }
}
