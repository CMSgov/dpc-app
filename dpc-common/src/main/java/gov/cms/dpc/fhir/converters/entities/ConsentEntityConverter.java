package gov.cms.dpc.fhir.converters.entities;

import gov.cms.dpc.common.consent.entities.ConsentEntity;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import gov.cms.dpc.fhir.FHIRExtractors;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.http.HttpStatus;
import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.utilities.xhtml.NodeType;
import org.hl7.fhir.utilities.xhtml.XhtmlNode;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static gov.cms.dpc.common.consent.entities.ConsentEntity.*;

/**
 * A utility class for converting a ConsentEntity into a FHIR Consent Resource.
 */
public class ConsentEntityConverter {
    /*
     * "magic string" uris for a PolicyRule, as defined
     * <a href="http://hl7.org/fhir/STU3/consent.html#6.2.5">by Hl7</a>
     */
    public static final String OPT_IN_MAGIC = "http://hl7.org/fhir/ConsentPolicy/opt-in";
    public static final String OPT_OUT_MAGIC = "http://hl7.org/fhir/ConsentPolicy/opt-out";
    public static final String SYSTEM_LOINC = "http://loinc.org";

    private ConsentEntityConverter() {}

    private static String patientIdentifier(DPCIdentifierSystem type, String value) {
        return String.format("%s|%s ", type.getSystem(), value);
    }

    private static Narrative narrativeText(String inOrOut, String hicn, String mbi) {
        boolean noHicn = hicn == null || hicn.isEmpty();
        boolean noMbi = mbi == null || mbi.isEmpty();

        StringBuilder sb = new StringBuilder("Consent status of ");
        sb.append(inOrOut);
        sb.append(" for the patient with identifiers [");
        sb.append(noHicn ? "" : patientIdentifier(DPCIdentifierSystem.HICN, hicn));
        sb.append((!noHicn && !noMbi) ? "], [" : "");
        sb.append(noMbi ? "" : patientIdentifier(DPCIdentifierSystem.MBI, mbi));
        sb.append("]");

        Narrative text = new Narrative();
        text.setDiv(new XhtmlNode(NodeType.Text).setValue(sb.toString()));
        text.setStatus(Narrative.NarrativeStatus.GENERATED);
        return text;
    }

    private static Reference patient(String fhirURL, String mbi) {
        if (mbi == null || mbi.isBlank()) {
            throw new IllegalArgumentException("ConsentEntity must have an MBI value");
        }

        return new Reference().setReference(String.format("%s/Patient?identity=|%s", fhirURL, mbi));
    }


    private static String policyRule(String value) {
        String code;
        if (OPT_IN.equals(value)) {
            code = OPT_IN_MAGIC;
        } else if (OPT_OUT.equals(value)) {
            code = OPT_OUT_MAGIC;
        } else {
            throw new IllegalArgumentException(String.format("invalid value %s; should be %s or %s", value, OPT_IN, OPT_OUT));
        }

        return code;
    }

    private static String policyUriToCode(String uri) {
        if (OPT_IN_MAGIC.equals(uri)) {
            return OPT_IN;
        } else if (OPT_OUT_MAGIC.equals(uri)) {
            return OPT_OUT;
        }
        throw new WebApplicationException(String.format("Policy rule must be %s or %s.", OPT_IN_MAGIC, OPT_OUT_MAGIC), Response.Status.BAD_REQUEST);
    }

    private static List<CodeableConcept> category(String loincCode) {
        // there must code to look up the code systems used in these CodeableConcept values. What is it?
        CodeableConcept category = new CodeableConcept();
        category.addCoding().setSystem(SYSTEM_LOINC).setCode(loincCode).setDisplay(ConsentEntity.CATEGORY_DISPLAY);
        return List.of(category);
    }

