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

    private static String policyCode(String value) {
        if (OPT_OUT.equals(value) || OPT_IN.equals(value)) {
            return value;
        }

        throw new IllegalArgumentException(String.format("invalid policyCode %s", value));
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

        // there is no consent status in entity, so we are defaulting to active. Correct?
        c.setStatus(Consent.ConsentState.ACTIVE);

        c.setDateTime(Date.from(consentEntity.getEffectiveDate().atStartOfDay().atOffset(ZoneOffset.UTC).toInstant()));
        c.setText(narrativeText(consentEntity.getPolicyCode(), consentEntity.getHicn(), consentEntity.getMbi()));
        c.setPatient(patient(fhirURL, consentEntity.getMbi()));

        // PolicyRule is a CodeableConcept in r4 but is a string in r3
        c.setPolicyRule(policyCode(consentEntity.getPolicyCode()));

        // scope is an r4 entity. in our data this is currently always "patient-privacy"
        // for the moment, we're not extending r3 to include it

        c.setCategory(category(consentEntity.getLoincCode()));

        c.setOrganization(List.of(new Reference().setReference(orgURL).setDisplay("Data at the Point of Care")));

        return c;
    }
}
