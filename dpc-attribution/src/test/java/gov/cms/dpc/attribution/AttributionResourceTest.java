package gov.cms.dpc.attribution;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.cms.dpc.attribution.models.AttributionCheckRequest;
import gov.cms.dpc.fhir.FHIRMediaTypes;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.DropwizardTestSupport;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static gov.cms.dpc.attribution.SharedMethods.UnmarshallResponse;
import static org.junit.jupiter.api.Assertions.*;

public class AttributionResourceTest {
    private static final DropwizardTestSupport<DPCAttributionConfiguration> APPLICATION = new DropwizardTestSupport<>(DPCAttributionService.class, null, ConfigOverride.config("server.applicationConnectors[0].port", "3727"));

    @BeforeAll
    public static void setup() {

    }

    @BeforeEach
    public void initDB() throws Exception {
        APPLICATION.before();
        APPLICATION.getApplication().run("db", "migrate");
        APPLICATION.getApplication().run("seed");
    }

    @AfterEach
    public void shutdown() {
        APPLICATION.after();
    }

    @Test
    public void testBasicAttributionFunctionality() throws IOException {

        try (final CloseableHttpClient client = HttpClients.createDefault()) {

            final HttpGet httpGet = new HttpGet("http://localhost:" + APPLICATION.getLocalPort() + "/v1/Group/0c527d2e-2e8a-4808-b11d-0fa06baf8254");
            httpGet.setHeader("Accept", FHIRMediaTypes.FHIR_JSON);

            try (final CloseableHttpResponse response = client.execute(httpGet)) {
                assertEquals(HttpStatus.OK_200, response.getStatusLine().getStatusCode(), "Should have succeeded");
                List<String> beneficiaries = UnmarshallResponse(response.getEntity());
                assertEquals(50, beneficiaries.size(), "Should have 50 beneficiaries");
            }

            // Check is attributed
            final HttpGet isAttributed = new HttpGet("http://localhost:" + APPLICATION.getLocalPort() + "/v1/Group/0c527d2e-2e8a-4808-b11d-0fa06baf8254/19990000002901");
            isAttributed.setHeader("Accept", FHIRMediaTypes.FHIR_JSON);

            try (final CloseableHttpResponse response = client.execute(isAttributed)) {
                assertEquals(HttpStatus.OK_200, response.getStatusLine().getStatusCode(), "Should be attributed");
            }

            // Remove some benes
            final HttpDelete httpRemove = new HttpDelete("http://localhost:" + APPLICATION.getLocalPort() + "/v1/Group/0c527d2e-2e8a-4808-b11d-0fa06baf8254/19990000002901");
            httpRemove.setHeader("Accept", FHIRMediaTypes.FHIR_JSON);

            try (CloseableHttpResponse response = client.execute(httpRemove)) {
                assertEquals(HttpStatus.NO_CONTENT_204, response.getStatusLine().getStatusCode(), "Should have succeeded");
            }

            // Check that they're gone
            try (CloseableHttpResponse response = client.execute(httpGet)) {
                assertEquals(HttpStatus.OK_200, response.getStatusLine().getStatusCode(), "Should have succeeded");
                List<String> beneficiaries = UnmarshallResponse(response.getEntity());
                assertEquals(49, beneficiaries.size(), "Should have 49 beneficiaries");
            }

//             Check not attributed

            final HttpGet notAttributed = new HttpGet("http://localhost:" + APPLICATION.getLocalPort() + "/v1/Group/0c527d2e-2e8a-4808-b11d-0fa06baf8254/19990000002901");
            notAttributed.setHeader("Accept", FHIRMediaTypes.FHIR_JSON);

            try (CloseableHttpResponse response = client.execute(notAttributed)) {
                assertEquals(HttpStatus.NOT_ACCEPTABLE_406, response.getStatusLine().getStatusCode(), "Should be attributed");
            }

//            // Add them back
//            final HttpPut httpCreate = new HttpPut("http://localhost:" + APPLICATION.getLocalPort() + "/v1/Group/0c527d2e-2e8a-4808-b11d-0fa06baf8254/19990000002901");
//            httpCreate.setHeader("Accept", FHIRMediaTypes.FHIR_JSON);
//
//            try (CloseableHttpResponse response = client.execute(httpCreate)) {
//                assertEquals(HttpStatus.NO_CONTENT_204, response.getStatusLine().getStatusCode(), "Should have succeeded");
//            }
//
//            // Check that they're back
//            try (CloseableHttpResponse response = client.execute(httpGet)) {
//                assertEquals(HttpStatus.OK_200, response.getStatusLine().getStatusCode(), "Should have succeeded");
//                List<String> beneficiaries = UnmarshallResponse(response.getEntity());
//                assertEquals(50, beneficiaries.size(), "Should have 50 beneficiaries");
//            }
//
//            // And attributed
//            try (CloseableHttpResponse response = client.execute(isAttributed)) {
//                assertEquals(HttpStatus.OK_200, response.getStatusLine().getStatusCode(), "Should be attributed");
//            }
        }
    }

