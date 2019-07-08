package gov.cms.dpc.api;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.client.api.IClientInterceptor;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.IHttpRequest;
import ca.uhn.fhir.rest.client.api.IHttpResponse;
import gov.cms.dpc.fhir.FHIRMediaTypes;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.eclipse.jetty.http.HttpStatus;
import org.hl7.fhir.dstu3.model.Bundle;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class APITestHelpers {
    public static String ATTRIBUTION_URL = "http://localhost:3500/v1";
    public static final String ORGANIZATION_ID = "0c527d2e-2e8a-4808-b11d-0fa06baf8254";

    private APITestHelpers() {
        // Not used
    }

    public static String setupOrganizationTest(FhirContext ctx) throws IOException {
        // Register an organization, and a token
        // Read in the test file
        String macaroon;
        try (InputStream inputStream = AuthenticationTest.class.getClassLoader().getResourceAsStream("organization.tmpl.json")) {
            final IParser parser = ctx.newJsonParser();
            final Bundle orgBundle = (Bundle) parser.parseResource(inputStream);
            final String bundleString = parser.encodeResourceToString(orgBundle);
            try (CloseableHttpClient client = HttpClients.createDefault()) {
                final HttpPost httpPost = new HttpPost(ATTRIBUTION_URL + "/Organization");
                httpPost.setHeader("Accept", FHIRMediaTypes.FHIR_JSON);
                httpPost.setEntity(new StringEntity(bundleString));

                try (CloseableHttpResponse response = client.execute(httpPost)) {
                    assertEquals(HttpStatus.OK_200, response.getStatusLine().getStatusCode(), "Should have succeeded");
                }

                // Now, create a Macaroon
                final HttpPost tokenPost = new HttpPost(String.format("%s/Organization/%s/token", ATTRIBUTION_URL, ORGANIZATION_ID));
                tokenPost.setHeader("Accept", FHIRMediaTypes.FHIR_JSON);

                try (CloseableHttpResponse response = client.execute(tokenPost)) {
                    assertEquals(HttpStatus.OK_200, response.getStatusLine().getStatusCode(), "Should have succeeded");
                    macaroon = EntityUtils.toString(response.getEntity());
                    assertNotNull(macaroon, "Should have Macaroon");
                }
            }
        }

        return macaroon;
    }

    // TODO: Remove as part of DPC-373
    public static IGenericClient buildAuthenticatedClient(FhirContext ctx, String baseURL,String macaroon) {
        final IGenericClient client = ctx.newRestfulGenericClient(baseURL);
        client.registerInterceptor(new MacaroonsInterceptor(macaroon));

        return client;
    }

    public static class MacaroonsInterceptor implements IClientInterceptor {

        private final String macaroon;

        MacaroonsInterceptor(String macaroon) {
            this.macaroon = macaroon;
        }

        @Override
        public void interceptRequest(IHttpRequest theRequest) {
            theRequest.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + this.macaroon);
        }

        @Override
        public void interceptResponse(IHttpResponse theResponse) {
            // Not used
        }
    }
}
