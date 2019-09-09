package gov.cms.dpc.attribution.resources.v1;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.cms.dpc.attribution.AbstractAttributionTest;
import gov.cms.dpc.attribution.AttributionTestHelpers;
import gov.cms.dpc.common.entities.TokenEntity;
import org.apache.commons.lang3.tuple.Pair;
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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
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

        final Pair<Organization, String> pair = createOrganization();
        final String org_id = pair.getLeft().getIdElement().getIdPart();
        final String macaroon = pair.getRight();

        // Verify that it's correct.
        try (final CloseableHttpClient client = HttpClients.createDefault()) {
            final HttpGet httpGet = new HttpGet(getServerURL() + String.format("/Token/%s/verify?token=%s", org_id, macaroon));

            try (CloseableHttpResponse response = client.execute(httpGet)) {
                assertEquals(HttpStatus.OK_200, response.getStatusLine().getStatusCode(), "Token should be valid");
            }
        }

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

        final Pair<Organization, String> pair = createOrganization();
        final String orgID = pair.getLeft().getIdElement().getIdPart();

        final List<TokenEntity> tokens = fetchTokens(orgID);
        assertFalse(tokens.isEmpty(), "Should have tokens");
        final TokenEntity token = tokens.get(0);

        assertAll(() -> assertEquals(String.format("Token for organization %s.", orgID), token.getLabel(), "Should have auto-generated label"),
                () -> assertEquals(LocalDate.now().plus(1, ChronoUnit.YEARS), token.getExpiresAt().toLocalDate(), "Should expire in 1 year"));
    }

    @Test
    void testTokenCustomLabel() throws IOException {

        final Pair<Organization, String> pair = createOrganization();
        final String orgID = pair.getLeft().getIdElement().getIdPart();
        // List the tokens

        final String customLabel = "custom token label";
        // Create a new token with a custom label
        try (final CloseableHttpClient client = HttpClients.createDefault()) {
            final HttpPost httpPost = new HttpPost(getServerURL() + String.format("/Token/%s?label=%s", orgID, URLEncoder.encode(customLabel, StandardCharsets.UTF_8)));
            try (CloseableHttpResponse response = client.execute(httpPost)) {
                assertEquals(HttpStatus.OK_200, response.getStatusLine().getStatusCode(), "Should have found organization");
            }
        }

        // List the tokens
        final List<TokenEntity> tokens = fetchTokens(orgID);
        assertEquals(1, tokens
                .stream()
                .filter(token -> token.getLabel().equals(customLabel))
                .count(), "Should have token with custom label");
    }

    @Test
    void testTokenCustomExpiration() throws IOException {
        final Pair<Organization, String> pair = createOrganization();
        final String orgID = pair.getLeft().getIdElement().getIdPart();

        // Create a new token with an expiration greater than 1 year
        // Should fail
        OffsetDateTime expires = OffsetDateTime.now(ZoneOffset.UTC).plusYears(5);
        try (final CloseableHttpClient client = HttpClients.createDefault()) {
            final HttpPost httpPost = new HttpPost(getServerURL() + String.format("/Token/%s?expiration=%s", orgID, expires.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)));
            try (CloseableHttpResponse response = client.execute(httpPost)) {
                assertEquals(HttpStatus.BAD_REQUEST_400, response.getStatusLine().getStatusCode(), "Should fail with exceeding expiration time");
            }
        }

        expires = OffsetDateTime.now(ZoneOffset.UTC).minusDays(10);
        try (final CloseableHttpClient client = HttpClients.createDefault()) {
            final HttpPost httpPost = new HttpPost(getServerURL() + String.format("/Token/%s?expiration=%s", orgID, expires.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)));
            try (CloseableHttpResponse response = client.execute(httpPost)) {
                assertEquals(HttpStatus.BAD_REQUEST_400, response.getStatusLine().getStatusCode(), "Should fail with past expiration time");
            }
        }

        final OffsetDateTime expiresFinal = OffsetDateTime.now(ZoneOffset.UTC).plusDays(10);
        try (final CloseableHttpClient client = HttpClients.createDefault()) {
            final HttpPost httpPost = new HttpPost(getServerURL() + String.format("/Token/%s?expiration=%s", orgID, expiresFinal.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)));
            try (CloseableHttpResponse response = client.execute(httpPost)) {
                assertEquals(HttpStatus.OK_200, response.getStatusLine().getStatusCode(), "Should have been created.");
            }
        }

        final List<TokenEntity> tokens = fetchTokens(orgID);
        assertEquals(1, tokens.stream().filter(token -> token.getExpiresAt().toLocalDate().equals(expiresFinal.toLocalDate())).count(), "Should have 1 token with matching expiration");
    }

    Pair<Organization, String> createOrganization() throws IOException {
        final Organization organization = AttributionTestHelpers.createOrganization(ctx, getServerURL());
        final String orgID = organization.getIdElement().getIdPart();
        final String macaroon = generateToken(orgID);
        return Pair.of(organization, macaroon);
    }

    String generateToken(String orgID) throws IOException {

        try (final CloseableHttpClient client = HttpClients.createDefault()) {
            final HttpPost httpPost = new HttpPost(getServerURL() + String.format("/Token/%s", orgID));

            try (CloseableHttpResponse response = client.execute(httpPost)) {
                assertEquals(HttpStatus.OK_200, response.getStatusLine().getStatusCode(), "Should have found organization");
                final String macaroon = EntityUtils.toString(response.getEntity());
                // Verify that the first few bytes are correct, to ensure we encoded correctly.
                assertTrue(macaroon.startsWith("eyJ2IjoyLCJs"), "Should have correct starting string value");
                return macaroon;
            }
        }
    }

    List<TokenEntity> fetchTokens(String orgID) throws IOException {
        try (final CloseableHttpClient client = HttpClients.createDefault()) {
            final HttpGet httpGet = new HttpGet(getServerURL() + String.format("/Token/%s", orgID));

            try (CloseableHttpResponse response = client.execute(httpGet)) {
                return this.mapper.readValue(response.getEntity().getContent(), new TypeReference<List<TokenEntity>>() {
                });
            }
        }
    }
}
