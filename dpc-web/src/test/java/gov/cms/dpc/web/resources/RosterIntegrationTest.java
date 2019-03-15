package gov.cms.dpc.web.resources;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import gov.cms.dpc.queue.Pair;
import gov.cms.dpc.web.AbstractApplicationTest;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Practitioner;
import org.hl7.fhir.instance.model.api.IBaseOperationOutcome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.MissingResourceException;
import java.util.stream.Collectors;

public class RosterIntegrationTest extends AbstractApplicationTest {

    private static final String CSV = "test_associations.csv";
    private List<Bundle> providerBundles = new ArrayList<>();


    @BeforeEach
    public void setupEach() throws IOException {

        final List<Pair<String, String>> providerPairs = new ArrayList<>();
        // Get the test seeds
        final InputStream resource = RosterIntegrationTest.class.getClassLoader().getResourceAsStream(CSV);
        if (resource == null) {
            throw new MissingResourceException("Can not find seeds file", this.getClass().getName(), CSV);
        }

        // Truncate everything

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource, StandardCharsets.UTF_8))) {
            for (String line; (line = reader.readLine()) != null; ) {
                final String[] splits = line.split(",");

                providerPairs.add(new Pair<>(splits[1], splits[0]));
            }
        }

        providerPairs
                .stream()
                .collect(Collectors.groupingBy(Pair::getLeft))
                .forEach((provider, patients) -> {
                    final Bundle bundle = new Bundle();

                    final Practitioner practitioner = new Practitioner();
                    practitioner.setId(provider);

                    patients
                            .forEach((value) -> {
                                final Patient patient = new Patient();
                                patient.addIdentifier().setValue(value.getRight());
                                bundle.addEntry().setResource(patient);
                            });
                    providerBundles.add(bundle);
                });
    }

    @Test
    public void test() {

        final IGenericClient client = ctx.newRestfulGenericClient("http://localhost:" + APPLICATION.getLocalPort() + "/v1/");

        final Bundle firstBundle = providerBundles.get(0);

        final MethodOutcome execute = client
                .create()
                .resource(firstBundle)
                .encodedJson()
                .execute();

        final IBaseOperationOutcome operationOutcome = execute.getOperationOutcome();
    }
}
