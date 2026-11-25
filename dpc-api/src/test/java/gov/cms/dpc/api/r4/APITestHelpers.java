package gov.cms.dpc.api.r4;

import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import gov.cms.dpc.testing.factories.r4.FHIRPatientBuilder;
import gov.cms.dpc.testing.factories.r4.FHIRPractitionerBuilder;
import org.hl7.fhir.r4.model.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

public class APITestHelpers {
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

    public static Practitioner createPractitionerResource(String npi, String orgId) {
        return FHIRPractitionerBuilder.newBuilder()
                .withNpi(npi)
                .withOrgTag(orgId)
                .withName("Test", "Practitioner")
                .withId(UUID.randomUUID())
                .build();
    }

    public static Patient createPatientResource(String mbi, String orgId) {
        return FHIRPatientBuilder.newBuild()
                .withMbi(mbi)
                .withBirthDate("1990-01-01")
                .withName("Test", "Patient")
                .withGender(Enumerations.AdministrativeGender.OTHER)
                .managedBy(orgId)
                .withId(UUID.randomUUID())
                .build();
    }

    public static void setupPractitionerTest(IGenericClient client, IParser parser) throws IOException {
        try (InputStream inputStream = APITestHelpers.class.getClassLoader().getResourceAsStream("r4/provider_bundle.json")) {
            final Parameters providerParameters = (Parameters) parser.parseResource(inputStream);

            client
                    .operation()
                    .onType(Practitioner.class)
                    .named("submit")
                    .withParameters(providerParameters)
                    .encodedJson()
                    .execute();
        }
    }

    public static void setupPatientTest(IGenericClient client, IParser parser) throws IOException {
        try (InputStream inputStream = APITestHelpers.class.getClassLoader().getResourceAsStream("r4/patient_bundle.json")) {
            final Parameters patientParameters = (Parameters) parser.parseResource(inputStream);

            client
                    .operation()
                    .onType(Patient.class)
                    .named("submit")
                    .withParameters(patientParameters)
                    .encodedJson()
                    .execute();
        }
    }
}
