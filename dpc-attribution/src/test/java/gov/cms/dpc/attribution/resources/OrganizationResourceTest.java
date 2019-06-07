package gov.cms.dpc.attribution.resources;

import gov.cms.dpc.attribution.AbstractAttributionTest;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import gov.cms.dpc.fhir.FHIRMediaTypes;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
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

        // Read in the test file
        final Organization resource = new Organization();
        resource.addIdentifier().setSystem(DPCIdentifierSystem.MBI.getSystem()).setValue("test-mbi");


        try (final CloseableHttpClient client = HttpClients.createDefault()) {
            final HttpPost httpPost = new HttpPost(getServerURL() + "/Organization");
            httpPost.setHeader("Accept", FHIRMediaTypes.FHIR_JSON);
            httpPost.setEntity(new StringEntity(ctx.newJsonParser().encodeResourceToString(resource)));

            try (CloseableHttpResponse response = client.execute(httpPost)) {
                assertEquals(HttpStatus.INTERNAL_SERVER_ERROR_500, response.getStatusLine().getStatusCode(), "Should have failed");
            }
        }
    }

    @Test
    void testMacaroonGeneration() throws IOException {
        try (final CloseableHttpClient client = HttpClients.createDefault()) {
            final HttpGet httpGet = new HttpGet(getServerURL() + "/Organization/1/token");

            try (CloseableHttpResponse response = client.execute(httpGet)) {
                assertAll(() -> assertEquals(HttpStatus.OK_200, response.getStatusLine().getStatusCode(), "Should have succeeded"),
                        () -> assertNotEquals("", EntityUtils.toString(response.getEntity()), "Should not have empty string"));
            }
        }
    }
}
