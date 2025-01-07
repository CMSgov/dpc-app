package gov.cms.dpc.consent.resources;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Splitter;
import com.google.inject.name.Named;
import gov.cms.dpc.common.consent.entities.ConsentEntity;
import gov.cms.dpc.consent.jdbi.ConsentDAO;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import gov.cms.dpc.fhir.FHIRExtractors;
import gov.cms.dpc.fhir.annotations.FHIR;
import gov.cms.dpc.fhir.converters.entities.ConsentEntityConverter;
import io.dropwizard.hibernate.UnitOfWork;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import org.eclipse.jetty.http.HttpStatus;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Consent;
import org.hl7.fhir.dstu3.model.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Path("v1/Consent")
public class ConsentResource {

    private final ConsentDAO dao;
    private final String fhirReferenceURL;

    @Inject
    ConsentResource(ConsentDAO dao, @Named("fhirReferenceURL") String fhirReferenceURL) {
        this.dao = dao;
        this.fhirReferenceURL = fhirReferenceURL;
    }

    @POST
    @FHIR
    @UnitOfWork
    @ApiOperation(value = "Create a Consent resource")
    @ApiResponses(value = { @ApiResponse(code = 201, message = "Consent resource was created"),
            @ApiResponse(code = 400, message = "Consent resource was not created due to bad request") })
    public Response create(@ApiParam(value = "Consent resource") Consent consent) {
        ConsentEntity entity = ConsentEntityConverter.fromFhir(consent);
        entity = dao.persistConsent(entity);
        Consent result = ConsentEntityConverter.toFhir(entity, fhirReferenceURL);
        return Response.status(Response.Status.CREATED).entity(result).build();
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    @GET
    @FHIR
    @Timed
    @ExceptionMetered
    @UnitOfWork
    @ApiOperation(value = "Search for Consent Entries", notes = "Search for Consent records. " +
            "<p>Must provide ONE OF Consent ID as an _id or identifier, or a patient MBI or HICN to search for.", response = Bundle.class)
    @ApiResponses(@ApiResponse(code = 400, message = "Must provide Consent or Patient id"))
    public List<Consent> search(
            @ApiParam(value = "Consent resource _id") @QueryParam(Consent.SP_RES_ID) Optional<UUID> id,
            @ApiParam(value = "Consent resource identifier") @QueryParam(Consent.SP_IDENTIFIER) Optional<UUID> identifier,
            @ApiParam(value = "Patient Identifier") @QueryParam(Consent.SP_PATIENT) Optional<String> patientId) {

        List<ConsentEntity> entities = new ArrayList<>();

        // Priority order for processing params. If multiple params are passed, we only pay attention to one
        if (id.isPresent()) {
            final Optional<ConsentEntity> consentEntity = this.dao.getConsent(id.get());
            entities = consentEntity.map(List::of).orElse(entities);

        } else if (identifier.isPresent()) {
            // not sure we should support this
            final Optional<ConsentEntity> consentEntity = this.dao.getConsent(identifier.get());
            entities = consentEntity.map(List::of).orElse(entities);

        } else if (patientId.isPresent()) {
            for (String pId : Splitter.on(',').split(patientId.get())) {
                final Identifier patientIdentifier = FHIRExtractors.parseIDFromQueryParam(pId);
                entities.addAll(getEntitiesByPatient(patientIdentifier));
            }

        } else {
            throw new WebApplicationException("Must have some form of Consent Resource ID or Patient ID", Response.Status.BAD_REQUEST);
        }

        return entities
                .stream()
                .map(e -> ConsentEntityConverter.toFhir(e, fhirReferenceURL))
                .toList();
    }

    @GET
    @Path("/{consentId}")
    @FHIR
    @Timed
    @ExceptionMetered
    @UnitOfWork
    @ApiOperation(value = "Locate a Consent entry by id")
    @ApiResponses(@ApiResponse(code = 400, message = "invalid id value. Must have a consent resource id"))
    public Consent getConsent(@ApiParam(value = "Consent resource ID", required = true) @PathParam("consentId") UUID consentId) {

        final ConsentEntity consentEntity = this.dao.getConsent(consentId).orElseThrow(() ->
                new WebApplicationException("invalid consent resource id value", HttpStatus.NOT_FOUND_404)
        );

        return ConsentEntityConverter.toFhir(consentEntity, fhirReferenceURL);
    }

    @PUT
    @Path("/{consentId}")
    @FHIR
    @UnitOfWork
    @ApiOperation(value = "Update a Consent resource")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Consent resource was updated"),
            @ApiResponse(code = 400, message = "Consent resource was not updated due to bad request") })
    public Consent update(@ApiParam(value = "Consent resource ID", required = true) @PathParam("consentId") UUID consentId,
                          @ApiParam(value = "Consent resource", required = true) Consent consent) {
        consent.setId(consentId.toString());
        ConsentEntity entity = ConsentEntityConverter.fromFhir(consent);
        entity = this.dao.persistConsent(entity);
        return ConsentEntityConverter.toFhir(entity, fhirReferenceURL);
    }

    private List<ConsentEntity> getEntitiesByPatient(Identifier patientIdentifier) {
        String field = switch (DPCIdentifierSystem.fromString(patientIdentifier.getSystem())) {
            case MBI -> "mbi";
            case HICN -> "hicn";
            default -> throw new WebApplicationException("Unknown Patient ID code system", Response.Status.BAD_REQUEST);
        };

        // we have been asked to search for a patient id defined by one among two (soon three!) coding systems
        // we need to determine which database field that system's value is stored in

        return this.dao.findBy(field, patientIdentifier.getValue());
    }
}
