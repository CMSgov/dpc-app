package gov.cms.dpc.attribution.resources;

import gov.cms.dpc.attribution.AbstractAttributionTest;
import gov.cms.dpc.fhir.FHIRMediaTypes;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static gov.cms.dpc.attribution.SharedMethods.UnmarshallResponse;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AttributionResourceTest extends AbstractAttributionTest {

    private AttributionResourceTest() {
        // Not used
    }

    @Test
    void testBasicAttributionFunctionality() throws IOException {

        try (final CloseableHttpClient client = HttpClients.createDefault()) {

            final HttpGet httpGet = new HttpGet(getServerURL() + "/Group/0c527d2e-2e8a-4808-b11d-0fa06baf8254");
            httpGet.setHeader("Accept", FHIRMediaTypes.FHIR_JSON);

            try (final CloseableHttpResponse response = client.execute(httpGet)) {
                assertEquals(HttpStatus.OK_200, response.getStatusLine().getStatusCode(), "Should have succeeded");
                List<String> beneficiaries = UnmarshallResponse(response.getEntity());
                assertEquals(50, beneficiaries.size(), "Should have 50 beneficiaries");
            }

            // Check is attributed
            final HttpGet isAttributed = new HttpGet(getServerURL() + "/Group/0c527d2e-2e8a-4808-b11d-0fa06baf8254/19990000002901");
            isAttributed.setHeader("Accept", FHIRMediaTypes.FHIR_JSON);

            try (final CloseableHttpResponse response = client.execute(isAttributed)) {
                assertEquals(HttpStatus.OK_200, response.getStatusLine().getStatusCode(), "Should be attributed");
            }

            // Remove some benes
            final HttpDelete httpRemove = new HttpDelete(getServerURL() + "/Group/0c527d2e-2e8a-4808-b11d-0fa06baf8254/19990000002901");
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

            final HttpGet notAttributed = new HttpGet(getServerURL() + "/Group/0c527d2e-2e8a-4808-b11d-0fa06baf8254/19990000002901");
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
            final HttpGet httpGet = new HttpGet(getServerURL() + "Group/0c527d2e-2e8a-4808-b11d-0fa06baf827b");
            httpGet.setHeader("Accept", FHIRMediaTypes.FHIR_JSON);

            try (CloseableHttpResponse response = client.execute(httpGet)) {
                assertEquals(HttpStatus.NOT_FOUND_404, response.getStatusLine().getStatusCode(), "Should have failed");
            }
        }
    }
}
