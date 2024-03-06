package gov.cms.dpc.api.resources.v1;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import gov.cms.dpc.api.APITestHelpers;
import gov.cms.dpc.api.AbstractSecureApplicationTest;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import gov.cms.dpc.testing.APIAuthHelpers;
import org.apache.http.HttpHeaders;
import org.eclipse.jetty.http.HttpStatus;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Organization;
import org.junit.jupiter.api.Test;

import javax.ws.rs.HttpMethod;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class AdminResourceTest extends AbstractSecureApplicationTest{

    @Test
    void testGetOrganizations() throws IOException, URISyntaxException {
        UUID orgID1 = UUID.randomUUID();
        UUID orgID2 = UUID.randomUUID();
        URL url = new URL(getBaseURL() + "/Admin/Organization/?ids=id|"+orgID1.toString() + "," + orgID2.toString());
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(HttpMethod.GET);
        conn.setRequestProperty(HttpHeaders.CONTENT_TYPE, "application/fhir+json");
        conn.setRequestProperty(HttpHeaders.AUTHORIZATION, "Bearer " + GOLDEN_MACAROON);

        conn.setDoOutput(true);

        assertNotNull(conn.getResponseCode());
        assertEquals(HttpStatus.OK_200, conn.getResponseCode());
        conn.disconnect();
    }

    @Test
    void testNoGoldenMacaroon() throws IOException, URISyntaxException {
        UUID orgID1 = UUID.randomUUID();
        URL url = new URL(getBaseURL() + "/Admin/Organization/?ids=id|"+orgID1.toString());
        System.out.println("admin organization search url: " + url.toString());
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(HttpMethod.GET);
        conn.setRequestProperty(HttpHeaders.CONTENT_TYPE, "application/fhir+json");
        APIAuthHelpers.AuthResponse auth = APIAuthHelpers.jwtAuthFlow(getBaseURL(), ORGANIZATION_TOKEN, PUBLIC_KEY_ID, PRIVATE_KEY);
        conn.setRequestProperty(HttpHeaders.AUTHORIZATION, "Bearer " + auth.accessToken);

        conn.setDoOutput(true);

        assertEquals(HttpStatus.UNAUTHORIZED_401, conn.getResponseCode());
        conn.disconnect();
    }

    @Test
    void testSearchByNpi() {
        IGenericClient client = APIAuthHelpers.buildAdminClient(ctx, getAdminResourceURL(), GOLDEN_MACAROON, false);

        Map<String, List<String>> searchParams = new HashMap<>();
        searchParams.put("npis", Collections.singletonList(DPCIdentifierSystem.MBI.getSystem() + "|" + APITestHelpers.ORGANIZATION_NPI));

        final Bundle orgBundle = client
                .search()
                .forResource(Organization.class)
                .whereMap(searchParams)
                .returnBundle(Bundle.class)
                .encodedJson()
                .execute();

        List<Bundle.BundleEntryComponent> orgs = orgBundle.getEntry();
        assertEquals(1, orgs.size());

        Organization org = (Organization) orgs.get(0).getResource();
        assertEquals(APITestHelpers.ORGANIZATION_NPI, org.getIdentifierFirstRep().getValue());
    }
}
