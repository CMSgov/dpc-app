package gov.cms.dpc.api.resources.v1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;

import javax.ws.rs.HttpMethod;

import org.apache.http.HttpHeaders;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.Test;

import gov.cms.dpc.api.AbstractSecureApplicationTest;

public class AdminResourceTest extends AbstractSecureApplicationTest{

    @Test
    void testOrganizationFetch() throws IOException, URISyntaxException {
        URL url = new URL(getBaseURL() + "/Organization/admin/organizations?ids=123,345");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(HttpMethod.GET);
        conn.setRequestProperty(HttpHeaders.CONTENT_TYPE, "application/fhir+json");
        conn.setRequestProperty(HttpHeaders.AUTHORIZATION, "Bearer " + GOLDEN_MACAROON);

        conn.setDoOutput(true);

        assertEquals(HttpStatus.OK_200, conn.getResponseCode());
    }
}
