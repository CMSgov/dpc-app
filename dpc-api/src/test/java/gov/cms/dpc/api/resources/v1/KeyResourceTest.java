package gov.cms.dpc.api.resources.v1;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.cms.dpc.api.AbstractSecureApplicationTest;
import gov.cms.dpc.api.models.CollectionResponse;
import gov.cms.dpc.testing.APIAuthHelpers;
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
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

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
    void testInvalidKeySubmission() throws NoSuchAlgorithmException, IOException, URISyntaxException {
        final String key = generatePublicKey();

        try (final CloseableHttpClient client = HttpClients.createDefault()) {

            final URIBuilder builder = new URIBuilder(String.format("%s/Key", getBaseURL()));
            builder.addParameter("label", "this is a test");
            final HttpPost post = new HttpPost(builder.build());
            post.setEntity(new StringEntity(key));
            post.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + this.fullyAuthedToken);

            try (CloseableHttpResponse response = client.execute(post)) {
                assertEquals(HttpStatus.OK_200, response.getStatusLine().getStatusCode(), "Key should be valid");
            }

            // Try the same key again
            try (CloseableHttpResponse response = client.execute(post)) {
                assertAll(() -> assertEquals(HttpStatus.BAD_REQUEST_400, response.getStatusLine().getStatusCode(), "Cannot submit duplicated keys"),
                        () -> assertEquals("duplicate key value violates unique constraint", EntityUtils.toString(response.getEntity()), "Should have nice error message"));
            }

            // Try again with same label
            post.setEntity(new StringEntity(generatePublicKey()));
            try (CloseableHttpResponse response = client.execute(post)) {
                assertAll(() -> assertEquals(HttpStatus.BAD_REQUEST_400, response.getStatusLine().getStatusCode(), "Cannot submit duplicated keys"),
                        () -> assertEquals("duplicate key value violates unique constraint", EntityUtils.toString(response.getEntity()), "Should have nice error message"));
            }

            // Try with too long label
            final URIBuilder b2 = new URIBuilder(String.format("%s/Key", getBaseURL()));
            b2.addParameter("label", "This is way too long to be used for a key id field. Never should pass");
            final HttpPost labelViolationPost = new HttpPost(b2.build());
            labelViolationPost.setEntity(new StringEntity(key));
            labelViolationPost.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + this.fullyAuthedToken);
            labelViolationPost.setHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
            try (CloseableHttpResponse response = client.execute(labelViolationPost)) {
                assertEquals(HttpStatus.BAD_REQUEST_400, response.getStatusLine().getStatusCode(), "Key label cannot be too long");
            }
        }
    }

    @Test
    void testRoundTrip() throws NoSuchAlgorithmException, IOException {
        final String key = generatePublicKey();

        KeyView entity;
        try (final CloseableHttpClient client = HttpClients.createDefault()) {
            final HttpPost post = new HttpPost(String.format("%s/Key", getBaseURL()));
            post.setEntity(new StringEntity(key));
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
                assertEquals(key.replaceAll("\\n", "").replaceAll("\\r", ""),
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

    private String generatePublicKey() throws NoSuchAlgorithmException {
        final KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        final KeyPair keyPair = kpg.generateKeyPair();

        final String encoded = Base64.getMimeEncoder().encodeToString(keyPair.getPublic().getEncoded());

        return String.format("-----BEGIN PUBLIC KEY-----\n%s\n-----END PUBLIC KEY-----\n", encoded);
    }

}
