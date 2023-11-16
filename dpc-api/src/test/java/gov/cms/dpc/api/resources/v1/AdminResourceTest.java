package gov.cms.dpc.api.resources.v1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.UUID;

import javax.ws.rs.HttpMethod;

import org.apache.http.HttpHeaders;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.Test;

import gov.cms.dpc.api.AbstractSecureApplicationTest;

public class AdminResourceTest extends AbstractSecureApplicationTest{

    @Test
    void testGetOrganizations() throws IOException, URISyntaxException {
        UUID orgID1 = UUID.randomUUID();
        UUID orgID2 = UUID.randomUUID();
        URL url = new URL(getBaseURL() + "Admin/organizations?ids="+orgID1.toString() + "," + orgID2.toString());
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(HttpMethod.GET);
        conn.setRequestProperty(HttpHeaders.CONTENT_TYPE, "application/fhir+json");
        conn.setRequestProperty(HttpHeaders.AUTHORIZATION, "Bearer " + GOLDEN_MACAROON);

        conn.setDoOutput(true);

        assertNotNull(conn.getResponseCode());
        assertEquals(HttpStatus.OK_200, conn.getResponseCode());
    }
}
