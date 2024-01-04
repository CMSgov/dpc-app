package gov.cms.dpc.api.resources.v1;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.cms.dpc.api.AbstractSecureApplicationTest;
import gov.cms.dpc.api.entities.IpAddressEntity;
import gov.cms.dpc.testing.APIAuthHelpers;
import io.hypersistence.utils.hibernate.type.basic.Inet;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static gov.cms.dpc.api.APITestHelpers.ORGANIZATION_ID;

class IpAddressResourceTest extends AbstractSecureApplicationTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final String fullyAuthedToken;

    private IpAddressResourceTest() throws IOException, URISyntaxException {
        this.fullyAuthedToken = APIAuthHelpers.jwtAuthFlow(getBaseURL(), ORGANIZATION_TOKEN, PUBLIC_KEY_ID, PRIVATE_KEY).accessToken;
    }
    @Test
    public void doIWork() {
        IpAddressEntity ipAddressEntity = new IpAddressEntity()
            .setLabel("Ip address test")
            .setOrganizationId(UUID.fromString(ORGANIZATION_ID))
            .setIpAddress(new Inet("192.168.1.1"));

        try (final CloseableHttpClient client = HttpClients.createDefault()) {
            String ipAddressJson = mapper.writeValueAsString(ipAddressEntity);
            URIBuilder uriBuilder = new URIBuilder(String.format("%s/IpAddress", getBaseURL()));

            HttpPost post = new HttpPost(uriBuilder.build());
            post.setEntity(new StringEntity(ipAddressJson));
            post.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
            post.setHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
            post.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + this.fullyAuthedToken);

            CloseableHttpResponse response = client.execute(post);
        } catch (Exception e) {
            // If we threw an exception, the test fails
            assertTrue(false);
        }
    }
}
