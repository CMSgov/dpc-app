package gov.cms.dpc.api.resources.v1;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import gov.cms.dpc.api.AbstractSecureApplicationTest;
import gov.cms.dpc.common.converters.jackson.StringToOffsetDateTimeConverter;
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
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.UUID;

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

        CertificateView entity = null;
        try (final CloseableHttpClient client = HttpClients.createDefault()) {
            final HttpPost post = new HttpPost(String.format("%s/Certificate", getBaseURL()));
            post.setEntity(new StringEntity(key));
            post.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + ORGANIZATION_TOKEN);
            post.setHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);

            try (CloseableHttpResponse response = client.execute(post)) {
                assertEquals(HttpStatus.OK_200, response.getStatusLine().getStatusCode(), "Token should be valid");
                entity = this.mapper.readValue(response.getEntity().getContent(), CertificateView.class);
            }
            assertNotNull(entity, "Should have retrieved entity");
            final HttpGet get = new HttpGet(String.format("%s/Certificate/%s", getBaseURL(), entity.id));
            get.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + ORGANIZATION_TOKEN);
            get.setHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);

            try (CloseableHttpResponse response = client.execute(get)) {
                final CertificateView fetched = this.mapper.readValue(response.getEntity().getContent(), CertificateView.class);
                // Verify the keys are equal, aside from different new line characters
                assertEquals(key.replaceAll("\\n", "").replaceAll("\\r", ""),
                        fetched.certificate.replaceAll("\\n", "").replaceAll("\\r", ""), "Fetch should be equal");
            }
        }
    }

    private String generatePublicKey() throws NoSuchAlgorithmException {
        final KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        final KeyPair keyPair = kpg.generateKeyPair();

        final String encoded = Base64.getMimeEncoder().encodeToString(keyPair.getPublic().getEncoded());

        return String.format("-----BEGIN PUBLIC KEY-----\n%s\n-----END PUBLIC KEY-----\n", encoded);
    }

    static class CertificateView {

        public UUID id;
        public String certificate;
        @JsonDeserialize(converter = StringToOffsetDateTimeConverter.class)
        public OffsetDateTime createdAt;

        CertificateView() {
            // Not used
        }
    }
}
