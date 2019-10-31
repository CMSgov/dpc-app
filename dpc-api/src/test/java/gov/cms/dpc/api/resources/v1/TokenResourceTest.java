package gov.cms.dpc.api.resources.v1;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.cms.dpc.api.AbstractSecureApplicationTest;
import gov.cms.dpc.api.entities.TokenEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
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
import java.util.List;

import static gov.cms.dpc.api.APITestHelpers.ORGANIZATION_ID;
import static org.junit.jupiter.api.Assertions.*;

class TokenResourceTest extends AbstractSecureApplicationTest {

    private final ObjectMapper mapper;

    private TokenResourceTest() {
        this.mapper = new ObjectMapper();
    }

    @Test
    void testTokenList() throws IOException {

        final List<TokenEntity> tokens = fetchTokens(ORGANIZATION_ID);
        assertFalse(tokens.isEmpty(), "Should have tokens");
        final TokenEntity token = tokens.get(0);

        assertAll(() -> assertEquals(String.format("Token for organization %s.", ORGANIZATION_ID), token.getLabel(), "Should have auto-generated label"),
                () -> assertEquals(LocalDate.now().plus(1, ChronoUnit.YEARS), token.getExpiresAt().toLocalDate(), "Should expire in 1 year"),
                () -> assertEquals(LocalDate.now(), token.getCreatedAt().toLocalDate(), "Should be created today"),
                () -> assertNull(token.getToken(), "Should not have token field"));

        // Check to see if a direct fetch works as well
        try (final CloseableHttpClient client = HttpClients.createDefault()) {
            final HttpGet httpGet = new HttpGet(getBaseURL() + String.format("/Token/%s", token.getId()));
            httpGet.addHeader(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", ORGANIZATION_TOKEN));

            try (CloseableHttpResponse response = client.execute(httpGet)) {
                assertEquals(HttpStatus.OK_200, response.getStatusLine().getStatusCode(), "Should have succeeded");
                final TokenEntity singleEntity = this.mapper.readValue(response.getEntity().getContent(), TokenEntity.class);
                assertEquals(token, singleEntity, "Should be the same");
            }
        }
    }

    @Test
    void testTokenCustomLabel() throws IOException {
        // List the tokens

        final String customLabel = "custom token label";
        // Create a new token with a custom label
        try (final CloseableHttpClient client = HttpClients.createDefault()) {
            final HttpPost httpPost = new HttpPost(getBaseURL() + String.format("/Token?label=%s", URLEncoder.encode(customLabel, StandardCharsets.UTF_8)));
            httpPost.addHeader(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", ORGANIZATION_TOKEN));
            try (CloseableHttpResponse response = client.execute(httpPost)) {
                assertEquals(HttpStatus.OK_200, response.getStatusLine().getStatusCode(), "Should have found organization");
                final TokenEntity tokenEntity = this.mapper.readValue(response.getEntity().getContent(), TokenEntity.class);
                assertAll(() -> assertEquals(customLabel, tokenEntity.getLabel(), "Should have correct label"),
                        () -> assertNotNull(tokenEntity.getToken(), "Should have generated token"));
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
            final HttpPost httpPost = new HttpPost(getBaseURL() + String.format("/Token?expiration=%s", expires.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)));
            httpPost.addHeader(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", ORGANIZATION_TOKEN));
            try (CloseableHttpResponse response = client.execute(httpPost)) {
                assertEquals(HttpStatus.BAD_REQUEST_400, response.getStatusLine().getStatusCode(), "Should fail with exceeding expiration time");
            }
        }

        expires = OffsetDateTime.now(ZoneOffset.UTC).minusDays(10);
        try (final CloseableHttpClient client = HttpClients.createDefault()) {
            final HttpPost httpPost = new HttpPost(getBaseURL() + String.format("/Token?expiration=%s", expires.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)));
            httpPost.addHeader(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", ORGANIZATION_TOKEN));
            try (CloseableHttpResponse response = client.execute(httpPost)) {
                assertEquals(HttpStatus.BAD_REQUEST_400, response.getStatusLine().getStatusCode(), "Should fail with past expiration time");
            }
        }

        final OffsetDateTime expiresFinal = OffsetDateTime.now(ZoneOffset.UTC).plusDays(10);
        try (final CloseableHttpClient client = HttpClients.createDefault()) {
            final HttpPost httpPost = new HttpPost(getBaseURL() + String.format("/Token?expiration=%s", expiresFinal.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)));
            httpPost.addHeader(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", ORGANIZATION_TOKEN));
            try (CloseableHttpResponse response = client.execute(httpPost)) {
                assertEquals(HttpStatus.OK_200, response.getStatusLine().getStatusCode(), "Should have been created.");
            }
        }

        final List<TokenEntity> tokens = fetchTokens(ORGANIZATION_ID);
        assertEquals(1, tokens.stream().filter(token -> token.getExpiresAt().toLocalDate().equals(expiresFinal.toLocalDate())).count(), "Should have 1 token with matching expiration");
    }

    @Test
    void testTokenDeletion() throws IOException {
        // Get all the tokens
        final List<TokenEntity> tokens = fetchTokens(ORGANIZATION_ID);
        assertFalse(tokens.isEmpty(), "Should have tokens");
        final TokenEntity token = tokens.get(0);

        // Remove the token
        try (final CloseableHttpClient client = HttpClients.createDefault()) {
            final HttpDelete httpDelete = new HttpDelete(getBaseURL() + String.format("/Token/%s", token.getId()));
            httpDelete.addHeader(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", ORGANIZATION_TOKEN));
            try (CloseableHttpResponse response = client.execute(httpDelete)) {
                assertEquals(HttpStatus.OK_200, response.getStatusLine().getStatusCode(), "Should have succeeded");
            }


            // Fetching token should throw an exception
            final HttpGet httpGet = new HttpGet(getBaseURL() + "/Token");
            httpGet.addHeader(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", ORGANIZATION_TOKEN));

            try (CloseableHttpResponse response = client.execute(httpGet)) {
                assertEquals(HttpStatus.UNAUTHORIZED_401, response.getStatusLine().getStatusCode(), "Should be unauthorized");
            }
        }
    }

    List<TokenEntity> fetchTokens(String orgID) throws IOException {
        try (final CloseableHttpClient client = HttpClients.createDefault()) {
            final HttpGet httpGet = new HttpGet(getBaseURL() + "/Token");
            httpGet.addHeader(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", ORGANIZATION_TOKEN));

            try (CloseableHttpResponse response = client.execute(httpGet)) {
                return this.mapper.readValue(response.getEntity().getContent(), new TypeReference<List<TokenEntity>>() {
                });
            }
        }
    }
}
