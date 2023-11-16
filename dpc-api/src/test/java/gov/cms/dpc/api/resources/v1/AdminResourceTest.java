package gov.cms.dpc.api.resources.v1;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;

import javax.ws.rs.HttpMethod;

import org.apache.http.HttpHeaders;
import org.junit.jupiter.api.Test;

import gov.cms.dpc.api.AbstractSecureApplicationTest;

public class AdminResourceTest extends AbstractSecureApplicationTest{

    @Test
    void testGetOrganizations() throws IOException, URISyntaxException {
        URL url = new URL(getBaseURL() + "admin/organizations?ids=123,345");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(HttpMethod.GET);
        conn.setRequestProperty(HttpHeaders.CONTENT_TYPE, "application/fhir+json");
        conn.setRequestProperty(HttpHeaders.AUTHORIZATION, "Bearer " + GOLDEN_MACAROON);

        conn.setDoOutput(true);

        assertNotNull(conn.getResponseCode());
    }
}
