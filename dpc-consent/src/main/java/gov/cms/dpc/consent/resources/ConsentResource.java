package gov.cms.dpc.consent.resources;

import gov.cms.dpc.common.consent.entities.ConsentEntity;
import gov.cms.dpc.consent.jdbi.ConsentDAO;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import gov.cms.dpc.fhir.FHIRExtractors;
import gov.cms.dpc.fhir.annotations.FHIR;
import io.dropwizard.hibernate.UnitOfWork;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.eclipse.jetty.http.HttpStatus;
import org.hl7.fhir.dstu3.model.*;
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

@Path("/")
public class ConsentResource {
    static final String LOINC_CATEGORY = "64292-6";
    static final String OPT_IN = "OPTIN";
    static final String OPT_OUT = "OPTOUT";
    static final String TREATMENT = "TREAT";
    static final String SCOPE_CODE = "patient-privacy";

    private final ConsentDAO dao;

    @Inject
    ConsentResource(ConsentDAO dao) {
        this.dao = dao;
    }

    @GET
    @Path("/Consent")
    @FHIR
    @UnitOfWork
    @ApiOperation(value = "Search for Consent Entries", notes = "Search for Consent records. " +
            "<p>Must provide ONE OF Consent ID as an _id or identifier, or a patient MBI or HICN to search for.")
    @ApiResponses(@ApiResponse(code = 400, message = "Must provide Consent or Patient id"))
    public Bundle search(
            @ApiParam(value = "Consent resource _id") @QueryParam("_id") Optional<UUID> id,
            @ApiParam(value = "Consent resource identifier") @QueryParam("identifier") Optional<UUID> identifier,
            @ApiParam(value = "Patient Identifier") @QueryParam("patient") Optional<String> patientId ) {

        if (id.isEmpty() && identifier.isEmpty() && patientId.isEmpty()) {
            throw new WebApplicationException("Must have some form of Consent Resource ID or Patient Resource ID", Response.Status.BAD_REQUEST);
        }

        List<ConsentEntity> entities = null;

        // Priority order for processing params. If multiple params are passed, we only pay attention to one
        if (id.isPresent()) {

            final Optional<ConsentEntity> consentEntity = this.dao.getConsent(id.get());
            entities = consentEntity.map(List::of).orElseGet(() -> List.of(defaultConsentEntity(id, Optional.empty(), Optional.empty())));

        } else if (identifier.isPresent()) {
            // not sure we should support this
            final Optional<ConsentEntity> consentEntity = this.dao.getConsent(identifier.get());
            entities = consentEntity.map(List::of).orElseGet(() -> List.of(defaultConsentEntity(id, Optional.empty(), Optional.empty())));

        } else if (patientId.isPresent()) {
            final Identifier patientIdentifier = FHIRExtractors.parseIDFromQueryParam(patientId.get());
            entities = getEntitiesByPatient(patientIdentifier);
        }

        return bundleFor(entities);
    }

    @GET
    @Path("/Consent/{consentId}")
    @FHIR
    @UnitOfWork
    @ApiOperation(value = "Locate a Consent entry by id")
    @ApiResponses(@ApiResponse(code = 400, message = "invalid id value. Must have a consent resource id"))
    public Consent getConsent(@ApiParam(value = "Consent resource ID", required = true) @PathParam("consentId") UUID consentId) {

        final Optional<ConsentEntity> consentEntity = this.dao.getConsent(consentId);

        if (consentEntity.isEmpty()) {
            throw new WebApplicationException("invalid consent resource id value", HttpStatus.NOT_FOUND_404);
        }

        return convert(consentEntity.get());
    }

    private List<ConsentEntity> getEntitiesByPatient(Identifier patientIdentifier) {
        List<ConsentEntity> entities;
        Optional<String> hicn = Optional.empty();
        Optional<String> mbi  = Optional.empty();
        String field;

        switch (DPCIdentifierSystem.fromString(patientIdentifier.getSystem())) {
            case MBI:
                mbi = Optional.of(patientIdentifier.getValue());
                field = "mbi";
                break;
            case HICN:
                hicn = Optional.of(patientIdentifier.getValue());
                field = "hicn";
                break;
            default:
                throw new WebApplicationException("Unknown Patient ID code system", Response.Status.BAD_REQUEST);
        }

        entities = this.dao.findBy(field, patientIdentifier.getValue());

        if (entities.isEmpty()) {
            entities = List.of(defaultConsentEntity(Optional.empty(), hicn, mbi));
        }
        return entities;
    }

