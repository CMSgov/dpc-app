package gov.cms.dpc.api.resources.v1;

import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.cms.dpc.api.APITestHelpers;
import gov.cms.dpc.api.AbstractSecureApplicationTest;
import gov.cms.dpc.api.entities.TokenEntity;
import gov.cms.dpc.api.models.CollectionResponse;
import gov.cms.dpc.fhir.FHIRFormatters;
import gov.cms.dpc.fhir.helpers.FHIRHelpers;
import gov.cms.dpc.testing.APIAuthHelpers;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

import javax.inject.Inject;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

import static gov.cms.dpc.api.APITestHelpers.ORGANIZATION_ID;
import static org.junit.jupiter.api.Assertions.*;

class TokenResourceTest extends AbstractSecureApplicationTest {

    private final ObjectMapper mapper;
    private String fullyAuthedToken;

    @Inject
    private TokenResourceTest() {
        this.mapper = new ObjectMapper();

        try {
            this.fullyAuthedToken = APIAuthHelpers.jwtAuthFlow(getBaseURL(), ORGANIZATION_TOKEN, PUBLIC_KEY_ID, PRIVATE_KEY).accessToken;
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @BeforeEach
    public void beforeEachTest() throws IOException {
        String decodedToken = new String(Base64.getDecoder().decode(ORGANIZATION_TOKEN));
        String orgTokenId = decodedToken.substring(decodedToken.indexOf("\"i\":\"")+5,decodedToken.indexOf("\",\"c\":"));

        final CollectionResponse<TokenEntity> tokens = fetchTokens(ORGANIZATION_ID, this.fullyAuthedToken);

        List<TokenEntity> tokensToBeDeleted = tokens.getEntities().stream()
                .filter(token -> !token.getId().equals(orgTokenId))
                .collect(Collectors.toList());

        tokensToBeDeleted.stream().forEach(token -> deleteToken(token.getId()));
    }

    @Test
    void testTokenList() throws IOException {

        final CollectionResponse<TokenEntity> tokens = fetchTokens(ORGANIZATION_ID, this.fullyAuthedToken);
        assertFalse(tokens.getEntities().isEmpty(), "Should have at least one token");
        assertEquals(LocalDate.now(ZoneOffset.UTC), tokens.getCreatedAt().atZoneSameInstant(ZoneOffset.UTC).toLocalDate(), "Should have created date");
        final TokenEntity token = ((List<TokenEntity>) tokens.getEntities()).get(0);

        assertAll(() -> assertEquals(String.format("Token for organization %s.", ORGANIZATION_ID), token.getLabel(), "Should have auto-generated label"),
                () -> assertEquals(LocalDate.now().plus(1, ChronoUnit.YEARS), token.getExpiresAt().toLocalDate(), "Should expire in 1 year"),
                () -> assertEquals(LocalDate.now(), token.getCreatedAt().toLocalDate(), "Should be created today"),
                () -> assertNull(token.getToken(), "Should not have token field"));

        // Check to see if a direct fetch works as well
        try (final CloseableHttpClient client = HttpClients.createDefault()) {
            final HttpGet httpGet = new HttpGet(getBaseURL() + String.format("/Token/%s", token.getId()));
            httpGet.addHeader(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", this.fullyAuthedToken));

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
            httpPost.addHeader(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", this.fullyAuthedToken));
            try (CloseableHttpResponse response = client.execute(httpPost)) {
                assertEquals(HttpStatus.OK_200, response.getStatusLine().getStatusCode(), "Should have found organization");
                final TokenEntity tokenEntity = this.mapper.readValue(response.getEntity().getContent(), TokenEntity.class);
                assertAll(() -> assertEquals(customLabel, tokenEntity.getLabel(), "Should have correct label"),
                        () -> assertNotNull(tokenEntity.getToken(), "Should have generated token"));
            }
        }

        // List the tokens
        final CollectionResponse<TokenEntity> tokens = fetchTokens(ORGANIZATION_ID, this.fullyAuthedToken);
        assertEquals(1, tokens
                .getEntities()
                .stream()
                .filter(token -> token.getLabel().equals(customLabel))
                .count(), "Should have token with custom label");
    }

    @Test
    void testTokenCreationWithExceedingExpirationTime() throws IOException {
        // Create a new token with an expiration greater than 1 year
        final OffsetDateTime expires = OffsetDateTime.now(ZoneOffset.UTC).plusYears(5);
        try (final CloseableHttpClient client = HttpClients.createDefault()) {
            final HttpPost httpPost = new HttpPost(getBaseURL() + String.format("/Token?expiration=%s", expires.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)));
            httpPost.addHeader(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", this.fullyAuthedToken));
            try (CloseableHttpResponse response = client.execute(httpPost)) {
                assertEquals(HttpStatus.BAD_REQUEST_400, response.getStatusLine().getStatusCode(), "Should fail with exceeding expiration time");
            }
        }
    }

    @Test
    void testTokenCreationWithPastExpirationTime() throws IOException {
        final OffsetDateTime expires = OffsetDateTime.now(ZoneOffset.UTC).minusDays(10);
        try (final CloseableHttpClient client = HttpClients.createDefault()) {
            final HttpPost httpPost = new HttpPost(getBaseURL() + String.format("/Token?expiration=%s", expires.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)));
            httpPost.addHeader(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", this.fullyAuthedToken));
            try (CloseableHttpResponse response = client.execute(httpPost)) {
                assertEquals(HttpStatus.BAD_REQUEST_400, response.getStatusLine().getStatusCode(), "Should fail with past expiration time");
            }
        }
    }



    @Test
    void testTokenCreationWithValidCustomExpiration() throws IOException {
        final OffsetDateTime expiresFinal = OffsetDateTime.now(ZoneOffset.UTC).plusDays(10);
        try (final CloseableHttpClient client = HttpClients.createDefault()) {
            final HttpPost httpPost = new HttpPost(getBaseURL() + String.format("/Token?expiration=%s", expiresFinal.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)));
            httpPost.addHeader(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", this.fullyAuthedToken));
            try (CloseableHttpResponse response = client.execute(httpPost)) {
                assertEquals(HttpStatus.OK_200, response.getStatusLine().getStatusCode(), "Should have been created.");
            }
        }

        final CollectionResponse<TokenEntity> tokens = fetchTokens(ORGANIZATION_ID, this.fullyAuthedToken);
        assertEquals(1, tokens.getEntities().stream().filter(token -> token.getExpiresAt().atZoneSameInstant(ZoneOffset.UTC).toLocalDate().equals(expiresFinal.toLocalDate())).count(), "Should have 1 token with matching expiration");
    }

    @Test
    void testTokenCreationWithDefaults() throws IOException {
        try (final CloseableHttpClient client = HttpClients.createDefault()) {
            final HttpPost httpPost = new HttpPost(getBaseURL() + String.format("/Token"));
            httpPost.addHeader(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", this.fullyAuthedToken));

            String newTokenId;
            try (CloseableHttpResponse response = client.execute(httpPost)) {
                assertEquals(HttpStatus.OK_200, response.getStatusLine().getStatusCode(), "Should have been created.");
                final TokenEntity tokenReturned = mapper.readValue(response.getEntity().getContent(), TokenEntity.class);
                assertNotNull(tokenReturned, "Should have contained a response body");
                assertEquals(tokenReturned.getLabel(), String.format("Token for organization %s.", ORGANIZATION_ID), "Returned token should have default label");
                assertNotNull(tokenReturned.getCreatedAt(), "Returned token should have a non-empty createdAt value");
                assertNotNull(tokenReturned.getExpiresAt(), "Returned token should have a non-empty expiredAt value");
                assertFalse(StringUtils.isBlank(tokenReturned.getToken()), "Returned token should not have a blank token value.");
                assertFalse(StringUtils.isBlank(tokenReturned.getId()), "Returned token should not have a blank ID value");
                newTokenId = tokenReturned.getId();
            }

            final HttpGet httpGet = new HttpGet(getBaseURL() + String.format("/Token/%s", newTokenId));
            httpGet.addHeader(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", this.fullyAuthedToken));

            try (CloseableHttpResponse response = client.execute(httpGet)) {
                assertEquals(HttpStatus.OK_200, response.getStatusLine().getStatusCode(), "Should have succeeded");
                final TokenEntity tokenReturned = this.mapper.readValue(response.getEntity().getContent(), TokenEntity.class);
                assertNotNull(tokenReturned, "Should have contained a response body");
                assertEquals(tokenReturned.getLabel(), String.format("Token for organization %s.", ORGANIZATION_ID), "Returned token should have default label");
                assertNotNull(tokenReturned.getCreatedAt(), "Returned token should have a non-empty createdAt value");
                assertNotNull(tokenReturned.getExpiresAt(), "Returned token should have a non-empty expiredAt value");
                assertTrue(StringUtils.isBlank(tokenReturned.getToken()), "Returned token should not have a token value (Value is returned only during creation).");
                assertFalse(StringUtils.isBlank(tokenReturned.getId()), "Returned token should not have a blank ID value");
            }
        }
    }

    @Test
    void testTokenCreationUsingBodyParams() throws IOException {
        final OffsetDateTime expiresFinal = OffsetDateTime.now(ZoneOffset.UTC).plusDays(10);
        final String expiresFormatted =  FHIRFormatters.INSTANT_FORMATTER.format(expiresFinal);
        try (final CloseableHttpClient client = HttpClients.createDefault()) {
            final HttpPost httpPost = new HttpPost(getBaseURL() + "/Token");
            httpPost.addHeader(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", this.fullyAuthedToken));
            httpPost.setEntity(new StringEntity("{\"expiresAt\":\""+expiresFormatted+"\",\"label\":\"custom-label\"}"));
            httpPost.addHeader(HttpHeaders.CONTENT_TYPE, "application/json");
            try (CloseableHttpResponse response = client.execute(httpPost)) {
                assertEquals(HttpStatus.OK_200, response.getStatusLine().getStatusCode(), "Should have been created.");
            }
        }

        final CollectionResponse<TokenEntity> tokens = fetchTokens(ORGANIZATION_ID, this.fullyAuthedToken);
        assertEquals(1, tokens.getEntities().stream()
                .filter(token -> token.getExpiresAt().atZoneSameInstant(ZoneOffset.UTC).toLocalDate().equals(expiresFinal.toLocalDate()))
                .filter(token -> token.getLabel().equals("custom-label")).count(), "Should have 1 token with matching expiration and custom label");
    }

    @Test
    void testNoHtmlInBodyLabel() throws IOException {
        final OffsetDateTime expiresFinal = OffsetDateTime.now(ZoneOffset.UTC).plusDays(10);
        final String expiresFormatted =  FHIRFormatters.INSTANT_FORMATTER.format(expiresFinal);
        try (final CloseableHttpClient client = HttpClients.createDefault()) {
            final HttpPost httpPost = new HttpPost(getBaseURL() + "/Token");
            httpPost.addHeader(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", this.fullyAuthedToken));
            httpPost.setEntity(new StringEntity("{\"expiresAt\":\""+expiresFormatted+"\",\"label\":\"Ok Text<script>alert('Bad script')</script>\"}"));
            httpPost.addHeader(HttpHeaders.CONTENT_TYPE, "application/json");
            try (CloseableHttpResponse response = client.execute(httpPost)) {
                assertEquals(HttpStatus.UNPROCESSABLE_ENTITY_422, response.getStatusLine().getStatusCode(), "422 Should have been returned due to html/script tags");
            }
        }
    }

    @Test
    void testTokenCreationWithInvalidExpirationBodyParam() throws IOException {
        final String expiresFormatted =  "invalid-date-time-format-ZZZ";
        try (final CloseableHttpClient client = HttpClients.createDefault()) {
            final HttpPost httpPost = new HttpPost(getBaseURL() + "/Token");
            httpPost.addHeader(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", this.fullyAuthedToken));
            httpPost.setEntity(new StringEntity("{\"expiresAt\":\""+expiresFormatted+"\",\"label\":\"custom-label\"}"));
            httpPost.addHeader(HttpHeaders.CONTENT_TYPE, "application/json");
            try (CloseableHttpResponse response = client.execute(httpPost)) {
                assertEquals(HttpStatus.BAD_REQUEST_400, response.getStatusLine().getStatusCode(), "Should have been created.");
            }
        }
    }



    @Test
    void testTokenDeletion() throws IOException {
        //Create a token
        TokenEntity token = createToken();

        //Ensure it was crated and persisted
        CollectionResponse<TokenEntity> tokens = fetchTokens(ORGANIZATION_ID, this.fullyAuthedToken);
        assertFalse(tokens.getEntities().isEmpty(), "Should have a token");
        List<TokenEntity> tokenList = tokens.getEntities().stream().filter(t -> t.getId().equals(token.getId())).collect(Collectors.toList());
        assertTrue(tokenList.size() == 1 , "There should exist exactly 1 token with matching token ID");

        //Delete it
        try (final CloseableHttpClient client = HttpClients.createDefault()) {
            final HttpDelete httpDelete = new HttpDelete(getBaseURL() + String.format("/Token/%s", token.getId()));
            httpDelete.addHeader(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", this.fullyAuthedToken));
            try (CloseableHttpResponse response = client.execute(httpDelete)) {
                assertEquals(HttpStatus.NO_CONTENT_204, response.getStatusLine().getStatusCode(), "Should have succeeded with a 204 (No Content)");
                assertNull(response.getEntity(), "Response should not have an entity (empty body)");
            }
        }

        //Ensure it is no longer returned when listing tokens
        tokens = fetchTokens(ORGANIZATION_ID, this.fullyAuthedToken);
        tokenList = tokens.getEntities().stream().filter(t -> t.getId().equals(token.getId())).collect(Collectors.toList());
        assertTrue(tokenList.size() == 0 , "There should exist exactly 0 tokens with matching token ID");

        //Ensure it is not longer returned when fetched by id
        final HttpGet httpGet = new HttpGet(getBaseURL() + "/Token/"+token.getId());
        httpGet.addHeader(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", this.fullyAuthedToken));
        try (final CloseableHttpClient client = HttpClients.createDefault();CloseableHttpResponse response = client.execute(httpGet)) {
            assertEquals(HttpStatus.NOT_FOUND_404, response.getStatusLine().getStatusCode(), "Should not have been found (Expected 404)");
        }
    }

    private TokenEntity createToken() throws IOException {
        try (final CloseableHttpClient client = HttpClients.createDefault()) {
            final HttpPost httpPost = new HttpPost(getBaseURL() + "/Token");
            httpPost.addHeader(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", this.fullyAuthedToken));
            try (CloseableHttpResponse response = client.execute(httpPost)) {
                return this.mapper.readValue(response.getEntity().getContent(), TokenEntity.class);
            }
        }
    }

    private boolean deleteToken(String tokenId){
        try (final CloseableHttpClient client = HttpClients.createDefault()) {
            final HttpDelete httpDelete = new HttpDelete(getBaseURL() + String.format("/Token/%s", tokenId));
            httpDelete.addHeader(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", this.fullyAuthedToken));
            try (CloseableHttpResponse response = client.execute(httpDelete)) {
                return response.getStatusLine().getStatusCode() == 204;
            }
        }catch (IOException e){
            e.printStackTrace();
            return false;
        }
    }

    @Test
    void testTokenSigning() throws IOException, URISyntaxException {
        final IParser parser = ctx.newJsonParser();
        final IGenericClient attrClient = APITestHelpers.buildAttributionClient(ctx);
        // Create a new org and make sure it has no providers
        final String m2 = FHIRHelpers.registerOrganization(attrClient, parser, OTHER_ORG_ID, "1112111111", getAdminURL());

        // Create a new JWT
        final APIAuthHelpers.AuthResponse authResponse = APIAuthHelpers.jwtAuthFlow(this.getBaseURL(), fullyAuthedToken, PUBLIC_KEY_ID, PRIVATE_KEY);
        assertAll(() -> assertNotEquals("", authResponse.accessToken, "Should have token"),
                () -> assertEquals(300, authResponse.expiresIn, "Should be valid for 300 seconds"),
                () -> assertEquals("system/*.*", authResponse.scope, "Should have correct scope"),
                () -> assertEquals("bearer", authResponse.tokenType, "Should be a macaroon"));

        // Try to authenticate using the private key for org 1 and the token for org 2, should throw an exception, but in the auth handler
        final AssertionFailedError error = assertThrows(AssertionFailedError.class, () -> APIAuthHelpers.jwtAuthFlow(this.getBaseURL(), m2, PUBLIC_KEY_ID, PRIVATE_KEY));
        error.getMessage();
    }

    CollectionResponse<TokenEntity> fetchTokens(String orgID, String token) throws IOException {
        try (final CloseableHttpClient client = HttpClients.createDefault()) {
            final HttpGet httpGet = new HttpGet(getBaseURL() + "/Token");
            httpGet.addHeader(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token));

            try (CloseableHttpResponse response = client.execute(httpGet)) {
                return this.mapper.readValue(response.getEntity().getContent(), new TypeReference<CollectionResponse<TokenEntity>>() {
                });
            }
        }
    }
}
