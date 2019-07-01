package gov.cms.dpc.attribution.resources;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import gov.cms.dpc.attribution.AbstractAttributionTest;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import gov.cms.dpc.fhir.FHIRMediaTypes;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.eclipse.jetty.http.HttpStatus;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Organization;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

class OrganizationResourceTest extends AbstractAttributionTest {

    private OrganizationResourceTest() {
        // Not used
    }

    @Test
    void testBasicRegistration() throws IOException {

        // Read in the test file
        final InputStream inputStream = OrganizationResourceTest.class.getClassLoader().getResourceAsStream("organization.tmpl.json");
        final Bundle resource = (Bundle) ctx.newJsonParser().parseResource(inputStream);


        try (final CloseableHttpClient client = HttpClients.createDefault()) {
            final HttpPost httpPost = new HttpPost(getServerURL() + "/Organization");
            httpPost.setHeader("Accept", FHIRMediaTypes.FHIR_JSON);
            httpPost.setEntity(new StringEntity(ctx.newJsonParser().encodeResourceToString(resource)));

            try (CloseableHttpResponse response = client.execute(httpPost)) {
                assertEquals(HttpStatus.OK_200, response.getStatusLine().getStatusCode(), "Should have succeeded");
            }
        }
    }

    @Test
    void testInvalidOrganization() throws IOException {

        // Create fake organization with missing data
        final Organization resource = new Organization();
        resource.addIdentifier().setSystem(DPCIdentifierSystem.MBI.getSystem()).setValue("test-mbi");


        try (final CloseableHttpClient client = HttpClients.createDefault()) {
            final HttpPost httpPost = new HttpPost(getServerURL() + "/Organization");
            httpPost.setHeader("Accept", FHIRMediaTypes.FHIR_JSON);
            httpPost.setEntity(new StringEntity(ctx.newJsonParser().encodeResourceToString(resource)));

            try (CloseableHttpResponse response = client.execute(httpPost)) {
                assertEquals(HttpStatus.INTERNAL_SERVER_ERROR_500, response.getStatusLine().getStatusCode(), "Should have succeeded");
            }
        }
    }


    @Test
    void testSearchAndValidate() throws IOException {
        // Read in the test file
        final InputStream inputStream = OrganizationResourceTest.class.getClassLoader().getResourceAsStream("organization.tmpl.json");
        final Bundle resource = (Bundle) ctx.newJsonParser().parseResource(inputStream);


        try (final CloseableHttpClient client = HttpClients.createDefault()) {
            final HttpPost httpPost = new HttpPost(getServerURL() + "/Organization");
            httpPost.setHeader("Accept", FHIRMediaTypes.FHIR_JSON);
            httpPost.setEntity(new StringEntity(ctx.newJsonParser().encodeResourceToString(resource)));

            try (CloseableHttpResponse response = client.execute(httpPost)) {
                assertEquals(HttpStatus.OK_200, response.getStatusLine().getStatusCode(), "Should have succeeded");
            }
        }

        // Create a token and save it
        String macaroon;
        try (final CloseableHttpClient client = HttpClients.createDefault()) {
            final HttpPost httpPost = new HttpPost(getServerURL() + String.format("/Organization/%s/token", ORGANIZATION_ID));


            try (CloseableHttpResponse response = client.execute(httpPost)) {
                assertEquals(HttpStatus.OK_200, response.getStatusLine().getStatusCode(), "Should have found organization");
                macaroon = EntityUtils.toString(response.getEntity());
                // Verify that the first few bytes are correct, to ensure we encoded correctly.
                assertTrue(macaroon.startsWith("eyJ2IjoyLCJs"), "Should have correct starting string value");
            }
        }

        // Now do the search using the FHIR client
        final IGenericClient client = buildClient();

        final Bundle execute = client
                .search()
                .forResource(Organization.class)
//                .withTag("http://cms.gov/token", macaroon)
                .returnBundle(Bundle.class)
                .encodedJson()
                .execute();

        assertEquals(1, execute.getEntry().size(), "Should have found an organization");
    }

    @Test
    void testEmptyTokenSearch() {
        final IGenericClient client = buildClient();

        final IQuery<Bundle> query = client
                .search()
                .forResource(Organization.class)
                .returnBundle(Bundle.class)
                .encodedJson();

        final InvalidRequestException exception = assertThrows(InvalidRequestException.class, query::execute, "Should fail on empty token");
        assertEquals(HttpStatus.BAD_REQUEST_400, exception.getStatusCode(), "Should be bad request");
    }

    private IGenericClient buildClient() {
        ctx.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);
        return ctx.newRestfulGenericClient(getServerURL());
    }
}