    private Bundle bundleFor(List<ConsentEntity> consentEntities) {

        if (consentEntities.isEmpty()) {
            throw new WebApplicationException("Cannot find patient with given ID", javax.ws.rs.core.Response.Status.NOT_FOUND);
        }

        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.COLLECTION);
        consentEntities.forEach(e -> {
            bundle.addEntry().setResource(convert(e));
        });
        bundle.setTotal(bundle.getEntry().size());

        return bundle;
    }

    static String patientIdentifier(DPCIdentifierSystem type, String value) {
        return String.format("%s|%s ", type.getSystem(), value);
    }

    static Narrative narrativeText(String inOrOut, String hicn, String mbi) {
        StringBuilder sb = new StringBuilder("Words about the ");
        sb.append(inOrOut);
        sb.append(" status of the patient with identifiers ");
        sb.append(hicn == null || hicn.isEmpty() ? "" : patientIdentifier(DPCIdentifierSystem.HICN, hicn));
        sb.append(mbi == null || mbi.isEmpty() ? "" : patientIdentifier(DPCIdentifierSystem.MBI, mbi));

        Narrative text = new Narrative();
        text.setDiv(new XhtmlNode(NodeType.Text).setValue(sb.toString()));
        text.setStatus(Narrative.NarrativeStatus.GENERATED);
        return text;
    }

    static String policyCode(String value) {
        if (OPT_OUT.equals(value) || OPT_IN.equals(value)) {
            return value;
        }
        throw new IllegalArgumentException(String.format("invalid policyCode %s", value));
    }

    static List<CodeableConcept> category(String loincCode) {
        // there must be a way to look up the code systems used in these CodeableConcept values. what is it?
        CodeableConcept category = new CodeableConcept();
        category.addCoding().setSystem("http://loinc.org").setCode(loincCode);
        return List.of(category);
    }

    static Consent convert(ConsentEntity consentEntity) {
        Consent c = new Consent();

        c.setId(consentEntity.getId().toString());

        // there is no consent status in entity, so we are defaulting to active. Correct?
        c.setStatus(Consent.ConsentState.ACTIVE);
        c.setDateTime(Date.from(consentEntity.getEffectiveDate().atStartOfDay().atOffset(ZoneOffset.UTC).toInstant()));
        c.setText(narrativeText(consentEntity.getPolicyCode(), consentEntity.getHicn(), consentEntity.getMbi()));

        // hicn or mbi? both?
        c.setPatient(new Reference(new IdType("Patient", consentEntity.getMbi())));

        // PolicyRule is a CodeableConcept in r4 but is a string in r3
        c.setPolicyRule(policyCode(consentEntity.getPolicyCode()));

        // scope is an r4 entity. in our data this is currently always "patient-privacy"
        // hence, I don't think it's worth extending r3 to include it atm

        c.setCategory(category(consentEntity.getLoincCode()));

        return c;
    }

    static ConsentEntity defaultConsentEntity(Optional<UUID> id, Optional<String> hicn, Optional<String> mbi) {
        ConsentEntity ce = new ConsentEntity();

        ce.setId(UUID.randomUUID());
        id.ifPresent(ce::setId);

        ce.setCreatedAt(OffsetDateTime.now(ZoneId.of("UTC")));
        ce.setEffectiveDate(LocalDate.now(ZoneId.of("UTC")));
        ce.setUpdatedAt(OffsetDateTime.now(ZoneId.of("UTC")));

        hicn.ifPresent(ce::setHicn);
        mbi.ifPresent(ce::setMbi);

        ce.setLoincCode(LOINC_CATEGORY);
        ce.setPolicyCode(OPT_IN);
        ce.setPurposeCode(TREATMENT);
        ce.setScopeCode(SCOPE_CODE);

        return ce;
    }
}
