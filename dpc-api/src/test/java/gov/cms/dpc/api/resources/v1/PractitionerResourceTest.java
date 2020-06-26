package gov.cms.dpc.api.resources.v1;

import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IReadExecutable;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import gov.cms.dpc.api.APITestHelpers;
import gov.cms.dpc.api.AbstractSecureApplicationTest;
import gov.cms.dpc.common.utils.NPIUtil;
import gov.cms.dpc.fhir.helpers.FHIRHelpers;
import gov.cms.dpc.testing.APIAuthHelpers;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.HttpHeaders;
import org.eclipse.jetty.http.HttpStatus;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Practitioner;
import org.junit.jupiter.api.Test;

import javax.ws.rs.HttpMethod;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PractitionerResourceTest extends AbstractSecureApplicationTest {

    PractitionerResourceTest() {
        // Not used
    }

    @Test
    void ensurePractitionersExist() throws IOException, URISyntaxException, GeneralSecurityException {
        final IParser parser = ctx.newJsonParser();
        final IGenericClient attrClient = APITestHelpers.buildAttributionClient(ctx);
        IGenericClient client = APIAuthHelpers.buildAuthenticatedClient(ctx, getBaseURL(), ORGANIZATION_TOKEN, PUBLIC_KEY_ID, PRIVATE_KEY);
        APITestHelpers.setupPractitionerTest(client, parser);

        // Find everything attributed
        final Bundle practitioners = client
                .search()
                .forResource(Practitioner.class)
                .returnBundle(Bundle.class)
                .encodedJson()
                .execute();

        assertEquals(4, practitioners.getTotal(), "Should have all the providers");

        final Bundle specificSearch = client
                .search()
                .forResource(Practitioner.class)
                .where(Practitioner.IDENTIFIER.exactly().code("1232131239"))
                .returnBundle(Bundle.class)
                .encodedJson()
                .execute();

        assertEquals(1, specificSearch.getTotal(), "Should have a specific provider");

        // Fetch the provider directly
        final Practitioner foundProvider = (Practitioner) specificSearch.getEntryFirstRep().getResource();

        final IReadExecutable<Practitioner> clientQuery = client
                .read()
                .resource(Practitioner.class)
                .withId(foundProvider.getIdElement())
                .encodedJson();

        final Practitioner queriedProvider = clientQuery
                .execute();

        assertTrue(foundProvider.equalsDeep(queriedProvider), "Search and GET should be identical");

        // Try to delete the practitioner

        client
                .delete()
                .resourceById(queriedProvider.getIdElement())
                .encodedJson()
                .execute();


        // Try again, should be not found
        assertThrows(AuthenticationException.class, clientQuery::execute, "Should not have practitioner");

        // Create a new org and make sure it has no providers
        final String m2 = FHIRHelpers.registerOrganization(attrClient, parser, OTHER_ORG_ID, "1112111111", getAdminURL());
        // Submit a new public key to use for JWT flow
        final String keyLabel = "new-key";
        final Pair<UUID, PrivateKey> uuidPrivateKeyPair = APIAuthHelpers.generateAndUploadKey(keyLabel, OTHER_ORG_ID, GOLDEN_MACAROON, getBaseURL());

        // Update the authenticated client to use the new organization
        client = APIAuthHelpers.buildAuthenticatedClient(ctx, getBaseURL(), m2, uuidPrivateKeyPair.getLeft(), uuidPrivateKeyPair.getRight());

        final Bundle otherPractitioners = client
                .search()
                .forResource(Practitioner.class)
                .returnBundle(Bundle.class)
                .encodedJson()
                .execute();

        assertEquals(0, otherPractitioners.getTotal(), "Should not have any practitioners");

        // Try to look for one of the other practitioners
        final Bundle otherSpecificSearch = client
                .search()
                .forResource(Practitioner.class)
                .where(Practitioner.IDENTIFIER.exactly().identifier(foundProvider.getIdentifierFirstRep().getValue()))
                .returnBundle(Bundle.class)
                .encodedJson()
                .execute();

        assertEquals(0, otherSpecificSearch.getTotal(), "Should not have a specific provider");

        // Try to search for our fund provider
    }

    @Test
    void testCreateInvalidPractitioner() throws IOException, URISyntaxException {
        URL url = new URL(getBaseURL() + "/Practitioner");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(HttpMethod.POST);
        conn.setRequestProperty(HttpHeaders.CONTENT_TYPE, "application/fhir+json");

        APIAuthHelpers.AuthResponse auth = APIAuthHelpers.jwtAuthFlow(getBaseURL(), ORGANIZATION_TOKEN, PUBLIC_KEY_ID, PRIVATE_KEY);
        conn.setRequestProperty(HttpHeaders.AUTHORIZATION, "Bearer " + auth.accessToken);

        conn.setDoOutput(true);
        String reqBody = "{\"test\": \"test\"}";
        conn.getOutputStream().write(reqBody.getBytes());

        assertEquals(HttpStatus.BAD_REQUEST_400, conn.getResponseCode());

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()))) {
            StringBuilder respBuilder = new StringBuilder();
            String respLine = null;
            while ((respLine = reader.readLine()) != null) {
                respBuilder.append(respLine.trim());
            }
            String resp = respBuilder.toString();
            assertTrue(resp.contains("\"resourceType\":\"OperationOutcome\""));
            assertTrue(resp.contains("Invalid JSON content"));
        }

        conn.disconnect();
    }

    @Test
    public void testCreatePractitionerReturnsAppropriateHeaders() {
        IGenericClient client = APIAuthHelpers.buildAuthenticatedClient(ctx, getBaseURL(), ORGANIZATION_TOKEN, PUBLIC_KEY_ID, PRIVATE_KEY);
        Practitioner practitioner = APITestHelpers.createPractitionerResource(NPIUtil.generateNPI(), APITestHelpers.ORGANIZATION_ID);

        MethodOutcome methodOutcome = client.create()
                .resource(practitioner)
                .encodedJson()
                .execute();

        String location = methodOutcome.getResponseHeaders().get("location").get(0);
        String date = methodOutcome.getResponseHeaders().get("last-modified").get(0);
        assertNotNull(location);
        assertNotNull(date);

        Practitioner foundPractitioner = client.read()
                .resource(Practitioner.class)
                .withUrl(location)
                .encodedJson()
                .execute();

        assertEquals(practitioner.getIdentifierFirstRep().getValue(), foundPractitioner.getIdentifierFirstRep().getValue());

        client.delete()
                .resource(foundPractitioner)
                .encodedJson()
                .execute();
    }
}
