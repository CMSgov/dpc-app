package gov.cms.dpc.attribution.scripts;

import ca.uhn.fhir.context.FhirContext;
import gov.cms.dpc.fhir.FHIRExtractors;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Parameters;
import org.hl7.fhir.dstu3.model.Patient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Helper script for adding MBI identifiers to patient rosters.
 * This is marked as {@link Disabled} because it's not actually a test, we put it here to prevent it from being pulled into the runtime JAR.
 */
@Disabled
public class AddMBIToPatients {

    static final String MBI_CSV = "prod_sbx_bene_ids.csv";

    private static Map<String, PatientMBI> patientMap = new HashMap<>();
    private static FhirContext ctx = FhirContext.forDstu3();

    @BeforeAll
    static void setup() throws IOException {
        try (InputStream inputStream = AddMBIToPatients.class.getClassLoader().getResourceAsStream(MBI_CSV)) {
            assert inputStream != null;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                for (String line; (line = reader.readLine()) != null; ) {
                    final String[] splits = line.split(",", -1);
                    patientMap.put(splits[0], new PatientMBI(splits[1], splits[2]));
                }
            }
        }
    }

    @Test
    void updatePatients() throws IOException {
        final String bundleName = "patient_bundle.json";
        try (InputStream stream = AddMBIToPatients.class.getClassLoader().getResourceAsStream(bundleName)) {
            final Bundle bundle = (Bundle) ctx.newJsonParser().parseResource(stream);
            final Bundle updatedBundle = updateBundle(bundle, (id) -> "-" + id);

            try (FileWriter fileWriter = new FileWriter(String.format("../src/main/resources/%s", bundleName), StandardCharsets.UTF_8)) {
                ctx.newJsonParser().encodeResourceToWriter(updatedBundle, fileWriter);
            }
        }
    }

    @Test
    void updateDPRPatients() throws IOException {
        final String bundleName = "patient_bundle-dpr.json";
        try (InputStream stream = AddMBIToPatients.class.getClassLoader().getResourceAsStream(bundleName)) {
            final Parameters parameters = (Parameters) ctx.newJsonParser().parseResource(stream);
            final Bundle updatedBundle = updateBundle((Bundle) parameters.getParameterFirstRep().getResource(), (id) -> id);

            try (FileWriter fileWriter = new FileWriter(String.format("../src/main/resources/%s", bundleName), StandardCharsets.UTF_8)) {
                ctx.newJsonParser().encodeResourceToWriter(updatedBundle, fileWriter);
            }
        }
    }

    private Bundle updateBundle(Bundle bundle, Function<String, String> beneIDConverter) {
        bundle
                .getEntry()
                .stream()
                .map(Bundle.BundleEntryComponent::getResource)
                .map(resource -> (Patient) resource)
                .forEach(patient -> {
                    final String beneID = FHIRExtractors.getPatientMBI(patient);
                    final PatientMBI mbi = patientMap.get(beneIDConverter.apply(beneID));
                    assert !(mbi == null);
                    patient.addIdentifier().setSystem("http://hl7.org/fhir/sid/us-mbi").setValue(mbi.mbi);
                    patient.addIdentifier().setSystem("https://bluebutton.cms.gov/resources/identifier/mbi-hash").setValue(mbi.mbiHash);
                });

        return bundle;
    }

    private static class PatientMBI {
        private final String mbi;
        private final String mbiHash;

        private PatientMBI(String mbi, String mbiHash) {
            this.mbi = mbi;
            this.mbiHash = mbiHash;
        }
    }

}