    @Test
    void testUnknownProvider() throws IOException {

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            final HttpGet httpGet = new HttpGet("http://localhost:" + APPLICATION.getLocalPort() + "/v1/Group/0c527d2e-2e8a-4808-b11d-0fa06baf827b");
            httpGet.setHeader("Accept", FHIRMediaTypes.FHIR_JSON);

            try (CloseableHttpResponse response = client.execute(httpGet)) {
                assertEquals(HttpStatus.NOT_FOUND_404, response.getStatusLine().getStatusCode(), "Should have failed");
            }
        }
    }

    @Test
    void testManualAttributionCheck() throws IOException {
        final List<String> patientIDs = List.of("19990000002901", "19990000002902", "19990000002903");
        // Check that all patients are attributed
        BiConsumer<HttpResponse, ObjectMapper> consumer = (response, objectMapper) -> {
            assertEquals(HttpStatus.OK_200, response.getStatusLine().getStatusCode(), "Should have succeeded");

            // Ensure the list is empty, meaning all the patients are attributed
            final List patients;
            try {
                patients = objectMapper.readValue(EntityUtils.toString(response.getEntity()), List.class);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            assertEquals(0, patients.size(), "Should not have any unattributed patients");
        };


        checkAttributed("0c527d2e-2e8a-4808-b11d-0fa06baf827b", patientIDs, consumer);

        // Check that one (existing) patient is not attributed
        consumer = (response, objectMapper) -> {
            assertEquals(HttpStatus.OK_200, response.getStatusLine().getStatusCode(), "Should have succeeded");

            // Ensure the list is empty, meaning all the patients are attributed
            final List patients;
            try {
                patients = objectMapper.readValue(EntityUtils.toString(response.getEntity()), List.class);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            assertAll(() -> assertEquals(1, patients.size(), "Should not have any unattributed patients"),
                    () -> assertTrue(patients.contains("20000000000890"), "Should have specific unattributed patient"));
        };

        patientIDs.add("20000000000890");
        checkAttributed("0c527d2e-2e8a-4808-b11d-0fa06baf827b", patientIDs, consumer);

        // Check that an additional (non-existing) patient is not attributed
        consumer = (response, objectMapper) -> {
            assertEquals(HttpStatus.OK_200, response.getStatusLine().getStatusCode(), "Should have succeeded");

            // Ensure the list is empty, meaning all the patients are attributed
            final List patients;
            try {
                patients = objectMapper.readValue(EntityUtils.toString(response.getEntity()), List.class);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            assertAll(() -> assertEquals(2, patients.size(), "Should have two unattributed patients"),
                    () -> assertTrue(patients.contains("20000000000890"), "Should have specific unattributed patient"),
                    () -> assertTrue(patients.contains("-1"), "Should have bogus unattributed patient"));
        };

        patientIDs.add("-1");
        checkAttributed("0c527d2e-2e8a-4808-b11d-0fa06baf827b", patientIDs, consumer);

    }

    private void checkAttributed(String groupID, List<String> patientIDs, BiConsumer<HttpResponse, ObjectMapper> consumer) throws IOException {
        // Verify that all listed patients are attributed.
        final AttributionCheckRequest acr = new AttributionCheckRequest();
        acr.setGroupID(groupID);
        acr.setPatientIDs(patientIDs);
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            final HttpPost httpPost = new HttpPost("http://localhost:" + APPLICATION.getLocalPort() + "/v1/Group/0c527d2e-2e8a-4808-b11d-0fa06baf827b");
            final ObjectMapper objectMapper = new ObjectMapper();
            httpPost.setEntity(new StringEntity(objectMapper.writeValueAsString(acr)));
            httpPost.setHeader(HttpHeaders.ACCEPT, "application/json");
            httpPost.setHeader(HttpHeaders.CONTENT_TYPE, FHIRMediaTypes.FHIR_JSON);

            try (CloseableHttpResponse response = client.execute(httpPost)) {
                consumer.accept(response, objectMapper);
            }
        }
    }
}
