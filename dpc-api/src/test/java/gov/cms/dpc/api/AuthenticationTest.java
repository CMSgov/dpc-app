package gov.cms.dpc.api;

import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.client.api.IClientInterceptor;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.IHttpRequest;
import ca.uhn.fhir.rest.client.api.IHttpResponse;
import ca.uhn.fhir.rest.gclient.IReadExecutable;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
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
import org.hl7.fhir.dstu3.model.Organization;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

public class AuthenticationTest extends AbstractApplicationTest {

    private static String ATTRIBUTION_URL = "http://localhost:3500/v1";
    private static final String ORGANIZATION_ID = "0c527d2e-2e8a-4808-b11d-0fa06baf8254";
    private static final String BAD_ORG_ID = "0c527d2e-2e8a-4808-b11d-0fa06baf8252";

    private AuthenticationTest() {
        // Not used
    }

    @Test
    void testBasicAuthentication() throws IOException {
        // Manually setup the required org functions
        final String macaroon = setupTest();

        // Now, try to read the organization, which should succeed
        final IGenericClient client = this.ctx.newRestfulGenericClient(getBaseURL());
        client.registerInterceptor(new MacaroonsInterceptor(macaroon));

        final Organization organization = client
                .read()
                .resource(Organization.class)
                .withId(ORGANIZATION_ID)
                .encodedJson()
                .execute();

        assertNotNull(organization, "Should have a single Organization");

        final IReadExecutable<Organization> orgRequest = client
                .read()
                .resource(Organization.class)
                .withId(BAD_ORG_ID)
                .encodedJson();

        assertThrows(AuthenticationException.class, orgRequest::execute, "Should be unauthorized");
    }

    private String setupTest() throws IOException {
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
