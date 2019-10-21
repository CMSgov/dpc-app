package gov.cms.dpc.api.auth;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import gov.cms.dpc.api.APITestHelpers;
import gov.cms.dpc.api.AbstractSecureApplicationTest;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.eclipse.jetty.http.HttpStatus;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Organization;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.*;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static gov.cms.dpc.api.APITestHelpers.ORGANIZATION_ID;
import static org.junit.jupiter.api.Assertions.*;

class BackendServicesAuthTest extends AbstractSecureApplicationTest {

    BackendServicesAuthTest() {
        // Not used
    }

    @Test
    void testRoundTrip() throws NoSuchAlgorithmException, IOException, URISyntaxException {
        String accessToken;
        // Create and upload Public key
        final String keyID = "test-key";
        final PrivateKey key = generateAndUploadKey(keyID);

        // Create a new JWT with the private key
        Map<String, Object> claims = new HashMap<>();
        claims.put("test", "claim");
        final String jwt = Jwts.builder()
                .setHeaderParam("kid", keyID)
                .setSubject(ORGANIZATION_TOKEN)
                .setIssuer("test issuer")
                .signWith(key, SignatureAlgorithm.RS384)
                .compact();

        // Submit JWT to /auth endpoint
        try (final CloseableHttpClient client = HttpClients.createDefault()) {
            final HttpPost post = new HttpPost(String.format("%s/Token/auth", getBaseURL()));
            post.setEntity(new StringEntity(jwt));
            post.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);

            try (CloseableHttpResponse response = client.execute(post)) {
                assertEquals(HttpStatus.OK_200, response.getStatusLine().getStatusCode(), "Key should be valid");
                accessToken = EntityUtils.toString(response.getEntity());
                assertNotEquals(ORGANIZATION_TOKEN, accessToken, "New Macaroon should not be identical");
            }
        }

        // Verify we can pull the Organization resource
        final IGenericClient client = APITestHelpers.buildAuthenticatedClient(ctx, getBaseURL(), accessToken);

        final Organization orgBundle = client
                .read()
                .resource(Organization.class)
                .withId(new IdType("Organization", ORGANIZATION_ID))
                .encodedJson()
                .execute();

        assertNotNull(orgBundle, "Should have found the organization");
    }

    private PrivateKey generateAndUploadKey(String keyID) throws IOException, URISyntaxException, NoSuchAlgorithmException {
        final KeyPair keyPair = generateKeyPair();
        final String key = generatePublicKey(keyPair.getPublic());

        try (final CloseableHttpClient client = HttpClients.createDefault()) {
            final URIBuilder builder = new URIBuilder(String.format("%s/Key", getBaseURL()));
            builder.addParameter("label", keyID);
            final HttpPost post = new HttpPost(builder.build());
            post.setEntity(new StringEntity(key));
            post.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + ORGANIZATION_TOKEN);
            post.setHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);

            try (CloseableHttpResponse response = client.execute(post)) {
                assertEquals(HttpStatus.OK_200, response.getStatusLine().getStatusCode(), "Key should be valid");
            }
        }

        return keyPair.getPrivate();
    }

    private KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        final KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        return kpg.generateKeyPair();
    }

    private String generatePublicKey(PublicKey key) {
        final String encoded = Base64.getMimeEncoder().encodeToString(key.getEncoded());
        return String.format("-----BEGIN PUBLIC KEY-----\n%s\n-----END PUBLIC KEY-----\n", encoded);
    }
}
