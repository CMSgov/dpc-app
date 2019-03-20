package gov.cms.dpc.attribution;

import ca.uhn.fhir.context.FhirContext;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.cms.dpc.common.utils.SeedProcessor;
import gov.cms.dpc.fhir.FHIRMediaTypes;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.DropwizardTestSupport;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.eclipse.jetty.http.HttpStatus;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Practitioner;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AttributionFHIRTest {

    private static final DropwizardTestSupport<DPCAttributionConfiguration> APPLICATION = new DropwizardTestSupport<>(DPCAttributionService.class, null, ConfigOverride.config("server.applicationConnectors[0].port", "3727"));
    private static final FhirContext ctx = FhirContext.forDstu3();
    private static final String CSV = "test_associations.csv";
    private static Map<String, List<Pair<String, String>>> groupedPairs = new HashMap<>();
    private static final ObjectMapper mapper = new ObjectMapper();
    private static SeedProcessor seedProcessor;

    @BeforeAll
    public static void setup() throws IOException {
        APPLICATION.before();

        // Get the test seeds
        final InputStream resource = AttributionFHIRTest.class.getClassLoader().getResourceAsStream(CSV);
        if (resource == null) {
            throw new MissingResourceException("Can not find seeds file", AttributionFHIRTest.class.getName(), CSV);
        }

        // Read in the seeds and create the 'Roster' bundle
        seedProcessor = new SeedProcessor(resource);
        groupedPairs = seedProcessor.extractProviderMap();
    }

    @BeforeEach
    public void initDB() throws Exception {
        APPLICATION.getApplication().run("db", "drop-all", "--confirm-delete-everything");
        APPLICATION.getApplication().run("db", "migrate");
    }

    @AfterAll
    public static void shutdown() throws Exception {
        APPLICATION.getApplication().run("db", "drop-all", "--confirm-delete-everything");
        APPLICATION.after();
    }

    @TestFactory
    Stream<DynamicTest> generateBundleTests() {

        Function<Bundle, String> nameGenerator = (bundle) -> String.format("Testing provider: %s", ((Practitioner) bundle.getEntryFirstRep().getResource()).getIdentifierFirstRep().getValue());

        // Get all the provider IDs and generate tests for them.
        return groupedPairs
                .entrySet()
                .stream()
                .map(seedProcessor::generateRosterBundle)
                .map((bundle) -> DynamicTest.dynamicTest(nameGenerator.apply(bundle), () -> submitRoster(bundle)));

    }

    private void submitRoster(Bundle bundle) throws Exception {

        // Manually call lifecycle hooks
        initDB();

        final String practitionerID = ((Practitioner) bundle.getEntryFirstRep().getResource()).getIdentifierFirstRep().getValue();
        final String patientID = ((Patient) bundle.getEntry().get(1).getResource()).getIdentifierFirstRep().getValue();

        try (final CloseableHttpClient client = HttpClients.createDefault()) {

            final HttpPost httpPost = new HttpPost("http://localhost:" + APPLICATION.getLocalPort() + "/v1/Group");
            httpPost.setHeader("Accept", FHIRMediaTypes.FHIR_JSON);
            httpPost.setEntity(new StringEntity(ctx.newJsonParser().encodeResourceToString(bundle)));

            try (CloseableHttpResponse response = client.execute(httpPost)) {
                assertEquals(HttpStatus.OK_200, response.getStatusLine().getStatusCode(), "Should have succeeded");
            }

            // Get the patients

            // Check how many are attributed
            final HttpGet getPatients = new HttpGet("http://localhost:" + APPLICATION.getLocalPort() + "/v1/Group/" + practitionerID);
            getPatients.setHeader("Accept", FHIRMediaTypes.FHIR_JSON);

            try (CloseableHttpResponse response = client.execute(getPatients)) {
                assertEquals(HttpStatus.OK_200, response.getStatusLine().getStatusCode(), "Should be attributed");
                List<String> beneies = mapper.readValue(EntityUtils.toString(response.getEntity()), new TypeReference<List<String>>() {
                });
                // Since the practitioner is not
                assertEquals(bundle.getEntry().size() - 1, beneies.size(), "Should have the same number of beneies");
            }

            // Check that a specific patient is attributed
            final HttpGet isAttributed = new HttpGet(String.format("http://localhost:%d/v1/Group/%s/%s", APPLICATION.getLocalPort(), practitionerID, patientID));
            isAttributed.setHeader("Accept", FHIRMediaTypes.FHIR_JSON);

            try (CloseableHttpResponse response = client.execute(isAttributed)) {
                assertEquals(HttpStatus.OK_200, response.getStatusLine().getStatusCode(), "Should be attributed");
            }

            // Remove the patient and try again
        }
    }


}
