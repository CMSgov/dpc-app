package gov.cms.dpc.attribution.resources.v1;

import gov.cms.dpc.attribution.resources.AbstractGroupResource;
import gov.cms.dpc.common.interfaces.AttributionEngine;
import gov.cms.dpc.fhir.annotations.FHIR;
import io.dropwizard.hibernate.UnitOfWork;
import org.eclipse.jetty.http.HttpStatus;
import org.hl7.fhir.dstu3.model.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.Optional;
import java.util.Set;

public class GroupResource extends AbstractGroupResource {

    private static final Logger logger = LoggerFactory.getLogger(GroupResource.class);

    private final AttributionEngine engine;

    @Inject
    GroupResource(AttributionEngine engine) {
        this.engine = engine;
    }

    @POST
    @FHIR
    @UnitOfWork
    public Response submitRoster(Bundle providerBundle) {
        this.engine.addAttributionRelationships(providerBundle);

        return Response.ok().build();
    }

    @Path("/{groupID}")
    @GET
    @Override
    @UnitOfWork
    public Set<String> getAttributedPatients(@PathParam("groupID") String groupID) {
        final Optional<Set<String>> attributedBeneficiaries = engine.getAttributedBeneficiaries(groupID);
        if (attributedBeneficiaries.isEmpty()) {
            throw new WebApplicationException(String.format("Unable to find provider: %s", groupID), Response.Status.NOT_FOUND);
        }

        return attributedBeneficiaries.get();
    }

    @Path("/{groupID}/{patientID}")
    @GET
    @UnitOfWork
    @Override
    public boolean isAttributed(@PathParam("groupID") String groupID, @PathParam("patientID") String patientID) {
        final boolean attributed = engine.isAttributed(groupID, patientID);
        if (!attributed) {
            throw new WebApplicationException(HttpStatus.NOT_ACCEPTABLE_406);
        }
        return true;
    }

    @Path("/{groupID}/{patientID}")
    @PUT
    @UnitOfWork
    @Override
    public void attributePatient(@PathParam("groupID") String groupID, @PathParam("patientID") String patientID) {
        try {
            this.engine.addAttributionRelationship(groupID, patientID);
        } catch (Exception e) {
            logger.error("Error attributing patient", e);
            throw new WebApplicationException("Cannot attribute patients", HttpStatus.INTERNAL_SERVER_ERROR_500);
        }
    }

    @Path("/{groupID}/{patientID}")
    @UnitOfWork
    @Override
    @DELETE
    public void removeAttribution(@PathParam("groupID") String groupID, @PathParam("patientID") String patientID) {
        try {
            this.engine.removeAttributionRelationship(groupID, patientID);
        } catch (Exception e) {
            logger.error("Error removing patient", e);
            throw new WebApplicationException("Cannot remove patient attribution patients", HttpStatus.INTERNAL_SERVER_ERROR_500);
        }
    }
}
