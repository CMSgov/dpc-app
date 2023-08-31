package gov.cms.dpc.api.resources.v1;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.cms.dpc.api.AbstractSecureApplicationTest;
import gov.cms.dpc.api.models.CollectionResponse;
import gov.cms.dpc.testing.APIAuthHelpers;
import gov.cms.dpc.testing.KeyType;
import gov.cms.dpc.testing.models.KeyView;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class KeyResourceTest extends AbstractSecureApplicationTest {

    private final ObjectMapper mapper;
    private final String fullyAuthedToken;

    private KeyResourceTest() {
        this.mapper = new ObjectMapper();
        // Do the JWT flow in order to get a correct ORGANIZATION_TOKEN, this is normally handled by the HAPI client
        try {
            this.fullyAuthedToken = APIAuthHelpers.jwtAuthFlow(getBaseURL(), ORGANIZATION_TOKEN, PUBLIC_KEY_ID, PRIVATE_KEY).accessToken;
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testInvalidKeySubmission() throws GeneralSecurityException, IOException, URISyntaxException {
        KeyResource.KeySignature keySig = generateKeyAndSignature();

        try (final CloseableHttpClient client = HttpClients.createDefault()) {
            final URIBuilder builder = new URIBuilder(String.format("%s/Key", getBaseURL()));
            builder.addParameter("label", "this is a test");
            final HttpPost post = new HttpPost(builder.build());
            String json = new ObjectMapper().writeValueAsString(keySig);
            post.setEntity(new StringEntity(json));
            post.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
            post.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + this.fullyAuthedToken);

            try (CloseableHttpResponse response = client.execute(post)) {
                assertEquals(HttpStatus.OK_200, response.getStatusLine().getStatusCode(), "Key should be valid");
            }

            // Try the same key again
            try (CloseableHttpResponse response = client.execute(post)) {
                assertAll(() -> assertEquals(HttpStatus.BAD_REQUEST_400, response.getStatusLine().getStatusCode(), "Cannot submit duplicated keys"),
                        () -> assertTrue(EntityUtils.toString(response.getEntity()).contains("duplicate key value violates unique constraint"), "Should have nice error message"));
            }

            KeyResource.KeySignature keySig2 = generateKeyAndSignature();
            // Try again with same label
            String json2 = new ObjectMapper().writeValueAsString(keySig2);
            post.setEntity(new StringEntity(json2));
            try (CloseableHttpResponse response = client.execute(post)) {
                assertAll(() -> assertEquals(HttpStatus.BAD_REQUEST_400, response.getStatusLine().getStatusCode(), "Cannot submit duplicated keys"),
                        () -> assertTrue(EntityUtils.toString(response.getEntity()).contains("duplicate key value violates unique constraint"), "Should have nice error message"));
            }

            // Try with too long label
            final URIBuilder b2 = new URIBuilder(String.format("%s/Key", getBaseURL()));
            b2.addParameter("label", "This is way too long to be used for a key id field. Never should pass");
            final HttpPost labelViolationPost = new HttpPost(b2.build());
            labelViolationPost.setEntity(new StringEntity(json));
            labelViolationPost.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
            labelViolationPost.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + this.fullyAuthedToken);
            labelViolationPost.setHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
            try (CloseableHttpResponse response = client.execute(labelViolationPost)) {
                assertEquals(HttpStatus.BAD_REQUEST_400, response.getStatusLine().getStatusCode(), "Key label cannot be too long");
                assertEquals("{\"code\":400,\"message\":\"Key label cannot be more than 25 characters\"}", EntityUtils.toString(response.getEntity()), "Key label should have correct error message");
            }
        }
    }

    @Test
    void testMismatchedKeyAndSignature() throws GeneralSecurityException, IOException, URISyntaxException {
        KeyResource.KeySignature keySig1 = generateKeyAndSignature();
        KeyResource.KeySignature keySig2 = generateKeyAndSignature();
        KeyResource.KeySignature mismatched = new KeyResource.KeySignature(keySig1.getKey(), keySig2.getSignature());
        String json3 = new ObjectMapper().writeValueAsString(mismatched);
        URIBuilder builder = new URIBuilder(String.format("%s/Key", getBaseURL()));
        builder.addParameter("label", "Key/sig mismatch");
        HttpPost post = new HttpPost(builder.build());
        post.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        post.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + this.fullyAuthedToken);
        post.setEntity(new StringEntity(json3));
        try (final CloseableHttpClient client = HttpClients.createDefault()) {
            try (CloseableHttpResponse response = client.execute(post)) {
                assertAll(() -> assertEquals(HttpStatus.BAD_REQUEST_400, response.getStatusLine().getStatusCode(), "Should not accept mismatched public key and signature"),
                        () -> assertTrue(EntityUtils.toString(response.getEntity()).contains("Public key could not be verified"), "Should have informative error message"));
            }
        }
    }

    @Test
    void testRoundTrip() throws GeneralSecurityException, IOException {
        KeyResource.KeySignature keySig = generateKeyAndSignature();

        KeyView entity;
        try (final CloseableHttpClient client = HttpClients.createDefault()) {
            final HttpPost post = new HttpPost(String.format("%s/Key", getBaseURL()));
            String json = new ObjectMapper().writeValueAsString(keySig);
            post.setEntity(new StringEntity(json));
            post.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
            post.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + this.fullyAuthedToken);

            try (CloseableHttpResponse response = client.execute(post)) {
                assertEquals(HttpStatus.OK_200, response.getStatusLine().getStatusCode(), "Token should be valid");
                entity = this.mapper.readValue(response.getEntity().getContent(), KeyView.class);
            }
            assertNotNull(entity, "Should have retrieved entity");
            final HttpGet get = new HttpGet(String.format("%s/Key/%s", getBaseURL(), entity.id));
            get.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + this.fullyAuthedToken);

            try (CloseableHttpResponse response = client.execute(get)) {
                assertEquals(HttpStatus.OK_200, response.getStatusLine().getStatusCode(), "Should have succeeded");
                final KeyView fetched = this.mapper.readValue(response.getEntity().getContent(), KeyView.class);
                // Verify the keys are equal, aside from different new line characters
                assertEquals(keySig.getKey().replaceAll("\\n", "").replaceAll("\\r", ""),
                        fetched.publicKey.replaceAll("\\n", "").replaceAll("\\r", ""), "Fetch should be equal");
            }

            final HttpGet keyGet = new HttpGet(String.format("%s/Key", getBaseURL()));
            keyGet.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + this.fullyAuthedToken);

            try (CloseableHttpResponse response = client.execute(keyGet)) {
                final CollectionResponse<KeyView> fetched = this.mapper.readValue(response.getEntity().getContent(), new TypeReference<CollectionResponse<KeyView>>() {
                });
                assertEquals(3, fetched.getCount(), "Should have multiple keys");
            }

            // Delete it
            final HttpDelete keyDeletion = new HttpDelete(String.format("%s/Key/%s", getBaseURL(), entity.id));
            keyDeletion.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + this.fullyAuthedToken);
            keyDeletion.setHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);

            try (CloseableHttpResponse response = client.execute(keyDeletion)) {
                assertEquals(HttpStatus.OK_200, response.getStatusLine().getStatusCode(), "Should have succeeded");
            }

            // Check to see everything is gone.
            try (CloseableHttpResponse response = client.execute(keyGet)) {
                final CollectionResponse<KeyView> fetched = this.mapper.readValue(response.getEntity().getContent(), new TypeReference<CollectionResponse<KeyView>>() {
                });
                assertEquals(2, fetched.getEntities().size(), "Should have one less key");
            }
        }
    }

    // TODO: Remove this test when ECC support is re-enabled.
    @Test
    public void testRejectEccKey() throws NoSuchAlgorithmException, IOException {
        KeyPair eccKeyPair = APIAuthHelpers.generateKeyPair(KeyType.ECC);
        String publicKeyStr = APIAuthHelpers.generatePublicKey(eccKeyPair.getPublic());
        KeyResource.KeySignature keySig = new KeyResource.KeySignature(publicKeyStr, "");
        try (final CloseableHttpClient client = HttpClients.createDefault()) {
            final HttpPost post = new HttpPost(String.format("%s/Key", getBaseURL()));
            String json = new ObjectMapper().writeValueAsString(keySig);
            post.setEntity(new StringEntity(json));
            post.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
            post.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + this.fullyAuthedToken);

            try (CloseableHttpResponse response = client.execute(post)) {
                assertEquals(HttpStatus.UNPROCESSABLE_ENTITY_422, response.getStatusLine().getStatusCode(), "ECC key should be rejected");
                Map<String, String> respBody = new ObjectMapper().readValue(response.getEntity().getContent(), Map.class);
                assertEquals(respBody.get("message"), "ECC keys are not currently supported", "Should return helpful error message");
            }
        }
    }

    protected static KeyResource.KeySignature generateKeyAndSignature() throws GeneralSecurityException {
        final KeyPair keyPair = APIAuthHelpers.generateKeyPair(KeyType.RSA);
        final String publicKey = APIAuthHelpers.generatePublicKey(keyPair.getPublic());
        final String signature = APIAuthHelpers.signString(keyPair.getPrivate(), KeyResource.SNIPPET);
        return new KeyResource.KeySignature(publicKey, signature);
    }
}
