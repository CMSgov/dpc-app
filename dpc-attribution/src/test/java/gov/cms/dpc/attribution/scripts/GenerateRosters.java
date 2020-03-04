package gov.cms.dpc.attribution.scripts;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Patient;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Generate Rosters from SyntheticMass data
 * This is marked as {@link Disabled} because it's not actually a test, we put it here to prevent it from being pulled into the runtime JAR.
 * <p>
 * Make sure APIKEY environment variable it set
 */
@Disabled
class GenerateRosters {

    private static final String CSV = "test_associations.csv";
    private static final String SYNTHEA_URL = "https://syntheticmass.mitre.org/v1/fhir/";

    private GenerateRosters() {
        // Not used
    }

    @Test
    public void generatePatients() throws IOException {

        final String apikey = System.getenv("APIKEY");
        if (apikey == null) {
            throw new IllegalStateException("APIKEY must be set");
        }

        final FhirContext ctx = FhirContext.forDstu3();
        ctx.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);
        final IGenericClient client = ctx.newRestfulGenericClient(SYNTHEA_URL);

        // Disable logging for tests
        LoggingInterceptor loggingInterceptor = new LoggingInterceptor();
        loggingInterceptor.setLogRequestSummary(false);
        loggingInterceptor.setLogResponseSummary(false);
        client.registerInterceptor(loggingInterceptor);

        // Create a Bundle to hold everything
        final Bundle patientBundle = new Bundle();

        patientBundle.setId("synthetic-roster-bundle");
        patientBundle.setType(Bundle.BundleType.COLLECTION);

        // List of Patients from the test seeds file
        final List<String> idFromSeeds = new ArrayList<>(getPatientIDFromSeeds());

        final Bundle syntheaBundle = client
                .search()
                .byUrl(String.format("%s/Patient/?count=%d&apikey=%s", SYNTHEA_URL, idFromSeeds.size(), apikey))
                .returnBundle(Bundle.class)
                .execute();

        final List<Patient> patientResources = syntheaBundle
                .getEntry()
                .stream()
                .map(Bundle.BundleEntryComponent::getResource)
                .map(resource -> (Patient) resource)
                .collect(Collectors.toList());

        for (int i = 0; i < patientResources.size(); i++) {
            final Patient patient = patientResources.get(i);
            final String patientMBI = idFromSeeds.get(i);

            // Add the patient MBI to the resource
            patient.addIdentifier()
                    .setSystem(DPCIdentifierSystem.MBI.getSystem())
                    .setValue(patientMBI);

            // Add to the Bundle
            patientBundle.addEntry().setResource(patient);
        }

        // Dump it all to a file
        try (FileWriter fileWriter = new FileWriter("../src/main/resources/patient_bundle.json", StandardCharsets.UTF_8)) {
            ctx.newJsonParser().encodeResourceToWriter(patientBundle, fileWriter);
        }
    }

    private Set<String> getPatientIDFromSeeds() throws IOException {
        Set<String> patientIDs = new HashSet<>();
        try (InputStream stream = GenerateRosters.class.getClassLoader().getResourceAsStream(CSV)) {

            if (stream == null) {
                throw new MissingResourceException("Cannot find attribution file", GenerateRosters.class.getName(), CSV);
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                for (String line; (line = reader.readLine()) != null; ) {
                    // We can ignore this, because it's not worth pulling in Guava just for this.
                    final String[] splits = line.split(",", -1);

                    patientIDs.add(splits[0]);
                }
            }
        }

        return patientIDs;
    }
}

