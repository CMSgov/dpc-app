package gov.cms.dpc.api.resources.v1;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.cms.dpc.api.AbstractSecureApplicationTest;
import gov.cms.dpc.api.entities.IpAddressEntity;
import gov.cms.dpc.api.models.CollectionResponse;
import gov.cms.dpc.api.models.CreateIpAddressRequest;
import gov.cms.dpc.testing.APIAuthHelpers;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.jupiter.api.*;

import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.UUID;

import static gov.cms.dpc.api.APITestHelpers.ORGANIZATION_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class IpAddressResourceTest extends AbstractSecureApplicationTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final String fullyAuthedToken;

    private static IpAddressEntity ipAddressEntityResponse = new IpAddressEntity();

    private final CreateIpAddressRequest ipRequest = new CreateIpAddressRequest("192.168.1.1", "test label");

    private IpAddressResourceTest() throws IOException, URISyntaxException {
        this.fullyAuthedToken = APIAuthHelpers.jwtAuthFlow(getBaseURL(), ORGANIZATION_TOKEN, PUBLIC_KEY_ID, PRIVATE_KEY).accessToken;
    }

    // TODO Once we turn on the IpAddress end point, remove this test and re-enable all of the others.
    @Test
    public void testForbidden() throws URISyntaxException, IOException {
        CloseableHttpClient client = HttpClients.createDefault();
        URIBuilder uriBuilder = new URIBuilder(String.format("%s/IpAddress", getBaseURL()));

        HttpGet get = new HttpGet(uriBuilder.build());
        get.setHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
        get.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + this.fullyAuthedToken);

        CloseableHttpResponse response = client.execute(get);
        assertEquals(HttpStatus.SC_FORBIDDEN, response.getStatusLine().getStatusCode());
    }

    @Test
    @Disabled
    @Order(1)
    public void testBadAuth() throws URISyntaxException, IOException {
        CloseableHttpClient client = HttpClients.createDefault();
        URIBuilder uriBuilder = new URIBuilder(String.format("%s/IpAddress", getBaseURL()));

        HttpGet get = new HttpGet(uriBuilder.build());
        get.setHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
        get.setHeader(HttpHeaders.AUTHORIZATION, "Bearer FAKE_AUTH_TOKEN" );

        CloseableHttpResponse response = client.execute(get);
        assertEquals(HttpStatus.SC_UNAUTHORIZED, response.getStatusLine().getStatusCode());
    }

    @Test
    @Disabled
    @Order(2)
    public void testNoAuth() throws URISyntaxException, IOException {
        CloseableHttpClient client = HttpClients.createDefault();
        URIBuilder uriBuilder = new URIBuilder(String.format("%s/IpAddress", getBaseURL()));

        HttpGet get = new HttpGet(uriBuilder.build());
        get.setHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);

        CloseableHttpResponse response = client.execute(get);
        assertEquals(HttpStatus.SC_UNAUTHORIZED, response.getStatusLine().getStatusCode());
    }

    @Test
    @Disabled
    @Order(3)
    public void testPost_happyPath() throws IOException, URISyntaxException {
        CloseableHttpClient client = HttpClients.createDefault();
        String ipAddressJson = mapper.writeValueAsString(ipRequest);
        URIBuilder uriBuilder = new URIBuilder(String.format("%s/IpAddress", getBaseURL()));

        HttpPost post = new HttpPost(uriBuilder.build());
        post.setEntity(new StringEntity(ipAddressJson));
        post.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        post.setHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
        post.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + this.fullyAuthedToken);

        CloseableHttpResponse response = client.execute(post);
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());

        IpAddressEntity responseIp = mapper.readValue(response.getEntity().getContent(), IpAddressEntity.class);
        assertNotNull(responseIp.getId());
        assertEquals(ORGANIZATION_ID, responseIp.getOrganizationId().toString());
        assertEquals(ipRequest.getLabel(), responseIp.getLabel());
        assertEquals(ipRequest.getIpAddress(), responseIp.getIpAddress().getAddress());
        assertNotNull(responseIp.getCreatedAt());

        // Save the updated ipAddressEntity for future tests
        this.ipAddressEntityResponse = responseIp;
    }

    @Test
    @Disabled
    @Order(4)
    // Force this to run after the POST test
    public void testGet() throws URISyntaxException, IOException {
        CloseableHttpClient client = HttpClients.createDefault();
        URIBuilder uriBuilder = new URIBuilder(String.format("%s/IpAddress", getBaseURL()));

        HttpGet get = new HttpGet(uriBuilder.build());
        get.setHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
        get.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + this.fullyAuthedToken);

        CloseableHttpResponse response = client.execute(get);
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());

        TypeReference<CollectionResponse<IpAddressEntity>> typeRef = new TypeReference<CollectionResponse<IpAddressEntity>>() {};
        CollectionResponse<IpAddressEntity> responseCollection = mapper.readValue(response.getEntity().getContent(), typeRef);
        assertEquals(1, responseCollection.getCount());

        IpAddressEntity responseIp = responseCollection.getEntities().stream().findFirst().get();
        assertNotNull(responseIp.getId());
        assertEquals(ipAddressEntityResponse.getOrganizationId(), responseIp.getOrganizationId());
        assertEquals(ipAddressEntityResponse.getLabel(), responseIp.getLabel());
        assertEquals(ipAddressEntityResponse.getIpAddress(), responseIp.getIpAddress());
        assertNotNull(responseIp.getCreatedAt());
    }

    @Test
    @Disabled
    @Order(5)
    public void testDelete_happyPath() throws URISyntaxException, IOException {
        CloseableHttpClient client = HttpClients.createDefault();
        URIBuilder uriBuilder = new URIBuilder(String.format("%s/IpAddress/%s", getBaseURL(), ipAddressEntityResponse.getId()));

        HttpDelete delete = new HttpDelete(uriBuilder.build());
        delete.setHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
        delete.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + this.fullyAuthedToken);

        CloseableHttpResponse response = client.execute(delete);
        assertEquals(HttpStatus.SC_NO_CONTENT, response.getStatusLine().getStatusCode());
    }

    @Test
    @Disabled
    @Order(6)
    public void testDelete_notFound() throws URISyntaxException, IOException {
        CloseableHttpClient client = HttpClients.createDefault();
        URIBuilder uriBuilder = new URIBuilder(String.format("%s/IpAddress/%s", getBaseURL(), UUID.randomUUID()));

        HttpDelete delete = new HttpDelete(uriBuilder.build());
        delete.setHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
        delete.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + this.fullyAuthedToken);

        CloseableHttpResponse response = client.execute(delete);
        assertEquals(HttpStatus.SC_NOT_FOUND, response.getStatusLine().getStatusCode());
    }

    @Test
    @Disabled
    @Order(7)
    // Force this test to run last since it's going to max out our Ips for the org
    public void testPost_tooManyIps() throws IOException, URISyntaxException {
        // We shouldn't have any rows in the table at this point, so fill up to the max
        for(int i=1; i<=8; i++) {
            writeIpAddress(String.format("test post %d", i), "192.168.1.1");
        }

        CreateIpAddressRequest ipAddressRequest = new CreateIpAddressRequest("192.128.1.1","should not post");

        CloseableHttpClient client = HttpClients.createDefault();
        String ipAddressJson = mapper.writeValueAsString(ipAddressRequest);
        URIBuilder uriBuilder = new URIBuilder(String.format("%s/IpAddress", getBaseURL()));

        HttpPost post = new HttpPost(uriBuilder.build());
        post.setEntity(new StringEntity(ipAddressJson));
        post.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        post.setHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
        post.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + this.fullyAuthedToken);

        CloseableHttpResponse response = client.execute(post);
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusLine().getStatusCode());
    }

    @Test
    @Disabled
    @Order(8)
    public void testPost_noIp() throws IOException, URISyntaxException {
        CreateIpAddressRequest emptyIpRequest = new CreateIpAddressRequest(null);

        CloseableHttpClient client = HttpClients.createDefault();
        String ipAddressJson = mapper.writeValueAsString(emptyIpRequest);
        URIBuilder uriBuilder = new URIBuilder(String.format("%s/IpAddress", getBaseURL()));

        HttpPost post = new HttpPost(uriBuilder.build());
        post.setEntity(new StringEntity(ipAddressJson));
        post.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        post.setHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
        post.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + this.fullyAuthedToken);

        CloseableHttpResponse response = client.execute(post);
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusLine().getStatusCode());
    }

    private IpAddressEntity writeIpAddress(String label, String ip) throws URISyntaxException, IOException {
        CreateIpAddressRequest ipAddressRequest = new CreateIpAddressRequest(ip, label);

        try (final CloseableHttpClient client = HttpClients.createDefault()) {
            String ipAddressJson = mapper.writeValueAsString(ipAddressRequest);
            URIBuilder uriBuilder = new URIBuilder(String.format("%s/IpAddress", getBaseURL()));

            HttpPost post = new HttpPost(uriBuilder.build());
            post.setEntity(new StringEntity(ipAddressJson));
            post.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
            post.setHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
            post.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + this.fullyAuthedToken);

            CloseableHttpResponse response = client.execute(post);

            return mapper.readValue(response.getEntity().getContent(), IpAddressEntity.class);
        } catch (Exception e) {
            throw e;
        }
    }
}
