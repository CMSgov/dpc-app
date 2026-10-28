package gov.cms.dpc.fhir.converters.entities.r4;

import gov.cms.dpc.common.consent.entities.ConsentEntity;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import gov.cms.dpc.fhir.r4.FHIRExtractors;
import gov.cms.dpc.fhir.converters.FHIRConverter;
import gov.cms.dpc.fhir.converters.FHIREntityConverter;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.http.HttpStatus;
import org.hl7.fhir.r4.model.*;
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
 * Converts between the shared ConsentEntity and R4 Consent resources.
 */
public class ConsentEntityConverter implements FHIRConverter<Consent, ConsentEntity> {
    public static final String SYSTEM_POLICY = "http://terminology.hl7.org/CodeSystem/v3-ActCode";
    public static final String SYSTEM_SCOPE = "http://terminology.hl7.org/CodeSystem/consentscope";
    public static final String SYSTEM_LOINC = "http://loinc.org";

    public ConsentEntityConverter() {
        // Required by ServiceLoader.
    }

    @Override
    public ConsentEntity fromFHIR(FHIREntityConverter converter, Consent resource) {
        return fromFHIR(resource);
    }

    /**
     * Registry conversions use a relative patient reference. Use toFhir(entity, fhirURL)
     * when the caller needs an absolute reference.
     */
    @Override
    public Consent toFHIR(FHIREntityConverter converter, ConsentEntity entity) {
        return toFHIR(entity, "");
    }

    @Override
    public Class<Consent> getFHIRResource() {
        return Consent.class;
    }

    @Override
    public Class<ConsentEntity> getJavaClass() {
        return ConsentEntity.class;
    }

    private static String patientIdentifier(DPCIdentifierSystem type, String value) {
        return String.format("%s|%s ", type.getSystem(), value);
    }

    private static Narrative narrativeText(String inOrOut, String hicn, String mbi) {
        boolean noHicn = hicn == null || hicn.isEmpty();
        boolean noMbi = mbi == null || mbi.isEmpty();

        String str = "Consent status of " + inOrOut +
                " for the patient with identifiers [" +
                (noHicn ? "" : patientIdentifier(DPCIdentifierSystem.HICN, hicn)) +
                ((!noHicn && !noMbi) ? "], [" : "") +
                (noMbi ? "" : patientIdentifier(DPCIdentifierSystem.MBI, mbi)) +
                "]";

        Narrative text = new Narrative();
        text.setDiv(new XhtmlNode(NodeType.Text).setValue(str));
        text.setStatus(Narrative.NarrativeStatus.GENERATED);
        return text;
    }

    private static Reference patient(String fhirURL, String mbi) {
        if (mbi == null || mbi.isBlank()) {
            throw new IllegalArgumentException("ConsentEntity must have an MBI value");
        }

        String prefix = fhirURL.isBlank() ? "" : fhirURL + "/";
        return new Reference().setReference(String.format("%sPatient?identity=|%s", prefix, mbi));
    }

    private static CodeableConcept policyRule(String value) {
        if (!OPT_IN.equals(value) && !OPT_OUT.equals(value)) {
            throw new IllegalArgumentException(String.format("Invalid value %s; should be %s or %s", value, OPT_IN, OPT_OUT));
        }
        return new CodeableConcept().addCoding(new Coding().setSystem(SYSTEM_POLICY).setCode(value));
    }

    private static String policyRuleToCode(CodeableConcept policy) {
        if (policy.getCoding().size() == 1) {
            Coding coding = policy.getCodingFirstRep();
            if (SYSTEM_POLICY.equals(coding.getSystem()) &&
                    (OPT_IN.equals(coding.getCode()) || OPT_OUT.equals(coding.getCode()))) {
                return coding.getCode();
            }
        }
        throw new WebApplicationException(String.format("Policy rule must have coding with system %s and code %s or %s.",
                SYSTEM_POLICY, OPT_IN, OPT_OUT), Response.Status.BAD_REQUEST);
    }

    private static CodeableConcept scope(String value) {
        return new CodeableConcept().addCoding(new Coding().setSystem(SYSTEM_SCOPE).setCode(value));
    }

    private static String scopeToCode(CodeableConcept scope) {
        if (scope.getCoding().size() == 1) {
            Coding coding = scope.getCodingFirstRep();
            if (SYSTEM_SCOPE.equals(coding.getSystem()) && SCOPE_CODE.equals(coding.getCode())) {
                return coding.getCode();
            }
        }
        throw new WebApplicationException(String.format("Scope must have one coding with system %s and code %s.",
                SYSTEM_SCOPE, SCOPE_CODE), HttpStatus.UNPROCESSABLE_ENTITY_422);
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

    // lookahead is not supported by RE2/J, so we have to use the Java regex classes
    // see https://github.com/google/re2/wiki/Syntax
    private static String mbiFromPatientReference(String patientRefStr) {
        String mbi = "";
        Pattern patientIdPattern = Pattern.compile("(?:^|/)Patient\\?identity=\\|(?<mbi>\\d[a-zA-Z][a-zA-Z0-9]\\d[a-zA-Z][a-zA-Z0-9]\\d[a-zA-Z]{2}\\d{2})");
        Matcher matcher = patientIdPattern.matcher(patientRefStr);
        if (matcher.find()) {
            mbi = matcher.group("mbi");
        }
        return mbi;
    }

    @SuppressWarnings("JdkObsolete") // Date class is used by FHIR R4 Consent model
    public static ConsentEntity fromFHIR(Consent consent) {
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
        entity.setPolicyCode(policyRuleToCode(consent.getPolicyRule()));
        entity.setScopeCode(scopeToCode(consent.getScope()));

        return entity;
    }

    @SuppressWarnings("JdkObsolete") // Date class is used by FHIR R4 Consent model
    public static Consent toFHIR(ConsentEntity consentEntity, String fhirURL) {
        Consent c = new Consent();

        c.setId(consentEntity.getId().toString());

        // there is no consent status in entity, so we are defaulting to active.
        c.setStatus(Consent.ConsentState.ACTIVE);

        c.setDateTime(Date.from(consentEntity.getEffectiveDate().atStartOfDay().atOffset(ZoneOffset.UTC).toInstant()));
        c.setText(narrativeText(consentEntity.getPolicyCode(), consentEntity.getHicn(), consentEntity.getMbi()));
        c.setPatient(patient(fhirURL, consentEntity.getMbi()));

        c.setPolicyRule(policyRule(consentEntity.getPolicyCode()));

        c.setScope(scope(consentEntity.getScopeCode()));

        c.setCategory(category(consentEntity.getLoincCode()));

        return c;
    }
}
