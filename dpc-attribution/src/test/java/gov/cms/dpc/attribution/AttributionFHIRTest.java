package gov.cms.dpc.attribution;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
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
import org.hl7.fhir.dstu3.model.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import static gov.cms.dpc.attribution.AttributionTestHelpers.DEFAULT_ORG_ID;
import static gov.cms.dpc.attribution.SharedMethods.createAttributionBundle;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AttributionFHIRTest {

    private static final DropwizardTestSupport<DPCAttributionConfiguration> APPLICATION = new DropwizardTestSupport<>(DPCAttributionService.class, null, ConfigOverride.config("server.applicationConnectors[0].port", "3727"));
    private static final FhirContext ctx = FhirContext.forDstu3();
    private static final String CSV = "test_associations.csv";
    private static Map<String, List<Pair<String, String>>> groupedPairs = new HashMap<>();
    private static final ObjectMapper mapper = new ObjectMapper();
    private static Organization organization;

    @BeforeAll
    static void setup() throws Exception {
        APPLICATION.before();
        APPLICATION.getApplication().run("db", "drop-all", "--confirm-delete-everything");
        APPLICATION.getApplication().run("db", "migrate");

        // Get the test seeds
        final InputStream resource = AttributionFHIRTest.class.getClassLoader().getResourceAsStream(CSV);
        if (resource == null) {
            throw new MissingResourceException("Can not find seeds file", AttributionFHIRTest.class.getName(), CSV);
        }

        // Read in the seeds and create the 'Roster' bundle
        groupedPairs = SeedProcessor.extractProviderMap(resource);

        // Create the Organization
        organization = AttributionTestHelpers.createOrganization(ctx, String.format("http://localhost:%s/v1/", APPLICATION.getLocalPort()));
    }

    @AfterAll
    static void shutdown() {
        APPLICATION.after();
    }

    @TestFactory
    Stream<DynamicTest> generateRosterTests() {

        // Create the Organization

        BiFunction<Bundle, String, String> nameGenerator = (bundle, operation) -> String.format("[%s] provider: %s", operation.toUpperCase(), ((Practitioner) bundle.getEntryFirstRep().getResource()).getIdentifierFirstRep().getValue());

        final UUID orgID = UUID.fromString(organization.getIdElement().getIdPart());

        // Get all the provider IDs and generate tests for them.
        return groupedPairs
                .entrySet()
                .stream()
                .map((Map.Entry<String, List<Pair<String, String>>> entry) -> SeedProcessor.generateRosterBundle(entry, orgID))
                .flatMap((bundle) -> Stream.of(
                        DynamicTest.dynamicTest(nameGenerator.apply(bundle, "Submit"), () -> submitRoster(bundle)),
                        DynamicTest.dynamicTest(nameGenerator.apply(bundle, "Update"), () -> updateRoster(bundle))));

    }

    private void submitRoster(Bundle bundle) throws Exception {

        // Provider first, then patients
        final Practitioner practitioner = (Practitioner) bundle.getEntryFirstRep().getResource();
        final String providerID = practitioner.getId();
        ctx.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);
        final IGenericClient client = ctx.newRestfulGenericClient("http://localhost:" + APPLICATION.getLocalPort() + "/v1/");

        final MethodOutcome createdPractitioner = client
                .create()
                .resource(practitioner)
                .encodedJson()
                .execute();

        assertTrue(createdPractitioner.getCreated(), "Should have created the practitioner");

        bundle
                .getEntry()
                .stream()
                .map(Bundle.BundleEntryComponent::getResource)
                .filter(resource -> resource.getResourceType() == ResourceType.Patient)
                .map(resource -> (Patient) resource)
                .forEach(patient -> {
                    final MethodOutcome created = client
                            .create()
                            .resource(patient)
                            .encodedJson()
                            .execute();
                    assertTrue(created.getCreated(), "Should have created the patient");
                });

        // Finally, the Group
        final Group rosterGroup = (Group) bundle.getEntry()
                .stream()
                .filter(entry -> entry.getResource().getResourceType() == ResourceType.Group)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Should have group"))
                .getResource();

        final MethodOutcome groupCreated = client
                .create()
                .resource(rosterGroup)
                .encodedJson()
                .execute();

        assertTrue(groupCreated.getCreated(), "Should have created the group");


        try (final CloseableHttpClient httpClient = HttpClients.createDefault()) {

            HttpPost httpPost = new HttpPost("http://localhost:" + APPLICATION.getLocalPort() + "/v1/Group/$submit");
            httpPost.setHeader("Accept", FHIRMediaTypes.FHIR_JSON);
            httpPost.setEntity(new StringEntity(ctx.newJsonParser().encodeResourceToString(bundle)));

            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                assertEquals(HttpStatus.CREATED_201, response.getStatusLine().getStatusCode(), "Should have succeeded");
            }

            // Get the patients

            // Check how many are attributed
            HttpGet getPatients = new HttpGet("http://localhost:" + APPLICATION.getLocalPort() + "/v1/Group/" + providerID);
            getPatients.setHeader("Accept", FHIRMediaTypes.FHIR_JSON);

            try (CloseableHttpResponse response = httpClient.execute(getPatients)) {
                assertEquals(HttpStatus.OK_200, response.getStatusLine().getStatusCode(), "Should be attributed");
                List<String> beneies = mapper.readValue(EntityUtils.toString(response.getEntity()), new TypeReference<List<String>>() {
                });
                assertEquals(bundle.getEntry().size() - 1, beneies.size(), "Should have the same number of beneies");
            }

            final String patientID = ((Patient) bundle.getEntry().get(1).getResource()).getIdentifierFirstRep().getValue();

            // Check that a specific patient is attributed
            final HttpGet isAttributed = new HttpGet(String.format("http://localhost:%d/v1/Group/%s/%s", APPLICATION.getLocalPort(), providerID, patientID));
            isAttributed.setHeader("Accept", FHIRMediaTypes.FHIR_JSON);

            try (CloseableHttpResponse response = httpClient.execute(isAttributed)) {
                assertEquals(HttpStatus.OK_200, response.getStatusLine().getStatusCode(), "Should be attributed");
            }

            // Submit again and make sure the size doesn't change
            httpPost = new HttpPost("http://localhost:" + APPLICATION.getLocalPort() + "/v1/Group/$submit");
            httpPost.setHeader("Accept", FHIRMediaTypes.FHIR_JSON);
            httpPost.setEntity(new StringEntity(ctx.newJsonParser().encodeResourceToString(bundle)));

            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                assertEquals(HttpStatus.CREATED_201, response.getStatusLine().getStatusCode(), "Should have succeeded");
            }

            // Check how many are attributed
            getPatients = new HttpGet("http://localhost:" + APPLICATION.getLocalPort() + "/v1/Group/" + providerID);
            getPatients.setHeader("Accept", FHIRMediaTypes.FHIR_JSON);

            try (CloseableHttpResponse response = httpClient.execute(getPatients)) {
                assertEquals(HttpStatus.OK_200, response.getStatusLine().getStatusCode(), "Should be attributed");
                List<String> beneies = mapper.readValue(EntityUtils.toString(response.getEntity()), new TypeReference<List<String>>() {
                });
                assertEquals(bundle.getEntry().size() - 1, beneies.size(), "Should have the same number of beneies");
            }
        }
    }


    private void updateRoster(Bundle bundle) throws IOException {

        final String providerID = ((Practitioner) bundle.getEntryFirstRep().getResource()).getIdentifierFirstRep().getValue();

        final HttpGet getPatients = new HttpGet("http://localhost:" + APPLICATION.getLocalPort() + "/v1/Group/" + providerID);
        getPatients.setHeader("Accept", FHIRMediaTypes.FHIR_JSON);

        try (final CloseableHttpClient client = HttpClients.createDefault()) {

            // Add an additional patient
            // Create a new bundle with extra patients to attribute
            final String newPatientID = "test-new-patient-id";
            final Bundle updateBundle = createAttributionBundle(providerID, newPatientID, organization.getIdElement().getIdPart());

            // Submit the bundle
            final HttpPost submitUpdate = new HttpPost("http://localhost:" + APPLICATION.getLocalPort() + "/v1/Group/$submit");
            submitUpdate.setHeader("Accept", FHIRMediaTypes.FHIR_JSON);
            submitUpdate.setEntity(new StringEntity(ctx.newJsonParser().encodeResourceToString(updateBundle)));

            try (CloseableHttpResponse response = client.execute(submitUpdate)) {
                assertEquals(HttpStatus.CREATED_201, response.getStatusLine().getStatusCode(), "Should have succeeded");
            }

            // Check how many are attributed

            try (CloseableHttpResponse response = client.execute(getPatients)) {
                assertEquals(HttpStatus.OK_200, response.getStatusLine().getStatusCode(), "Should be attributed");
                final List<String> patients = mapper.readValue(EntityUtils.toString(response.getEntity()), new TypeReference<List<String>>() {
                });
                // Since the practitioner is not
                assertEquals(bundle.getEntry().size(), patients.size(), "Should have an additional patient");
            }

            // Check that a specific patient is attributed
            final HttpGet updatedAttributed = new HttpGet(String.format("http://localhost:%d/v1/Group/%s/%s", APPLICATION.getLocalPort(), providerID, newPatientID));
            updatedAttributed.setHeader("Accept", FHIRMediaTypes.FHIR_JSON);

            try (CloseableHttpResponse response = client.execute(updatedAttributed)) {
                assertEquals(HttpStatus.OK_200, response.getStatusLine().getStatusCode(), "Should be attributed");
            }
        }
    }
}
