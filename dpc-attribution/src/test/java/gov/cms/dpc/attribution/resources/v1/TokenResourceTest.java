package gov.cms.dpc.attribution.resources.v1;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.cms.dpc.attribution.AbstractAttributionTest;
import gov.cms.dpc.attribution.AttributionTestHelpers;
import gov.cms.dpc.common.entities.TokenEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.eclipse.jetty.http.HttpStatus;
import org.hl7.fhir.dstu3.model.Organization;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TokenResourceTest extends AbstractAttributionTest {

    private static final String BAD_ORG_ID = "0c527d2e-2e8a-4808-b11d-0fa06baf8252";

    private final ObjectMapper mapper;

    private TokenResourceTest() {
        this.mapper = new ObjectMapper();
    }


    @Test
    void testTokenGeneration() throws IOException {
        final Organization organization = AttributionTestHelpers.createOrganization(ctx, getServerURL());
        final String org_id = organization.getIdElement().getIdPart();
        String macaroon;
        try (final CloseableHttpClient client = HttpClients.createDefault()) {
            final HttpPost httpPost = new HttpPost(getServerURL() + String.format("/Token/%s", org_id));


            try (CloseableHttpResponse response = client.execute(httpPost)) {
                assertEquals(HttpStatus.OK_200, response.getStatusLine().getStatusCode(), "Should have found organization");
                macaroon = EntityUtils.toString(response.getEntity());
                // Verify that the first few bytes are correct, to ensure we encoded correctly.
                assertTrue(macaroon.startsWith("eyJ2IjoyLCJs"), "Should have correct starting string value");
            }
        }

        // Verify that it's correct.
        try (final CloseableHttpClient client = HttpClients.createDefault()) {
            final HttpGet httpGet = new HttpGet(getServerURL() + String.format("/Token/%s/verify?token=%s", org_id, macaroon));

            try (CloseableHttpResponse response = client.execute(httpGet)) {
                assertEquals(HttpStatus.OK_200, response.getStatusLine().getStatusCode(), "Token should be valid");
            }
        }

        // Verify token only works for the given organization

        // Verify that it's unauthorized.
        try (final CloseableHttpClient client = HttpClients.createDefault()) {
            final HttpGet httpGet = new HttpGet(getServerURL() + String.format("/Token/%s/verify?token=%s", BAD_ORG_ID, macaroon));

            try (CloseableHttpResponse response = client.execute(httpGet)) {
                assertEquals(HttpStatus.UNAUTHORIZED_401, response.getStatusLine().getStatusCode(), "Should not be valid");
            }
        }
    }

    @Test
    void testUnknownOrgTokenGeneration() throws IOException {
        try (final CloseableHttpClient client = HttpClients.createDefault()) {
            final HttpPost httpPost = new HttpPost(getServerURL() + "/Token/" + UUID.randomUUID().toString());

            try (CloseableHttpResponse response = client.execute(httpPost)) {
                assertEquals(HttpStatus.NOT_FOUND_404, response.getStatusLine().getStatusCode(), "Should not have found organization");
            }
        }
    }

    @Test
    void testEmptyTokenHandling() throws IOException {
        try (final CloseableHttpClient client = HttpClients.createDefault()) {
            final HttpGet httpGet = new HttpGet(getServerURL() + String.format("/Token/%s/verify?token=%s", ORGANIZATION_ID, ""));

            try (CloseableHttpResponse response = client.execute(httpGet)) {
                assertEquals(HttpStatus.BAD_REQUEST_400, response.getStatusLine().getStatusCode(), "Should not be able to verify empty token");
            }
        }
    }

    @Test
    void testNonMacaroonHandling() throws IOException {
        final String badToken = Base64.getUrlEncoder().encodeToString(new String("This is not a macaroon").getBytes(StandardCharsets.UTF_8));
        try (final CloseableHttpClient client = HttpClients.createDefault()) {
            final HttpGet httpGet = new HttpGet(getServerURL() + String.format("/Token/%s/verify?token=%s", ORGANIZATION_ID, badToken));

            try (CloseableHttpResponse response = client.execute(httpGet)) {
                assertEquals(HttpStatus.UNPROCESSABLE_ENTITY_422, response.getStatusLine().getStatusCode(), "Should not be able to verify empty token");
            }
        }
    }

    @Test
    void testTokenList() throws IOException {

        final Organization organization = AttributionTestHelpers.createOrganization(ctx, getServerURL());
        final String org_id = organization.getIdElement().getIdPart();
        String macaroon;
        try (final CloseableHttpClient client = HttpClients.createDefault()) {
            final HttpPost httpPost = new HttpPost(getServerURL() + String.format("/Token/%s", org_id));


            try (CloseableHttpResponse response = client.execute(httpPost)) {
                assertEquals(HttpStatus.OK_200, response.getStatusLine().getStatusCode(), "Should have found organization");
                macaroon = EntityUtils.toString(response.getEntity());
                // Verify that the first few bytes are correct, to ensure we encoded correctly.
                assertTrue(macaroon.startsWith("eyJ2IjoyLCJs"), "Should have correct starting string value");
            }
        }

        try (final CloseableHttpClient client = HttpClients.createDefault()) {
            final HttpGet httpGet = new HttpGet(getServerURL() + String.format("/Token/%s", org_id));

            try (CloseableHttpResponse response = client.execute(httpGet)) {
                final List<TokenEntity> tokens = this.mapper.readValue(response.getEntity().getContent(), new TypeReference<List<TokenEntity>>() {
                });

                assertEquals(1, tokens.size(), "Should have a single token");
                final TokenEntity token = tokens.get(0);

                assertAll(() -> assertEquals(String.format("Access token for organization %s.", org_id), token.getLabel(), "Should have auto-generated label"),
                        () -> assertEquals(LocalDate.now().plus(1, ChronoUnit.YEARS), token.getExpiresAt().toLocalDate(), "Should expire in 1 year"));
            }
        }
    }
}
