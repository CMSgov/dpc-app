package gov.cms.dpc.api;

import gov.cms.dpc.fhir.DPCIdentifierSystem;
import org.hl7.fhir.r4.model.*;

import java.util.List;

// R4 versions of test helpers, can be removed when V1 is removed

public class V3APITestHelpers {
    public static Provenance createProvenance(String orgId, String practitionerId, List<String> patientIds) {
        final CodeableConcept reasonConcept = new CodeableConcept()
                .addCoding(new Coding().setSystem("http://hl7.org/fhir/v3/ActReason").setCode("TREAT"));

        final CodeableConcept roleConcept = new CodeableConcept()
                .addCoding(new Coding().setSystem("http://terminology.hl7.org/CodeSystem/v3-RoleClass").setCode("AGNT"));

        final Provenance.ProvenanceAgentComponent component =
                new Provenance.ProvenanceAgentComponent()
                        .addRole(roleConcept)
                        .setWho(new Reference(new IdType("Organization", orgId)))
                        .setOnBehalfOf(new Reference(practitionerId));

        final Provenance provenance = new Provenance()
                .addReason(reasonConcept)
                .addAgent(component);
        provenance.setRecorded(new java.util.Date());

        patientIds.forEach(patientId -> provenance.addTarget(new Reference(patientId)));
        return provenance;
    }

    public static Patient createPatientWithMbi(String mbi) {
        Patient patient = new Patient();
        patient.addIdentifier().setSystem(DPCIdentifierSystem.MBI.getSystem()).setValue(mbi);
        return patient;
    }
}
