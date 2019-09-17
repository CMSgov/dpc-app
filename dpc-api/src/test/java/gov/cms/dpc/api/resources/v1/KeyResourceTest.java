package gov.cms.dpc.api.resources.v1;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import gov.cms.dpc.api.AbstractSecureApplicationTest;
import gov.cms.dpc.common.converters.jackson.StringToOffsetDateTimeConverter;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class KeyResourceTest extends AbstractSecureApplicationTest {

    private final ObjectMapper mapper;

    private KeyResourceTest() {
        this.mapper = new ObjectMapper();
    }

    @Test
    void testRoundTrip() throws NoSuchAlgorithmException, IOException {
        final String key = generatePublicKey();

        KeyView entity = null;
        try (final CloseableHttpClient client = HttpClients.createDefault()) {
            final HttpPost post = new HttpPost(String.format("%s/Key", getBaseURL()));
            post.setEntity(new StringEntity(key));
            post.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + ORGANIZATION_TOKEN);
            post.setHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);

            try (CloseableHttpResponse response = client.execute(post)) {
                assertEquals(HttpStatus.OK_200, response.getStatusLine().getStatusCode(), "Token should be valid");
                entity = this.mapper.readValue(response.getEntity().getContent(), KeyView.class);
            }
            assertNotNull(entity, "Should have retrieved entity");
            final HttpGet get = new HttpGet(String.format("%s/Key/%s", getBaseURL(), entity.id));
            get.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + ORGANIZATION_TOKEN);
            get.setHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);

            try (CloseableHttpResponse response = client.execute(get)) {
                final KeyView fetched = this.mapper.readValue(response.getEntity().getContent(), KeyView.class);
                // Verify the keys are equal, aside from different new line characters
                assertEquals(key.replaceAll("\\n", "").replaceAll("\\r", ""),
                        fetched.publicKey.replaceAll("\\n", "").replaceAll("\\r", ""), "Fetch should be equal");
            }

            final HttpGet keyGet = new HttpGet(String.format("%s/Key", getBaseURL()));
            keyGet.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + ORGANIZATION_TOKEN);
            keyGet.setHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);

            try (CloseableHttpResponse response = client.execute(keyGet)) {
                final List<KeyView> fetched = this.mapper.readValue(response.getEntity().getContent(), new TypeReference<List<KeyView>>() {
                });
                assertEquals(1, fetched.size(), "Should have a single key");
            }

            // Delete it
            final HttpDelete keyDeletion = new HttpDelete(String.format("%s/Key/%s", getBaseURL(), entity.id));
            keyDeletion.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + ORGANIZATION_TOKEN);
            keyDeletion.setHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);

            try (CloseableHttpResponse response = client.execute(keyDeletion)) {
                assertEquals(HttpStatus.OK_200, response.getStatusLine().getStatusCode(), "Should have succeeded");
            }

            // Check to see everything is gone.
            try (CloseableHttpResponse response = client.execute(keyGet)) {
                final List<KeyView> fetched = this.mapper.readValue(response.getEntity().getContent(), new TypeReference<List<KeyView>>() {
                });
                assertEquals(0, fetched.size(), "Should not have any keys");
            }
        }
    }

    private String generatePublicKey() throws NoSuchAlgorithmException {
        final KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        final KeyPair keyPair = kpg.generateKeyPair();

        final String encoded = Base64.getMimeEncoder().encodeToString(keyPair.getPublic().getEncoded());

        return String.format("-----BEGIN PUBLIC KEY-----\n%s\n-----END PUBLIC KEY-----\n", encoded);
    }

    static class KeyView {

        public UUID id;
        public String publicKey;
        @JsonDeserialize(converter = StringToOffsetDateTimeConverter.class)
        public OffsetDateTime createdAt;

        KeyView() {
            // Not used
        }
    }
}
