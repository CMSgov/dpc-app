package gov.cms.dpc.fhir.helpers;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IClientInterceptor;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.IHttpRequest;
import ca.uhn.fhir.rest.client.api.IHttpResponse;
import ca.uhn.fhir.util.BundleBuilder;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.eclipse.jetty.http.HttpStatus;
import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;


public class FHIRHelpers {

    private static final Logger log = LoggerFactory.getLogger(FHIRHelpers.class);

    /**
     * Register an organization with the Attribution Service
     * Organizations are pulled from the `organization_bundle.json` file and filtered based on the provided resource ID
     *
     * @param client         - {@link IGenericClient} client to communicate to attribution service
     * @param parser         - {@link IParser} to use for reading {@link Bundle} JSON
     * @param organizationID - {@link String} organization ID to filter for
     * @param organizationNPI - {@link String} the NPI of the organization
     * @param adminURL       - {@link String} Base url for executing admin tasks
     * @return - {@link String} Access token generated for the {@link Organization}
     * @throws IOException - Throws if HTTP client fails
     */
    public static String registerOrganization(IGenericClient client, IParser parser, String organizationID, String organizationNPI, String adminURL) throws IOException {
        // Random number generator for Org NPI
        // Register an organization, and a token
        // Read in the test file
        String macaroon;
        try (InputStream inputStream = FHIRHelpers.class.getClassLoader().getResourceAsStream("organization.tmpl.json")) {
            final Bundle orgBundle = (Bundle) parser.parseResource(inputStream);

            // Update the Organization resource and set a random NPI
            final Organization origOrg = (Organization) orgBundle
                    .getEntryFirstRep().getResource();
            origOrg.getIdentifierFirstRep().setValue(organizationNPI);
            origOrg.setId(organizationID);

            // To create a resource with a specific id, we need to update/create it.
            client.update().resource(origOrg).withId(organizationID).execute();

            // Register the org's end point, which is the second resource in organization.tmpl.json
            final Endpoint endpoint = (Endpoint) orgBundle.getEntry().get(1).getResource();
            endpoint.setManagingOrganization(new Reference(new IdType("Organization", organizationID)));
            client.create().resource(endpoint).execute();

            try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
                final URIBuilder uriBuilder = new URIBuilder(String.format("%s/generate-token", adminURL));
                uriBuilder.setParameter("organization", organizationID);
                // Now, create a Macaroon
                final HttpPost tokenPost = new HttpPost(uriBuilder.build());

                try (CloseableHttpResponse response = httpClient.execute(tokenPost)) {
                    if (response.getStatusLine().getStatusCode() != HttpStatus.OK_200) {
                        throw new IllegalStateException(String.format("Unable to generate token: %s", response.getStatusLine().getReasonPhrase()));
                    }
                    macaroon = EntityUtils.toString(response.getEntity());
                }
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException("Cannot parse URI", e);
            }
        }
        return macaroon;
    }

    /**
     * Handle {@link MethodOutcome} which returns a {@link Response.Status#CREATED} if creation happened.
     * Otherwise, return {@link Response.Status#OK} if not created.
     *
     * @param outcome - {@link MethodOutcome} to handle
     * @return - {@link Response} with {@link IBaseResource} and appropriate HTTP status
     * @throws WebApplicationException with status {@link Response.Status#INTERNAL_SERVER_ERROR} if {@link MethodOutcome#getResource()} is null
     */
    public static Response handleMethodOutcome(MethodOutcome outcome) {
        final IBaseResource resource = outcome.getResource();
        if (resource == null) {
            throw new WebApplicationException("Unable to get resource.", Response.Status.INTERNAL_SERVER_ERROR);
        }
        final Response.Status status = outcome.getCreated() != null ? Response.Status.CREATED : Response.Status.OK;
        Response.ResponseBuilder builder = Response.status(status).entity(resource);
        if (outcome.getCreated() != null) {
            try {
                builder.location(new URI("v1/" + outcome.getResource().getIdElement().toString()));
            } catch (URISyntaxException e) {
                log.warn("Failed to add location header to resource {}", outcome.getResource().fhirType());
            }
            builder.lastModified(resource.getMeta().getLastUpdated());
        }
        return builder.build();
    }

    // TODO: Refactor this as part of DPC-511
    public static IGenericClient buildAuthenticatedClient(FhirContext ctx, String baseURL, String macaroon) {
        final IGenericClient client = ctx.newRestfulGenericClient(baseURL);
        client.registerInterceptor(new MacaroonsInterceptor(macaroon));

        return client;
    }

    public static String createGoldenMacaroon(String taskURL) throws IOException {

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            final HttpPost post = new HttpPost(String.format("%s/generate-token", taskURL));

            try (CloseableHttpResponse execute = client.execute(post)) {
                if (execute.getStatusLine().getStatusCode() != 200) {
                    throw new IllegalStateException("Could not create Macaroon");
                }
                return EntityUtils.toString(execute.getEntity());
            }
        }
    }

    /**
     * Given a bundle, it follows its next link and loads all of it's associated pages.  It then combines the
     * results into a single bundle.
     *
     * @param client Client used to load the bundle's pages from.
     * @param bundle A bundle that should be loaded.
     * @return An aggregate bundle that contains the resources from all of the submitted bundle's pages.
     */
    // TODO: Add tests!
    public static Bundle getPages(IGenericClient client, Bundle bundle) {
        Bundle results = bundle;

        while (bundle.getLink(Bundle.LINK_NEXT) != null) {
            bundle = client.loadPage().next(bundle).execute();
            results = combineBundles(client.getFhirContext(), results, bundle);
        }
        return results;
    }

    /**
     * Combines bundle 2 and 2 and returns the results.
     * @param ctx The FHIR context of the bundles
     * @param bundle1
     * @param bundle2
     * @return A new bundle combining the entries from bundle1 and bundle2.
     */
    // TODO: Add tests
    public static Bundle combineBundles(FhirContext ctx,  Bundle bundle1, Bundle bundle2) {
        BundleBuilder bundleBuilder = new BundleBuilder(ctx);
        bundle1.getEntry().stream().map(component -> component.getResource()).forEach(bundleBuilder::addCollectionEntry);
        bundle2.getEntry().stream().map(component -> component.getResource()).forEach(bundleBuilder::addCollectionEntry);

        Bundle results = (Bundle) bundleBuilder.getBundle();
        results.setTotal(results.getEntry().size());
        results.setType(Bundle.BundleType.SEARCHSET);

        return (Bundle) bundleBuilder.getBundle();
    }

    public static class MacaroonsInterceptor implements IClientInterceptor {

        private String macaroon;

        MacaroonsInterceptor(String macaroon) {
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
