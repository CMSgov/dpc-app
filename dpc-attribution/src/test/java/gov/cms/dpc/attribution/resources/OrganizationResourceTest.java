package gov.cms.dpc.attribution.resources;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import gov.cms.dpc.attribution.AbstractAttributionTest;
import gov.cms.dpc.attribution.AttributionTestHelpers;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.eclipse.jetty.http.HttpStatus;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Organization;
import org.hl7.fhir.dstu3.model.Parameters;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

@Disabled
class OrganizationResourceTest extends AbstractAttributionTest {

    private OrganizationResourceTest() {
        // Not used
    }

    @Test
    void testBasicRegistration() {

        // Read in the test file
        final InputStream inputStream = OrganizationResourceTest.class.getClassLoader().getResourceAsStream("organization.tmpl.json");
        final Bundle resource = (Bundle) ctx.newJsonParser().parseResource(inputStream);

        final IGenericClient client = AttributionTestHelpers.createFHIRClient(ctx, getServerURL());

        final Parameters parameters = new Parameters();
        parameters.addParameter().setResource(resource);

        client
                .operation()
                .onType(Organization.class)
                .named("submit")
                .withParameters(parameters)
                .returnResourceType(Organization.class)
                .encodedJson()
                .execute();
    }

    @Test
    void testInvalidOrganization() {

        // Create fake organization with missing data
        final Organization resource = new Organization();
        resource.addIdentifier().setSystem(DPCIdentifierSystem.MBI.getSystem()).setValue("test-mbi");

        final IGenericClient client = AttributionTestHelpers.createFHIRClient(ctx, getServerURL());

        final Parameters parameters = new Parameters();
        parameters.addParameter().setResource(resource);

        final var submit = client
                .operation()
                .onType(Organization.class)
                .named("submit")
                .withParameters(parameters)
                .returnResourceType(Organization.class)
                .encodedJson();

        assertThrows(InternalErrorException.class, submit::execute, "Should throw an internal server error");
    }


    @Test
    void testSearchAndValidate() throws IOException {
        // Read in the test file
        final InputStream inputStream = OrganizationResourceTest.class.getClassLoader().getResourceAsStream("organization.tmpl.json");
        final Bundle resource = (Bundle) ctx.newJsonParser().parseResource(inputStream);


        final IGenericClient client = AttributionTestHelpers.createFHIRClient(ctx, getServerURL());

        final Parameters parameters = new Parameters();
        parameters.addParameter().setResource(resource);

        final Organization organization = client
                .operation()
                .onType(Organization.class)
                .named("submit")
                .withParameters(parameters)
                .returnResourceType(Organization.class)
                .encodedJson()
                .execute();

        // Create a token and save it
        String macaroon;
        try (final CloseableHttpClient httpClient = HttpClients.createDefault()) {
            final HttpPost httpPost = new HttpPost(getServerURL() + String.format("/Organization/%s/token", organization.getIdElement().getIdPart()));


            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                assertEquals(HttpStatus.OK_200, response.getStatusLine().getStatusCode(), "Should have found organization");
                macaroon = EntityUtils.toString(response.getEntity());
                // Verify that the first few bytes are correct, to ensure we encoded correctly.
                assertTrue(macaroon.startsWith("eyJ2IjoyLCJs"), "Should have correct starting string value");
            }
        }

        final Bundle execute = client
                .search()
                .forResource(Organization.class)
                .withTag("http://cms.gov/token", macaroon)
                .returnBundle(Bundle.class)
                .encodedJson()
                .execute();

        assertEquals(1, execute.getEntry().size(), "Should have found an organization");
    }

    @Test
    void testEmptyTokenSearch() {
        final IGenericClient client = AttributionTestHelpers.createFHIRClient(ctx, getServerURL());

        final IQuery<Bundle> query = client
                .search()
                .forResource(Organization.class)
                .returnBundle(Bundle.class)
                .encodedJson();

        final InvalidRequestException exception = assertThrows(InvalidRequestException.class, query::execute, "Should fail on empty token");
        assertEquals(HttpStatus.BAD_REQUEST_400, exception.getStatusCode(), "Should be bad request");
    }
}
