package gov.cms.dpc.testing;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IClientInterceptor;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.IHttpRequest;
import ca.uhn.fhir.rest.client.api.IHttpResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.nitram509.jmacaroons.MacaroonVersion;
import com.github.nitram509.jmacaroons.MacaroonsBuilder;
import com.google.common.net.HttpHeaders;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.eclipse.jetty.http.HttpStatus;

import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.sql.Date;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class APIAuthHelpers {
    public static final String TASK_URL = "http://localhost:9900/tasks/";
    private static final String CLIENT_ASSERTION_TYPE = "urn:ietf:params:oauth:client-assertion-type:jwt-bearer";
    private static final ObjectMapper mapper = new ObjectMapper();

    public static IGenericClient buildAuthenticatedClient(FhirContext ctx, String baseURL, String macaroon, String keyID, PrivateKey privateKey) throws IOException, URISyntaxException {


        final AuthResponse authResponse = jwtAuthFlow(baseURL, macaroon, keyID, privateKey);
        // Request an access token from the JWT endpoint
        final IGenericClient client = ctx.newRestfulGenericClient(baseURL);
        client.registerInterceptor(new MacaroonsInterceptor(authResponse.accessToken));

        return client;
    }

    public static AuthResponse jwtAuthFlow(String baseURL, String macaroon, String keyID, PrivateKey privateKey) throws IOException, URISyntaxException {
        final String jwt = Jwts.builder()
                .setHeaderParam("kid", keyID)
                .setAudience(String.format("%sToken/auth", baseURL))
                .setIssuer(macaroon)
                .setSubject(macaroon)
                .setId(UUID.randomUUID().toString())
                .setExpiration(Date.from(Instant.now().plus(5, ChronoUnit.MINUTES)))
                .signWith(privateKey, SignatureAlgorithm.RS384)
                .compact();

        // Submit JWT to /auth endpoint
        final AuthResponse authResponse;
        try (final CloseableHttpClient client = HttpClients.createDefault()) {
            final URIBuilder builder = new URIBuilder(String.format("%s/Token/auth", baseURL));
            builder.addParameter("scope", "system/*:*");
            builder.addParameter("grant_type", "client_credentials");
            builder.addParameter("client_assertion_type", CLIENT_ASSERTION_TYPE);
            builder.addParameter("client_assertion", jwt);
            final HttpPost post = new HttpPost(builder.build());
            post.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED);

            try (CloseableHttpResponse response = client.execute(post)) {
                assertEquals(HttpStatus.OK_200, response.getStatusLine().getStatusCode(), "Token request should have succeeded");
                authResponse = mapper.readValue(response.getEntity().getContent(), AuthResponse.class);
                assertNotEquals(macaroon, authResponse.accessToken, "New Macaroon should not be identical");
                assertEquals(300, authResponse.expiresIn, "Should expire in 300 seconds");
            }
        }
        return authResponse;
    }

    public static String createGoldenMacaroon() throws IOException {

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            final HttpPost post = new HttpPost(String.format("%s/generate-token", TASK_URL));

            try (CloseableHttpResponse execute = client.execute(post)) {
                assertEquals(HttpStatus.OK_200, execute.getStatusLine().getStatusCode(), "Generated macaroon");
                return EntityUtils.toString(execute.getEntity());
            }
        }
    }

    public static KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        final KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        return kpg.generateKeyPair();
    }

    public static String generatePublicKey(PublicKey key) {
        final String encoded = Base64.getMimeEncoder().encodeToString(key.getEncoded());
        return String.format("-----BEGIN PUBLIC KEY-----\n%s\n-----END PUBLIC KEY-----\n", encoded);
    }

    /**
     * Generate a new {@link KeyPair} and submit the {@link PublicKey} to the API service, along with the given label
     *
     * @param keyID          - {@link String} identifier (kid) of the public key
     * @param organizationID - {@link String} organization ID to register key with
     * @param goldenMacaroon - {@link String} admin Macaroon that can upload keys
     * @param baseURL        - {@link String} baseURl to submit Key to
     * @return - {@link PrivateKey} which matches uploaded {@link PublicKey}
     * @throws IOException              - throws if something bad happens
     * @throws URISyntaxException       - throws if the URI is no good
     * @throws NoSuchAlgorithmException - throws if security breaks
     */
    public static PrivateKey generateAndUploadKey(String keyID, String organizationID, String goldenMacaroon, String baseURL) throws IOException, URISyntaxException, NoSuchAlgorithmException {
        final KeyPair keyPair = generateKeyPair();
        final String key = generatePublicKey(keyPair.getPublic());

        // Create org specific macaroon from Golden Macaroon
        // Base64 decode the Macaroon
        final String decoded = new String(Base64.getUrlDecoder().decode(goldenMacaroon), StandardCharsets.UTF_8);
        final String macaroon = MacaroonsBuilder
                .modify(MacaroonsBuilder.deserialize(decoded).get(0))
                .add_first_party_caveat(String.format("organization_id = %s", organizationID))
                .getMacaroon().serialize(MacaroonVersion.SerializationVersion.V2_JSON);

        try (final CloseableHttpClient client = HttpClients.createDefault()) {
            final URIBuilder builder = new URIBuilder(String.format("%s/Key", baseURL));
            builder.addParameter("label", keyID);
            final HttpPost post = new HttpPost(builder.build());
            post.setEntity(new StringEntity(key));
            post.setHeader(org.apache.http.HttpHeaders.AUTHORIZATION, "Bearer " + macaroon);
            post.setHeader(org.apache.http.HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);

            try (CloseableHttpResponse response = client.execute(post)) {
                assertEquals(HttpStatus.OK_200, response.getStatusLine().getStatusCode(), "Key should be valid");
            }
        }

        return keyPair.getPrivate();
    }


    public static class MacaroonsInterceptor implements IClientInterceptor {

        private String macaroon;

        public MacaroonsInterceptor(String macaroon) {
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

        public String getMacaroon() {
            return macaroon;
        }

        public void setMacaroon(String macaroon) {
            this.macaroon = macaroon;
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static class AuthResponse {

        @JsonProperty(value = "access_token")
        public String accessToken;
        @JsonProperty(value = "token_type")
        public String tokenType;
        @JsonProperty(value = "expires_in")
        public Long expiresIn;
        public String scope;

        public AuthResponse() {
            // Not used
        }
    }
}
