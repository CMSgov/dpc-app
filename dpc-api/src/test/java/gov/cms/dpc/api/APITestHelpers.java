package gov.cms.dpc.api;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.client.api.*;
import gov.cms.dpc.fhir.FHIRMediaTypes;
import io.dropwizard.testing.DropwizardTestSupport;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.eclipse.jetty.http.HttpStatus;
import org.hl7.fhir.dstu3.model.*;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class APITestHelpers {
    public static final String ATTRIBUTION_URL = "http://localhost:3500/v1";
    public static final String ORGANIZATION_ID = "46ac7ad6-7487-4dd0-baa0-6e2c8cae76a0";
    public static final String ATTRIBUTION_TRUNCATE_TASK = "http://localhost:9902/tasks/truncate";

    private APITestHelpers() {
        // Not used
    }

    public static IGenericClient buildAttributionClient(FhirContext ctx) {
        ctx.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);
        return ctx.newRestfulGenericClient(ATTRIBUTION_URL);
    }

    /**
     * Register an organization with the Attribution Service
     * Organizations are pulled from the `organization_bundle.json` file and filtered based on the provided resource ID
     * @param client - {@link IGenericClient} client to communicate to attribution service
     * @param parser - {@link IParser} to use for reading {@link Bundle} JSON
     * @param organizationID - {@link String} organzation ID to filter for
     * @return - {@link String} Access token generated for the {@link Organization}
     * @throws IOException
     */
    public static String registerOrganization(IGenericClient client, IParser parser, String organizationID) throws IOException {
        // Register an organization, and a token
        // Read in the test file
        String macaroon;
        try (InputStream inputStream = APITestHelpers.class.getClassLoader().getResourceAsStream("organization_bundle.json")) {


            final Bundle orgBundle = (Bundle) parser.parseResource(inputStream);

            // Filter the bundle to only return resources for the given Organization
            final Bundle filteredBundle = new Bundle();
            orgBundle
                    .getEntry()
                    .stream()
                    .filter(Bundle.BundleEntryComponent::hasResource)
                    .map(Bundle.BundleEntryComponent::getResource)
                    .filter(resource -> {
                        if (resource.getResourceType() == ResourceType.Organization) {
                            return resource.getIdElement().getIdPart().equals(organizationID);
                        } else {
                            return ((Endpoint) resource).getManagingOrganization().getReference().equals("Organization/" + organizationID);
                        }
                    })
                    .forEach(entry -> {
                        filteredBundle.addEntry().setResource(entry);
                    });

            final Parameters parameters = new Parameters();
            parameters.addParameter().setResource(filteredBundle);

            final Organization organization = client
                    .operation()
                    .onType(Organization.class)
                    .named("submit")
                    .withParameters(parameters)
                    .returnResourceType(Organization.class)
                    .encodedJson()
                    .execute();


            try (final CloseableHttpClient httpClient = HttpClients.createDefault()) {

                // Now, create a Macaroon
                final HttpPost tokenPost = new HttpPost(String.format("%s/%s/token", ATTRIBUTION_URL, organization.getId()));
                tokenPost.setHeader("Accept", FHIRMediaTypes.FHIR_JSON);

                try (CloseableHttpResponse response = httpClient.execute(tokenPost)) {
                    assertEquals(HttpStatus.OK_200, response.getStatusLine().getStatusCode(), "Should have succeeded");
                    macaroon = EntityUtils.toString(response.getEntity());
                    assertNotNull(macaroon, "Should have Macaroon");
                }
            }
        }

        return macaroon;
    }

    public static void setupPractitionerTest(IGenericClient client, IParser parser) throws IOException {

        try (InputStream inputStream = APITestHelpers.class.getClassLoader().getResourceAsStream("provider_bundle.json")) {
            final Bundle orgBundle = (Bundle) parser.parseResource(inputStream);

            // Post them all
            orgBundle
                    .getEntry()
                    .stream()
                    .map(Bundle.BundleEntryComponent::getResource)
                    .map(resource -> (Practitioner) resource)
                    .forEach(practitioner -> client
                            .create()
                            .resource(practitioner)
                            .encodedJson()
                            .execute());
        }
    }

    // TODO: Remove as part of DPC-373
    public static IGenericClient buildAuthenticatedClient(FhirContext ctx, String baseURL, String macaroon) {
        final IGenericClient client = ctx.newRestfulGenericClient(baseURL);
        client.registerInterceptor(new MacaroonsInterceptor(macaroon));

        return client;
    }

    static <C extends io.dropwizard.Configuration> void setupApplication(DropwizardTestSupport<C> application) throws IOException {
        truncateDatabase();
        application.before();
    }

    private static void truncateDatabase() throws IOException {

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            final HttpPost post = new HttpPost(ATTRIBUTION_TRUNCATE_TASK);

            try (CloseableHttpResponse execute = client.execute(post)) {
                assertEquals(HttpStatus.OK_200, execute.getStatusLine().getStatusCode(), "Should have truncated database");
            }
        }
    }

    static <C extends io.dropwizard.Configuration> void checkHealth(DropwizardTestSupport<C> application) throws IOException {
        // URI of the API Service Healthcheck
        final String healthURI = String.format("http://localhost:%s/healthcheck", application.getAdminPort());
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            final HttpGet healthCheck = new HttpGet(healthURI);

            try (CloseableHttpResponse execute = client.execute(healthCheck)) {
                assertEquals(HttpStatus.OK_200, execute.getStatusLine().getStatusCode(), "Should be healthy");
            }
        }
    }

    public static class MacaroonsInterceptor implements IClientInterceptor {

        private String macaroon;

        public MacaroonsInterceptor(String macaroon) {
            this.macaroon = macaroon;
        }

        @Override
        public void interceptRequest(IHttpRequest theRequest) {
            theRequest.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + this.macaroon);
        }

        @Override
        public void interceptResponse(IHttpResponse theResponse) {
            // Not used
        }

        public String getMacaroon() {
            return macaroon;
        }

        public void setMacaroon(String macaroon) {
            this.macaroon = macaroon;
        }
    }
}
