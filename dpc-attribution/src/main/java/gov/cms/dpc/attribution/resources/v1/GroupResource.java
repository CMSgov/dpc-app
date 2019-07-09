package gov.cms.dpc.attribution.resources.v1;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import gov.cms.dpc.attribution.resources.AbstractGroupResource;
import gov.cms.dpc.common.interfaces.AttributionEngine;
import gov.cms.dpc.fhir.FHIRBuilders;
import gov.cms.dpc.fhir.annotations.FHIR;
import org.eclipse.jetty.http.HttpStatus;
import org.hl7.fhir.dstu3.model.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Optional;

public class GroupResource extends AbstractGroupResource {

    private static final Logger logger = LoggerFactory.getLogger(GroupResource.class);

    private final AttributionEngine engine;

    @Inject
    GroupResource(AttributionEngine engine) {
        this.engine = engine;
    }

    @POST
    @FHIR
    @Override
    @Timed
    @ExceptionMetered
    public Response submitRoster(Bundle providerBundle) {
        logger.debug("API request to submit roster");
        this.engine.addAttributionRelationships(providerBundle);

        return Response.ok().build();
    }


    @Path("/{groupID}")
    @GET
    @Override
    @Timed
    @ExceptionMetered
    public List<String> getAttributedPatients(@PathParam("groupID") String groupID) {
        logger.debug("API request to retrieve attributed patients for {}", groupID);

        Optional<List<String>> attributedBeneficiaries;
        try {
            // Create a practitioner resource for retrieval
            attributedBeneficiaries = engine.getAttributedPatientIDs(FHIRBuilders.buildPractitionerFromNPI(groupID));
        } catch (Exception e) {
            logger.error("Cannot get attributed patients for {}", groupID, e);
            throw new WebApplicationException(String.format("Unable to retrieve attributed patients for: %s", groupID), Response.Status.INTERNAL_SERVER_ERROR);
        }

        if (attributedBeneficiaries.isEmpty()) {
            throw new WebApplicationException(String.format("Unable to find provider: %s", groupID), Response.Status.NOT_FOUND);
        }
        return attributedBeneficiaries.get();
    }

    @Path("/{groupID}/{patientID}")
    @GET
    @Override
    @Timed
    @ExceptionMetered
    public boolean isAttributed(@PathParam("groupID") String groupID, @PathParam("patientID") String patientID) {
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
    public void attributePatient(@PathParam("groupID") String groupID, @PathParam("patientID") String patientID) {
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
    public void removeAttribution(@PathParam("groupID") String groupID, @PathParam("patientID") String patientID) {
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
