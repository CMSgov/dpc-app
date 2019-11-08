package gov.cms.dpc.consent.resources;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Metered;
import com.codahale.metrics.annotation.Timed;
import gov.cms.dpc.common.annotations.APIV1;
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
import org.eclipse.jetty.http.HttpStatus;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Consent;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.utilities.xhtml.NodeType;
import org.hl7.fhir.utilities.xhtml.XhtmlNode;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Path("v1/Consent")
public class ConsentResource {

    private final ConsentDAO dao;

    @Inject
    ConsentResource(ConsentDAO dao) {
        this.dao = dao;
    }

    @GET
    @FHIR
    @Timed
    @ExceptionMetered
    @UnitOfWork
    @ApiOperation(value = "Search for Consent Entries", notes = "Search for Consent records. " +
            "<p>Must provide ONE OF Consent ID as an _id or identifier, or a patient MBI or HICN to search for.")
    @ApiResponses(@ApiResponse(code = 400, message = "Must provide Consent or Patient id"))
    public Bundle search(
            @ApiParam(value = "Consent resource _id") @QueryParam(Consent.SP_RES_ID) Optional<UUID> id,
            @ApiParam(value = "Consent resource identifier") @QueryParam(Consent.SP_IDENTIFIER) Optional<UUID> identifier,
            @ApiParam(value = "Patient Identifier") @QueryParam(Consent.SP_PATIENT) Optional<String> patientId ) {

        List<ConsentEntity> entities = null;

        // Priority order for processing params. If multiple params are passed, we only pay attention to one
        if (id.isPresent()) {

            final Optional<ConsentEntity> consentEntity = this.dao.getConsent(id.get());
            entities = consentEntity.map(List::of).orElseGet(() -> List.of(ConsentEntity.defaultConsentEntity(id, Optional.empty(), Optional.empty())));

        } else if (identifier.isPresent()) {
            // not sure we should support this
            final Optional<ConsentEntity> consentEntity = this.dao.getConsent(identifier.get());
            entities = consentEntity.map(List::of).orElseGet(() -> List.of(ConsentEntity.defaultConsentEntity(id, Optional.empty(), Optional.empty())));

        } else if (patientId.isPresent()) {

            final Identifier patientIdentifier = FHIRExtractors.parseIDFromQueryParam(patientId.get());
            entities = getEntitiesByPatient(patientIdentifier);

        } else {

            throw new WebApplicationException("Must have some form of Consent Resource ID or Patient ID", Response.Status.BAD_REQUEST);
        }

        return bundleOf(entities);
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

        return ConsentEntityConverter.convert(consentEntity);
    }

    private List<ConsentEntity> getEntitiesByPatient(Identifier patientIdentifier) {
        List<ConsentEntity> entities;
        Optional<String> hicnValue = Optional.empty();
        Optional<String> mbiValue  = Optional.empty();
        String field;

        // we have been asked to search for a patient id defined by one among two (soon three!) coding systems
        // we need to determine which database field that system's value is stored in
        switch (DPCIdentifierSystem.fromString(patientIdentifier.getSystem())) {
            case MBI:
                mbiValue = Optional.of(patientIdentifier.getValue());
                field = "mbi";
                break;
            case HICN:
                hicnValue = Optional.of(patientIdentifier.getValue());
                field = "hicn";
                break;
            default:
                throw new WebApplicationException("Unknown Patient ID code system", Response.Status.BAD_REQUEST);
        }

        entities = this.dao.findBy(field, patientIdentifier.getValue());

        if (entities.isEmpty()) {
            entities = List.of(ConsentEntity.defaultConsentEntity(Optional.empty(), hicnValue, mbiValue));
        }
        return entities;
    }

    private Bundle bundleOf(List<ConsentEntity> consentEntities) {

        if (consentEntities.isEmpty()) {
            throw new WebApplicationException("Cannot find patient with given ID", Response.Status.NOT_FOUND);
        }

        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.SEARCHSET);
        consentEntities.forEach(e -> bundle.addEntry().setResource(ConsentEntityConverter.convert(e)));
        bundle.setTotal(bundle.getEntry().size());

        return bundle;
    }
}
