package gov.cms.dpc.testing;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IClientInterceptor;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.IHttpRequest;
import ca.uhn.fhir.rest.client.api.IHttpResponse;
import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.nitram509.jmacaroons.MacaroonVersion;
import com.github.nitram509.jmacaroons.MacaroonsBuilder;
import com.google.common.net.HttpHeaders;
import gov.cms.dpc.testing.models.KeyView;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.eclipse.jetty.http.HttpStatus;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.sql.Date;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class APIAuthHelpers {
    public static final String TASK_URL = "http://localhost:9900/tasks/";
    public static final String KEY_VERIFICATION_SNIPPET = "This is the snippet used to verify a key pair in DPC.";
    private static final String CLIENT_ASSERTION_TYPE = "urn:ietf:params:oauth:client-assertion-type:jwt-bearer";
    private static final ObjectMapper mapper = new ObjectMapper();

    private APIAuthHelpers() {
        // Not used
    }

    public static IGenericClient buildAuthenticatedClient(FhirContext ctx, String baseURL, String macaroon, UUID keyID, PrivateKey privateKey) {
        return buildAuthenticatedClient(ctx, baseURL, macaroon, keyID, privateKey, false);
    }

    public static IGenericClient buildAuthenticatedClient(FhirContext ctx, String baseURL, String macaroon, UUID keyID, PrivateKey privateKey, boolean disableSSLCheck) {
        return buildAuthenticatedClient(ctx, baseURL, macaroon, keyID, privateKey, disableSSLCheck, false);
    }

    public static IGenericClient buildAuthenticatedClient(FhirContext ctx, String baseURL, String macaroon, UUID keyID, PrivateKey privateKey, boolean disableSSLCheck, boolean enableRequestLog) {
        final IGenericClient client = createBaseFHIRClient(ctx, baseURL, disableSSLCheck, enableRequestLog);
        client.registerInterceptor(new HAPISmartInterceptor(baseURL, macaroon, keyID, privateKey));

        // Add the async header the hard way
        final var addPreferInterceptor = new IClientInterceptor() {
            @Override
            public void interceptRequest(IHttpRequest iHttpRequest) {
                // Manually set these values, rather than pulling a dependency on dpc-common, where the constants are defined
                iHttpRequest.addHeader("Prefer", "respond-async");
            }

            @Override
            public void interceptResponse(IHttpResponse iHttpResponse) {
                // Not used
            }
        };
        client.registerInterceptor(addPreferInterceptor);

        return client;
    }

    public static IGenericClient buildAdminClient(FhirContext ctx, String baseURL, String macaroon, boolean disableSSLCheck) {
        return buildAdminClient(ctx, baseURL, macaroon, disableSSLCheck, false);
    }

    public static IGenericClient buildAdminClient(FhirContext ctx, String baseURL, String macaroon, boolean disableSSLCheck, boolean enableRequestLog) {
        final IGenericClient client = createBaseFHIRClient(ctx, baseURL, disableSSLCheck, enableRequestLog);
        client.registerInterceptor(new MacaroonsInterceptor(macaroon));
        return client;
    }

    public static AuthResponse jwtAuthFlow(String baseURL, String macaroon, UUID keyID, PrivateKey privateKey) throws IOException, URISyntaxException {
        /* TODO revert this workaround to previous version of code
         * - git diff f2d3abe1f23e4d1ad2f2a01 5d799c57712418de674 <<< green is good
         * see also https://github.com/CMSgov/dpc-app/pull/849
         */
        String audience = baseURL;
        if (baseURL.startsWith("http://internal-dpc-prod-")) {
            audience = "https://prod.dpc.cms.gov/api/v1";
        }
        final String jwt = Jwts.builder()
                .setHeaderParam("kid", keyID)
                .setAudience(String.format("%s/Token/auth", audience))
                .setIssuer(macaroon)
                .setSubject(macaroon)
                .setId(UUID.randomUUID().toString())
                .setExpiration(Date.from(Instant.now().plus(5, ChronoUnit.MINUTES).minus(30, ChronoUnit.SECONDS)))
                .signWith(privateKey, getSigningAlgorithm(KeyType.RSA))
                .compact();

        // Verify JWT with /validate endpoint
        try (final CloseableHttpClient client = createCustomHttpClient().trusting().build()) {
            final URIBuilder builder = new URIBuilder(String.format("%s/Token/validate", baseURL));
            final HttpPost post = new HttpPost(builder.build());
            post.setEntity(new StringEntity(jwt));
            post.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN);

            try (CloseableHttpResponse response = client.execute(post)) {
                assertEquals(HttpStatus.OK_200, response.getStatusLine().getStatusCode(), "Token validation should have succeeded");
            }
        }

        // Submit JWT to /auth endpoint
        final List<NameValuePair> formData = new ArrayList<>();
        formData.add(new BasicNameValuePair("scope", "system/*.*"));
        formData.add(new BasicNameValuePair("grant_type", "client_credentials"));
        formData.add(new BasicNameValuePair("client_assertion_type", CLIENT_ASSERTION_TYPE));
        formData.add(new BasicNameValuePair("client_assertion", jwt));

        final UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formData);
        final AuthResponse authResponse;
        try (final CloseableHttpClient client = createCustomHttpClient().trusting().build()) {
            final URIBuilder builder = new URIBuilder(String.format("%s/Token/auth", baseURL));
            final HttpPost post = new HttpPost(builder.build());
            post.setEntity(entity);
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
        return createGoldenMacaroon(TASK_URL);
    }

    public static String createGoldenMacaroon(String taskURL) throws IOException {

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            final HttpPost post = new HttpPost(String.format("%s/generate-token", taskURL));

            try (CloseableHttpResponse execute = client.execute(post)) {
                assertEquals(HttpStatus.OK_200, execute.getStatusLine().getStatusCode(), "Generated macaroon");
                return EntityUtils.toString(execute.getEntity());
            }
        }
    }

    public static KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        // TODO: Possibly revert to ECC type.
        return generateKeyPair(KeyType.RSA);
    }

    public static KeyPair generateKeyPair(KeyType keyType) throws NoSuchAlgorithmException {
        final KeyPairGenerator kpg = KeyPairGenerator.getInstance(keyType.getName());
        if (keyType == KeyType.RSA) {
            kpg.initialize(keyType.getKeySize());
        } else {
            ECGenParameterSpec spec = new ECGenParameterSpec("secp256r1");
            try {
                kpg.initialize(spec);
            } catch (InvalidAlgorithmParameterException e) {
                throw new IllegalArgumentException("Cannot generate key", e);
            }
        }
        return kpg.generateKeyPair();
    }

    public static String generatePublicKey(PublicKey key) {
        final String encoded = Base64.getMimeEncoder().encodeToString(key.getEncoded());
        return String.format("-----BEGIN PUBLIC KEY-----\n%s\n-----END PUBLIC KEY-----\n", encoded);
    }

    /**
     * Generate a new {@link KeyPair} and submit the {@link PublicKey} to the API service, along with the given label
     *
     * @param keyLabel       - {@link String} identifier (kid) of the public key
     * @param organizationID - {@link String} organization ID to register key with
     * @param goldenMacaroon - {@link String} admin Macaroon that can upload keys
     * @param baseURL        - {@link String} baseURl to submit Key to
     * @return - {@link Pair}  of {@link UUID} (public key ID) and {@link PrivateKey} which matches the uploaded {@link PublicKey}
     * @throws IOException              - throws if something bad happens
     * @throws URISyntaxException       - throws if the URI is no good
     * @throws NoSuchAlgorithmException - throws if security breaks
     */
    public static Pair<UUID, PrivateKey> generateAndUploadKey(String keyLabel, String organizationID, String goldenMacaroon, String baseURL) throws IOException, URISyntaxException, GeneralSecurityException {
        final KeyPair keyPair = generateKeyPair();
        final String key = generatePublicKey(keyPair.getPublic());
        final String signature = signString(keyPair.getPrivate(), KEY_VERIFICATION_SNIPPET);

        // Create org specific macaroon from Golden Macaroon
        final String macaroon = MacaroonsBuilder
                .modify(MacaroonsBuilder.deserialize(goldenMacaroon).get(0))
                .add_first_party_caveat(String.format("organization_id = %s", organizationID))
                .getMacaroon().serialize(MacaroonVersion.SerializationVersion.V2_JSON);

        final KeyView keyEntity;
        final URIBuilder builder = new URIBuilder(String.format("%s/Key", baseURL));
        builder.addParameter("label", keyLabel);
        final HttpPost post = new HttpPost(builder.build());
        Map<String, String> body = Map.of("key", key, "signature", signature);
        post.setEntity(new StringEntity(new ObjectMapper().writeValueAsString(body)));
        post.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + macaroon);
        post.setHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
        post.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        try (CloseableHttpClient client = createCustomHttpClient().trusting().build()) {
            try (CloseableHttpResponse response = client.execute(post)) {
                System.out.println("RESPONSE ENTITY CONTENT");
                System.out.println(response.getEntity().getContent().toString());
                keyEntity = mapper.readValue(response.getEntity().getContent(), KeyView.class);
                assertEquals(HttpStatus.OK_200, response.getStatusLine().getStatusCode(), "Key should be valid");
            }
        }

        return Pair.of(keyEntity.id, keyPair.getPrivate());
    }

    public static CustomHttpBuilder createCustomHttpClient() {
        return new CustomHttpBuilder();
    }

    public static String signString(PrivateKey privateKey, String str) throws GeneralSecurityException {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey);
        signature.update(str.getBytes(StandardCharsets.UTF_8));
        byte[] sigBytes = signature.sign();
        return Base64.getEncoder().encodeToString(sigBytes);
    }

    private static IGenericClient createBaseFHIRClient(FhirContext ctx, String baseURL, boolean disableSSLCheck, boolean enableRequestLog) {
        final HttpClientBuilder clientBuilder = HttpClients.custom();
        if (disableSSLCheck) {
            try {
                clientBuilder.setSSLContext(createTrustingSSLContext());
                clientBuilder.setSSLHostnameVerifier((s, sslSession) -> s.equalsIgnoreCase(sslSession.getPeerHost()));
            } catch (NoSuchAlgorithmException | KeyManagementException e) {
                throw new RuntimeException("Cannot create custom SSL context", e);
            }
        }

        ctx.getRestfulClientFactory().setHttpClient(clientBuilder.build());

        IGenericClient client = ctx.newRestfulGenericClient(baseURL);

        // Disable logging for tests
        LoggingInterceptor loggingInterceptor = new LoggingInterceptor();
        loggingInterceptor.setLogRequestSummary(enableRequestLog);
        loggingInterceptor.setLogResponseSummary(enableRequestLog);
        client.registerInterceptor(loggingInterceptor);

        return client;
    }

    private static SSLContext createTrustingSSLContext() throws KeyManagementException, NoSuchAlgorithmException {
        final SSLContext tls = SSLContext.getInstance("TLSv1.2");
        tls.init(null, getTrustingManager(), new SecureRandom());
        return tls;
    }

    private static TrustManager[] getTrustingManager() {
        return new TrustManager[]{new X509TrustManager() {
            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            @Override
            public void checkClientTrusted(X509Certificate[] certs, String authType) throws CertificateException {
                // only used for testing, so no certificates expected
                if (certs.length == 0) {
                    // do nothing
                } else if (certs.length > 0) {
                    // still do nothing
                } else {
                    throw new CertificateException();
                }
            }

            @Override
            public void checkServerTrusted(X509Certificate[] certs, String authType) throws CertificateException {
                // only used for testing, so no certificates expected
                if (certs.length == 0) {
                    // do nothing
                } else if (certs.length > 0) {
                    // still do nothing
                } else {
                    throw new CertificateException();
                }
            }

        }
        };
    }

    /**
     * Get the correct {@link SignatureAlgorithm} for the given {@link KeyType}
     *
     * @param keyType - {@link KeyType} to get algorithm for
     * @return - {@link SignatureAlgorithm} to use for signing JWT
     */
    public static SignatureAlgorithm getSigningAlgorithm(KeyType keyType) {
        return keyType == KeyType.ECC ? SignatureAlgorithm.ES256 : SignatureAlgorithm.RS384;
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
        @JsonIgnore
        public OffsetDateTime expiresAt;

        public AuthResponse() {
            // Set the expiration time, so we can track it later
            this.expiresAt = OffsetDateTime.now(ZoneOffset.UTC);
        }
    }

    public static class HAPISmartInterceptor implements IClientInterceptor {

        private final String baseURL;
        private final String clientToken;
        private final UUID keyID;
        private final PrivateKey privateKey;

        private OffsetDateTime shouldRefreshToken;
        private AuthResponse response;

        HAPISmartInterceptor(String baseURL, String clientToken, UUID keyID, PrivateKey privateKey) {
            this.baseURL = baseURL;
            this.clientToken = clientToken;
            this.keyID = keyID;
            this.privateKey = privateKey;

            // Do the initial JWT Auth flow
            refreshAuthToken();
        }

        @Override
        public void interceptRequest(IHttpRequest theRequest) {
            if (OffsetDateTime.now(ZoneOffset.UTC).isAfter(shouldRefreshToken)) {
                refreshAuthToken();
            }

            theRequest.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + this.response.accessToken);
        }

        @Override
        public void interceptResponse(IHttpResponse theResponse) {
            // We don't need this
        }

        public AuthResponse getAuthResponse() {
            return this.response;
        }

        private void refreshAuthToken() {
            System.out.println("Refreshing access token");
            try {
                final AuthResponse authResponse = jwtAuthFlow(this.baseURL, this.clientToken, this.keyID, this.privateKey);
                // Set the refresh time to be 30 seconds before expiration
                this.shouldRefreshToken = OffsetDateTime.now(ZoneOffset.UTC)
                        .plus(authResponse.expiresIn, ChronoUnit.SECONDS)
                        .minus(30, ChronoUnit.SECONDS);
                this.response = authResponse;
            } catch (IOException | URISyntaxException e) {
                throw new IllegalStateException("Cannot perform auth flow", e);
            }
        }
    }

    public static class HttpClientAuthInterceptor implements HttpRequestInterceptor {

        private final String baseURL;
        private final String clientToken;
        private final UUID keyID;
        private final PrivateKey privateKey;

        private OffsetDateTime shouldRefreshToken;
        private AuthResponse response;

        HttpClientAuthInterceptor(String baseURL, String clientToken, UUID keyID, PrivateKey privateKey) {
            this.baseURL = baseURL;
            this.clientToken = clientToken;
            this.keyID = keyID;
            this.privateKey = privateKey;

            // Do the initial refresh
            refreshAuthToken();
        }

        @Override
        public void process(HttpRequest request, HttpContext context) {
            if (OffsetDateTime.now(ZoneOffset.UTC).isAfter(this.shouldRefreshToken)) {
                refreshAuthToken();
            }
            request.addHeader(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", this.response.accessToken));
        }

        private void refreshAuthToken() {
            System.out.println("Refreshing access token");
            try {
                final AuthResponse authResponse = jwtAuthFlow(this.baseURL, this.clientToken, this.keyID, this.privateKey);
                // Set the refresh time to be 30 seconds before expiration
                this.shouldRefreshToken = OffsetDateTime.now(ZoneOffset.UTC)
                        .plus(authResponse.expiresIn, ChronoUnit.SECONDS)
                        .minus(30, ChronoUnit.SECONDS);
                this.response = authResponse;
            } catch (IOException | URISyntaxException e) {
                throw new IllegalStateException("Cannot perform auth flow", e);
            }
        }
    }

    public static class CustomHttpBuilder {

        private final org.apache.http.impl.client.HttpClientBuilder builder;

        CustomHttpBuilder() {
            this.builder = HttpClients.custom();
        }


        public CustomHttpBuilder trusting() {
            try {
                builder
                        .setSSLContext(createTrustingSSLContext())
                        .setSSLHostnameVerifier((s, sslSession) -> s.equalsIgnoreCase(sslSession.getPeerHost()));
            } catch (KeyManagementException | NoSuchAlgorithmException e) {
                throw new IllegalStateException("Cannot create trusting http context");
            }

            return this;
        }

        public CustomHttpBuilder isAuthed(String baseURL, String clientToken, UUID keyID, PrivateKey privateKey) {
            this.builder.addInterceptorFirst(new HttpClientAuthInterceptor(baseURL, clientToken, keyID, privateKey));
            return this;
        }

        public CloseableHttpClient build() {
            return this.builder.build();
        }
    }
}
