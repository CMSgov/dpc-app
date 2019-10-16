package gov.cms.dpc.api.resources.v1;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.cms.dpc.api.AbstractSecureApplicationTest;
import gov.cms.dpc.api.entities.TokenEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.eclipse.jetty.http.HttpStatus;
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

import static gov.cms.dpc.api.APITestHelpers.ORGANIZATION_ID;
import static org.junit.jupiter.api.Assertions.*;

class TokenResourceTest extends AbstractSecureApplicationTest {

    private static final String BAD_ORG_ID = "0c527d2e-2e8a-4808-b11d-0fa06baf8252";

    private final ObjectMapper mapper;

    private TokenResourceTest() {
        this.mapper = new ObjectMapper();
    }

    @Test
    void testUnknownOrgTokenGeneration() throws IOException {
        try (final CloseableHttpClient client = HttpClients.createDefault()) {
            final HttpPost httpPost = new HttpPost(getBaseURL() + "/Token/" + UUID.randomUUID().toString());
            httpPost.addHeader(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", ORGANIZATION_TOKEN));

            try (CloseableHttpResponse response = client.execute(httpPost)) {
                assertEquals(HttpStatus.UNAUTHORIZED_401, response.getStatusLine().getStatusCode(), "Should not have found organization");
            }
        }
    }

    @Test
    void testTokenList() throws IOException {

        final List<TokenEntity> tokens = fetchTokens(ORGANIZATION_ID);
        assertFalse(tokens.isEmpty(), "Should have tokens");
        final TokenEntity token = tokens.get(0);

        assertAll(() -> assertEquals(String.format("Token for organization %s.", ORGANIZATION_ID), token.getLabel(), "Should have auto-generated label"),
                () -> assertEquals(LocalDate.now().plus(1, ChronoUnit.YEARS), token.getExpiresAt().toLocalDate(), "Should expire in 1 year"));
    }

    @Test
    void testTokenCustomLabel() throws IOException {
        // List the tokens

        final String customLabel = "custom token label";
        // Create a new token with a custom label
        try (final CloseableHttpClient client = HttpClients.createDefault()) {
            final HttpPost httpPost = new HttpPost(getBaseURL() + String.format("/Token/%s?label=%s", ORGANIZATION_ID, URLEncoder.encode(customLabel, StandardCharsets.UTF_8)));
            httpPost.addHeader(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", ORGANIZATION_TOKEN));
            try (CloseableHttpResponse response = client.execute(httpPost)) {
                assertEquals(HttpStatus.OK_200, response.getStatusLine().getStatusCode(), "Should have found organization");
            }
        }

        // List the tokens
        final List<TokenEntity> tokens = fetchTokens(ORGANIZATION_ID);
        assertEquals(1, tokens
                .stream()
                .filter(token -> token.getLabel().equals(customLabel))
                .count(), "Should have token with custom label");
    }

    @Test
    void testTokenCustomExpiration() throws IOException {

        // Create a new token with an expiration greater than 1 year
        // Should fail
        OffsetDateTime expires = OffsetDateTime.now(ZoneOffset.UTC).plusYears(5);
        try (final CloseableHttpClient client = HttpClients.createDefault()) {
            final HttpPost httpPost = new HttpPost(getBaseURL() + String.format("/Token/%s?expiration=%s", ORGANIZATION_ID, expires.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)));
            httpPost.addHeader(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", ORGANIZATION_TOKEN));
            try (CloseableHttpResponse response = client.execute(httpPost)) {
                assertEquals(HttpStatus.BAD_REQUEST_400, response.getStatusLine().getStatusCode(), "Should fail with exceeding expiration time");
            }
        }

        expires = OffsetDateTime.now(ZoneOffset.UTC).minusDays(10);
        try (final CloseableHttpClient client = HttpClients.createDefault()) {
            final HttpPost httpPost = new HttpPost(getBaseURL() + String.format("/Token/%s?expiration=%s", ORGANIZATION_ID, expires.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)));
            httpPost.addHeader(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", ORGANIZATION_TOKEN));
            try (CloseableHttpResponse response = client.execute(httpPost)) {
                assertEquals(HttpStatus.BAD_REQUEST_400, response.getStatusLine().getStatusCode(), "Should fail with past expiration time");
            }
        }

        final OffsetDateTime expiresFinal = OffsetDateTime.now(ZoneOffset.UTC).plusDays(10);
        try (final CloseableHttpClient client = HttpClients.createDefault()) {
            final HttpPost httpPost = new HttpPost(getBaseURL() + String.format("/Token/%s?expiration=%s", ORGANIZATION_ID, expiresFinal.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)));
            httpPost.addHeader(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", ORGANIZATION_TOKEN));
            try (CloseableHttpResponse response = client.execute(httpPost)) {
                assertEquals(HttpStatus.OK_200, response.getStatusLine().getStatusCode(), "Should have been created.");
            }
        }

        final List<TokenEntity> tokens = fetchTokens(ORGANIZATION_ID);
        assertEquals(1, tokens.stream().filter(token -> token.getExpiresAt().toLocalDate().equals(expiresFinal.toLocalDate())).count(), "Should have 1 token with matching expiration");
    }

    String generateToken(String orgID) throws IOException {

        try (final CloseableHttpClient client = HttpClients.createDefault()) {
            final HttpPost httpPost = new HttpPost(getBaseURL() + String.format("/Token/%s", orgID));
            httpPost.setHeader(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", ORGANIZATION_TOKEN));

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
            final HttpGet httpGet = new HttpGet(getBaseURL() + String.format("/Token/%s", orgID));
            httpGet.addHeader(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", ORGANIZATION_TOKEN));

            try (CloseableHttpResponse response = client.execute(httpGet)) {
                return this.mapper.readValue(response.getEntity().getContent(), new TypeReference<List<TokenEntity>>() {
                });
            }
        }
    }
}