    private static String categoriesToLoincCode(List<CodeableConcept> categories) {
        if (categories == null || categories.size() != 1) {
            throw new WebApplicationException("Must include one category", HttpStatus.UNPROCESSABLE_ENTITY_422);
        }

        CodeableConcept category = categories.get(0);
        List<Coding> codings = category.getCoding();
        if (codings == null || codings.size() != 1) {
            throw new WebApplicationException("Category must have one coding", HttpStatus.UNPROCESSABLE_ENTITY_422);
        }

        Coding coding = category.getCodingFirstRep();
        if (!SYSTEM_LOINC.equals(coding.getSystem()) || !CATEGORY_LOINC_CODE.equals(coding.getCode())) {
            throw new WebApplicationException(String.format("Category coding must have system %s and code %s", SYSTEM_LOINC, CATEGORY_LOINC_CODE), HttpStatus.UNPROCESSABLE_ENTITY_422);
        }

        return coding.getCode();
    }

    private static String mbiFromPatientReference(String patientRefStr) {
        String mbi = "";
        Pattern patientIdPattern = Pattern.compile("/Patient\\?identity=\\|(?<mbi>\\d[a-zA-Z][a-zA-Z0-9]\\d[a-zA-Z][a-zA-Z0-9]\\d[a-zA-Z]{2}\\d{2})");
        Matcher matcher = patientIdPattern.matcher(patientRefStr);
        if (matcher.find()) {
            mbi = matcher.group("mbi");
        }
        return mbi;
    }

    @SuppressWarnings("JdkObsolete") // Date class is used by FHIR stu3 Consent model
    public static ConsentEntity fromFhir(Consent consent) {
        if (consent == null) {
            throw new WebApplicationException("No consent resource provided", Response.Status.BAD_REQUEST);
        }

        ConsentEntity entity = new ConsentEntity();

        String consentId = consent.getId();
        if (!StringUtils.isBlank(consentId)) {
            entity.setId(FHIRExtractors.getEntityUUID(consentId));
        }

        if (!Consent.ConsentState.ACTIVE.equals(consent.getStatus())) {
            throw new WebApplicationException("Only active consent records are accepted", HttpStatus.UNPROCESSABLE_ENTITY_422);
        }

        entity.setLoincCode(categoriesToLoincCode(consent.getCategory()));

        Reference patientRef = consent.getPatient();
        if (patientRef == null || StringUtils.isBlank(patientRef.getReference())) {
            throw new WebApplicationException("Consent resource must contain patient reference", Response.Status.BAD_REQUEST);
        }
        String mbi = mbiFromPatientReference(patientRef.getReference());
        if (StringUtils.isBlank(mbi)) {
            throw new WebApplicationException("Could not find MBI in patient reference", Response.Status.BAD_REQUEST);
        }
        entity.setMbi(mbi);

        Date dateTime = consent.getDateTime();
        LocalDate date = dateTime != null ? dateTime.toInstant().atOffset(ZoneOffset.UTC).toLocalDate() : LocalDate.now(ZoneOffset.UTC);
        entity.setEffectiveDate(date);
        entity.setPolicyCode(policyUriToCode(consent.getPolicyRule()));

        return entity;
    }

    @SuppressWarnings("JdkObsolete") // Date class is used by FHIR stu3 Consent model
    public static Consent toFhir(ConsentEntity consentEntity, String fhirURL) {
        Consent c = new Consent();

        c.setId(consentEntity.getId().toString());

        // there is no consent status in entity, so we are defaulting to active.
        c.setStatus(Consent.ConsentState.ACTIVE);

        c.setDateTime(Date.from(consentEntity.getEffectiveDate().atStartOfDay().atOffset(ZoneOffset.UTC).toInstant()));
        c.setText(narrativeText(consentEntity.getPolicyCode(), consentEntity.getHicn(), consentEntity.getMbi()));
        c.setPatient(patient(fhirURL, consentEntity.getMbi()));

        // PolicyRule is a CodeableConcept in r4 but is a uri in r3
        c.setPolicyRule(policyRule(consentEntity.getPolicyCode()));

        // scope is an r4 entity. in our data this is currently always "patient-privacy"
        // for the moment, we're not extending r3 to include it

        c.setCategory(category(consentEntity.getLoincCode()));

        return c;
    }
}
