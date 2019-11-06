package gov.cms.dpc.fhir.converters.entities;

import gov.cms.dpc.common.consent.entities.ConsentEntity;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.utilities.xhtml.NodeType;
import org.hl7.fhir.utilities.xhtml.XhtmlNode;

import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;

/**
 * A utility class for converting a ConsentEntity into a FHIR Consent Resource.
 */
public class ConsentEntityConverter {
    private static final String OPT_IN = "OPTIN";
    private static final String OPT_OUT = "OPTOUT";

    private ConsentEntityConverter() {}

    private static String patientIdentifier(DPCIdentifierSystem type, String value) {
        return String.format("%s|%s ", type.getSystem(), value);
    }

    private static Narrative narrativeText(String inOrOut, String hicn, String mbi) {
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

    private static String policyCode(String value) {
        if (OPT_OUT.equals(value) || OPT_IN.equals(value)) {
            return value;
        }
        throw new IllegalArgumentException(String.format("invalid policyCode %s", value));
    }

    private static List<CodeableConcept> category(String loincCode) {
        // there must be a way to look up the code systems used in these CodeableConcept values. what is it?
        CodeableConcept category = new CodeableConcept();
        category.addCoding().setSystem("http://loinc.org").setCode(loincCode);
        return List.of(category);
    }

    public static Consent convert(ConsentEntity consentEntity) {
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
}
