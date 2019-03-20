package gov.cms.dpc.attribution;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.cms.dpc.fhir.FHIRMediaTypes;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.DropwizardTestSupport;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AttributionIntegrationTest {
    private static final DropwizardTestSupport<DPCAttributionConfiguration> APPLICATION = new DropwizardTestSupport<>(DPCAttributionService.class, null, ConfigOverride.config("server.applicationConnectors[0].port", "3727"));

    @BeforeAll
    public static void setup() {

    }

    @BeforeEach
    public void initDB() throws Exception {
        APPLICATION.before();
        APPLICATION.getApplication().run("db", "migrate");
//        APPLICATION.getApplication().run("seed");
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

            // TODO: This test section is commented out until DPC-21 is completed
//            final HttpDelete httpRemove = new HttpDelete("http://localhost:" + APPLICATION.getLocalPort() + "/v1/Group/0c527d2e-2e8a-4808-b11d-0fa06baf8254/19990000002901");
//            httpRemove.setHeader("Accept", FHIRMediaTypes.FHIR_JSON);
//
//            try (CloseableHttpResponse response = client.execute(httpRemove)) {
//                assertEquals(HttpStatus.NO_CONTENT_204, response.getStatusLine().getStatusCode(), "Should have succeeded");
//            }
//
//            // Check that they're gone
//            try (CloseableHttpResponse response = client.execute(httpGet)) {
//                assertEquals(HttpStatus.OK_200, response.getStatusLine().getStatusCode(), "Should have succeeded");
//                List<String> beneficiaries = UnmarshallResponse(response.getEntity());
//                assertEquals(49, beneficiaries.size(), "Should have 49 beneficiaries");
//            }

            // Check not attributed

//            final HttpGet notAttributed = new HttpGet("http://localhost:" + APPLICATION.getLocalPort() + "/v1/Group/0c527d2e-2e8a-4808-b11d-0fa06baf8254/19990000002901");
//            notAttributed.setHeader("Accept", FHIRMediaTypes.FHIR_JSON);
//
//            try (CloseableHttpResponse response = client.execute(notAttributed)) {
//                assertEquals(HttpStatus.NOT_ACCEPTABLE_406, response.getStatusLine().getStatusCode(), "Should be attributed");
//            }

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
    public void testUnknownProvider() throws IOException {

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            final HttpGet httpGet = new HttpGet("http://localhost:" + APPLICATION.getLocalPort() + "/v1/Group/0c527d2e-2e8a-4808-b11d-0fa06baf827b");
            httpGet.setHeader("Accept", FHIRMediaTypes.FHIR_JSON);

            try (CloseableHttpResponse response = client.execute(httpGet)) {
                assertEquals(HttpStatus.NOT_FOUND_404, response.getStatusLine().getStatusCode(), "Should have failed");
            }
        }
    }


    @SuppressWarnings("unchecked")
    private static <T> List<T> UnmarshallResponse(HttpEntity entity) throws IOException {
        final ObjectMapper mapper = new ObjectMapper();

        return (List<T>) mapper.readValue(entity.getContent(), List.class);
    }
}
