package gov.cms.dpc.fhir.helpers;

import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.eclipse.jetty.http.HttpStatus;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Organization;
import org.hl7.fhir.dstu3.model.Parameters;
import org.hl7.fhir.instance.model.api.IBaseResource;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;


public class FHIRHelpers {


    /**
     * Register an organization with the Attribution Service
     * Organizations are pulled from the `organization_bundle.json` file and filtered based on the provided resource ID
     *
     * @param client         - {@link IGenericClient} client to communicate to attribution service
     * @param parser         - {@link IParser} to use for reading {@link Bundle} JSON
     * @param organizationID - {@link String} organization ID to filter for
     * @param adminURL       - {@link String} Base url for executing admin tasks
     * @return - {@link String} Access token generated for the {@link Organization}
     * @throws IOException - Throws if HTTP client fails
     */
    public static String registerOrganization(IGenericClient client, IParser parser, String organizationID, String adminURL) throws IOException {
        // Random number generator for Org NPI
        // Register an organization, and a token
        // Read in the test file
        String macaroon = "";
        try (InputStream inputStream = FHIRHelpers.class.getClassLoader().getResourceAsStream("organization.tmpl.json")) {


            final Bundle orgBundle = (Bundle) parser.parseResource(inputStream);

            // Update the Organization resource and set a random NPI
            final Organization origOrg = (Organization) orgBundle
                    .getEntryFirstRep().getResource();
            origOrg.getIdentifierFirstRep().setValue(organizationID);
            origOrg.setId(organizationID);

            final Parameters parameters = new Parameters();
            parameters.addParameter().setResource(orgBundle);

            client
                    .operation()
                    .onType(Organization.class)
                    .named("submit")
                    .withParameters(parameters)
                    .returnResourceType(Organization.class)
                    .encodedJson()
                    .execute();

            // FIXME: Token generation still needs to happen somehow.
            try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
                final URIBuilder uriBuilder = new URIBuilder(String.format("%s/generate-token", adminURL));
                uriBuilder.setParameter("organization", organizationID);
                // Now, create a Macaroon
                final HttpPost tokenPost = new HttpPost(uriBuilder.build());

                try (CloseableHttpResponse response = httpClient.execute(tokenPost)) {
                    if (response.getStatusLine().getStatusCode() != HttpStatus.OK_200) {
                        throw new IllegalStateException("Should have succeeded");
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
        return Response.status(status).entity(resource).build();
    }
}
