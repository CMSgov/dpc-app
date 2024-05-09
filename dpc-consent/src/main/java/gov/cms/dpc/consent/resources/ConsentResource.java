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
import org.eclipse.jetty.http.HttpStatus;
import org.hl7.fhir.dstu3.model.Consent;
import org.hl7.fhir.dstu3.model.Identifier;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

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
    public Response create(Consent consent) {
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
    public List<Consent> search(
            @QueryParam(Consent.SP_RES_ID) Optional<UUID> id,
            @QueryParam(Consent.SP_IDENTIFIER) Optional<UUID> identifier,
            @QueryParam(Consent.SP_PATIENT) Optional<String> patientId) {

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
                .collect(Collectors.toList());
    }

    @GET
    @Path("/{consentId}")
    @FHIR
    @Timed
    @ExceptionMetered
    @UnitOfWork
    public Consent getConsent(@PathParam("consentId") UUID consentId) {
        final ConsentEntity consentEntity = this.dao.getConsent(consentId).orElseThrow(() ->
                new WebApplicationException("invalid consent resource id value", HttpStatus.NOT_FOUND_404)
        );

        return ConsentEntityConverter.toFhir(consentEntity, fhirReferenceURL);
    }

    @PUT
    @Path("/{consentId}")
    @FHIR
    @UnitOfWork
    public Consent update(@PathParam("consentId") UUID consentId, Consent consent) {
        consent.setId(consentId.toString());
        ConsentEntity entity = ConsentEntityConverter.fromFhir(consent);
        entity = this.dao.persistConsent(entity);
        return ConsentEntityConverter.toFhir(entity, fhirReferenceURL);
    }

    private List<ConsentEntity> getEntitiesByPatient(Identifier patientIdentifier) {
        String field;

        // we have been asked to search for a patient id defined by one among two (soon three!) coding systems
        // we need to determine which database field that system's value is stored in
        switch (DPCIdentifierSystem.fromString(patientIdentifier.getSystem())) {
            case MBI:
                field = "mbi";
                break;
            case HICN:
                field = "hicn";
                break;
            default:
                throw new WebApplicationException("Unknown Patient ID code system", Response.Status.BAD_REQUEST);
        }

        return this.dao.findBy(field, patientIdentifier.getValue());
    }
}
