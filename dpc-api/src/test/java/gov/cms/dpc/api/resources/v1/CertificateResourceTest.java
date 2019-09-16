package gov.cms.dpc.api.resources.v1;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.cms.dpc.api.AbstractSecureApplicationTest;
import gov.cms.dpc.common.entities.CertificateEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
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
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CertificateResourceTest extends AbstractSecureApplicationTest {

    private final ObjectMapper mapper;

    private CertificateResourceTest() {
        this.mapper = new ObjectMapper();
    }

    @Test
    void roundTrip() throws NoSuchAlgorithmException, IOException {
        final String key = generatePublicKey();

        CertificateEntity entity = null;
        try (final CloseableHttpClient client = HttpClients.createDefault()) {
            final HttpPost post = new HttpPost(String.format("%s/Certificate", getBaseURL()));
            post.setEntity(new StringEntity(key));
            post.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + ORGANIZATION_TOKEN);
            post.setHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);

            try (CloseableHttpResponse response = client.execute(post)) {
                assertEquals(HttpStatus.OK_200, response.getStatusLine().getStatusCode(), "Token should be valid");
                entity = this.mapper.readValue(response.getEntity().getContent(), CertificateEntity.class);
            }
            assertNotNull(entity, "Should have retrieved entity");
            final HttpGet get = new HttpGet(String.format("%s/Certificate/%s", getBaseURL(), entity.getId()));
            get.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + ORGANIZATION_TOKEN);
            get.setHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);

            try (CloseableHttpResponse response = client.execute(get)) {
                final CertificateEntity fetched = this.mapper.readValue(response.getEntity().getContent(), CertificateEntity.class);
                assertEquals(entity, fetched, "Fetch should be equal");
            }

            // Try to read it back out
        }

//        final Response post = RESOURCES
//                .target("/Certificate")
//                .request(MediaType.TEXT_PLAIN)
//                .post(Entity.entity(key, MediaType.TEXT_PLAIN));
//
//        assertEquals(HttpStatus.OK_200, post.getStatus(), "Should have succeeded");
    }

    private String generatePublicKey() throws NoSuchAlgorithmException {
        final KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        final KeyPair keyPair = kpg.generateKeyPair();

        final String encoded = Base64.getMimeEncoder().encodeToString(keyPair.getPublic().getEncoded());

        return String.format("-----BEGIN PUBLIC KEY-----\n%s\n-----END PUBLIC KEY-----", encoded);
    }
}
