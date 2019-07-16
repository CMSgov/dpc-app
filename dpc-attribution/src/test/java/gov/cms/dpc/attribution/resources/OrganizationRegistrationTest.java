package gov.cms.dpc.attribution.resources;

import gov.cms.dpc.attribution.AbstractAttributionTest;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import gov.cms.dpc.fhir.FHIRMediaTypes;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrganizationRegistrationTest extends AbstractAttributionTest {

    private static final String BAD_ORG_ID = "0c527d2e-2e8a-4808-b11d-0fa06baf8252";

    private OrganizationRegistrationTest() {
        // Not used
    }

    @Test
    void testBasicRegistration() throws IOException {

        // Read in the test file
        final InputStream inputStream = OrganizationRegistrationTest.class.getClassLoader().getResourceAsStream("organization.tmpl.json");
        final Bundle resource = (Bundle) ctx.newJsonParser().parseResource(inputStream);

        try (final CloseableHttpClient client = HttpClients.createDefault()) {
            final HttpPost httpPost = new HttpPost(getServerURL() + "/Organization");
            httpPost.setHeader("Accept", FHIRMediaTypes.FHIR_JSON);
            httpPost.setEntity(new StringEntity(ctx.newJsonParser().encodeResourceToString(resource)));

            try (CloseableHttpResponse response = client.execute(httpPost)) {
                assertEquals(HttpStatus.CREATED_201, response.getStatusLine().getStatusCode(), "Should have succeeded");
            }
        }
    }

    @Test
    void testInvalidOrganization() throws IOException {

        // Read in the test file
        final Organization resource = new Organization();
        resource.addIdentifier().setSystem(DPCIdentifierSystem.MBI.getSystem()).setValue("test-mbi");


        try (final CloseableHttpClient client = HttpClients.createDefault()) {
            final HttpPost httpPost = new HttpPost(getServerURL() + "/Organization");
            httpPost.setHeader("Accept", FHIRMediaTypes.FHIR_JSON);
            httpPost.setEntity(new StringEntity(ctx.newJsonParser().encodeResourceToString(resource)));

            try (CloseableHttpResponse response = client.execute(httpPost)) {
                assertEquals(HttpStatus.INTERNAL_SERVER_ERROR_500, response.getStatusLine().getStatusCode(), "Should have failed");
            }
        }
    }

    @Test
    void testTokenGeneration() throws IOException {
        String macaroon;
        try (final CloseableHttpClient client = HttpClients.createDefault()) {
            final HttpPost httpPost = new HttpPost(getServerURL() + String.format("/Organization/%s/token", ORGANIZATION_ID));


            try (CloseableHttpResponse response = client.execute(httpPost)) {
                assertEquals(HttpStatus.OK_200, response.getStatusLine().getStatusCode(), "Should have found organization");
                macaroon = EntityUtils.toString(response.getEntity());
                // Verify that the first few bytes are correct, to ensure we encoded correctly.
                assertTrue(macaroon.startsWith("eyJ2IjoyLCJs"), "Should have correct starting string value");
            }
        }

        // Verify that it's correct.
        try (final CloseableHttpClient client = HttpClients.createDefault()) {
            final HttpGet httpGet = new HttpGet(getServerURL() + String.format("/Organization/%s/token/verify?token=%s", ORGANIZATION_ID, macaroon));

            try (CloseableHttpResponse response = client.execute(httpGet)) {
                assertEquals(HttpStatus.OK_200, response.getStatusLine().getStatusCode(), "Token should be valid");
            }
        }
    }

    @Test
    void testUnknownOrgTokenGeneration() throws IOException {
        try (final CloseableHttpClient client = HttpClients.createDefault()) {
            final HttpPost httpPost = new HttpPost(getServerURL() + "/Organization/1/token");

            try (CloseableHttpResponse response = client.execute(httpPost)) {
                assertEquals(HttpStatus.NOT_FOUND_404, response.getStatusLine().getStatusCode(), "Should not have found organization");
            }
        }
    }

    @Test
    void testEmptyTokenHandling() throws IOException {
        try (final CloseableHttpClient client = HttpClients.createDefault()) {
            final HttpGet httpGet = new HttpGet(getServerURL() + String.format("/Organization/%s/token/verify?token=%s", ORGANIZATION_ID, ""));

            try (CloseableHttpResponse response = client.execute(httpGet)) {
                assertEquals(HttpStatus.BAD_REQUEST_400, response.getStatusLine().getStatusCode(), "Should not be able to verify empty token");
            }
        }
    }

    @Test
    void testTokenWrongOrg() throws IOException {
        String macaroon;
        try (final CloseableHttpClient client = HttpClients.createDefault()) {
            final HttpPost httpPost = new HttpPost(getServerURL() + String.format("/Organization/%s/token", ORGANIZATION_ID));


            try (CloseableHttpResponse response = client.execute(httpPost)) {
                assertEquals(HttpStatus.OK_200, response.getStatusLine().getStatusCode(), "Should have found organization");
                macaroon = EntityUtils.toString(response.getEntity());
                // Verify that the first few bytes are correct, to ensure we encoded correctly.
                assertTrue(macaroon.startsWith("eyJ2IjoyLCJs"), "Should have correct starting string value");
            }
        }

        // Verify that it's unauthorized.
        try (final CloseableHttpClient client = HttpClients.createDefault()) {
            final HttpGet httpGet = new HttpGet(getServerURL() + String.format("/Organization/%s/token/verify?token=%s", BAD_ORG_ID, macaroon));

            try (CloseableHttpResponse response = client.execute(httpGet)) {
                assertEquals(HttpStatus.UNAUTHORIZED_401, response.getStatusLine().getStatusCode(), "Should not be valid");
            }
        }
    }
}
