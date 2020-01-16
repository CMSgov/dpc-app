package gov.cms.dpc.fhir.converters.entities;

import gov.cms.dpc.common.consent.entities.ConsentEntity;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.utilities.xhtml.NodeType;
import org.hl7.fhir.utilities.xhtml.XhtmlNode;

import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;

import static gov.cms.dpc.common.consent.entities.ConsentEntity.OPT_IN;
import static gov.cms.dpc.common.consent.entities.ConsentEntity.OPT_OUT;

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

    private static List<CodeableConcept> category(String loincCode) {
        // there must code to look up the code systems used in these CodeableConcept values. What is it?
        CodeableConcept category = new CodeableConcept();
        category.addCoding().setSystem("http://loinc.org").setCode(loincCode).setDisplay(ConsentEntity.CATEGORY_DISPLAY);
        return List.of(category);
    }

    public static Consent convert(ConsentEntity consentEntity, String orgURL, String fhirURL) {
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

        if (consentEntity.getCustodian() == null) {
            c.setOrganization(List.of(new Reference().setReference(orgURL).setDisplay("Data at the Point of Care")));
        } else {
            c.setOrganization(List.of(new Reference(new IdType("Organization", consentEntity.getCustodian().toString()))));
        }

        return c;
    }
}
