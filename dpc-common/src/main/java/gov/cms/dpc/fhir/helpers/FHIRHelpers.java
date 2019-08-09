package gov.cms.dpc.fhir.helpers;

import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import gov.cms.dpc.fhir.FHIRMediaTypes;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.eclipse.jetty.http.HttpStatus;
import org.hl7.fhir.dstu3.model.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.Random;
import java.util.UUID;


public class FHIRHelpers {


    /**
     * Register an organization with the Attribution Service
     * Organizations are pulled from the `organization_bundle.json` file and filtered based on the provided resource ID
     *
     * @param client         - {@link IGenericClient} client to communicate to attribution service
     * @param parser         - {@link IParser} to use for reading {@link Bundle} JSON
     * @param organizationID - {@link String} organization ID to filter for
     * @param attributionURL - {@link String} Attribution server to create Org at
     * @return - {@link String} Access token generated for the {@link Organization}
     * @throws IOException - Throws if HTTP client fails
     */
    public static String registerOrganization(IGenericClient client, IParser parser, String organizationID, String attributionURL) throws IOException {
        // Random number generator for Org NPI
        // Register an organization, and a token
        // Read in the test file
        String macaroon;
        try (InputStream inputStream = FHIRHelpers.class.getClassLoader().getResourceAsStream("organization.tmpl.json")) {


            final Bundle orgBundle = (Bundle) parser.parseResource(inputStream);

            // Update the Organization resource and set a random NPI
            final Organization origOrg = (Organization) orgBundle
                    .getEntryFirstRep().getResource();
            origOrg.getIdentifierFirstRep().setValue(organizationID);
            origOrg.setId(organizationID);

            final Parameters parameters = new Parameters();
            parameters.addParameter().setResource(orgBundle);

            final Organization organization = client
                    .operation()
                    .onType(Organization.class)
                    .named("submit")
                    .withParameters(parameters)
                    .returnResourceType(Organization.class)
                    .encodedJson()
                    .execute();

            try (CloseableHttpClient httpClient = HttpClients.createDefault()) {

                // Now, create a Macaroon
                final HttpPost tokenPost = new HttpPost(String.format("%s/%s/token", attributionURL, organization.getId()));
                tokenPost.setHeader("Accept", FHIRMediaTypes.FHIR_JSON);

                try (CloseableHttpResponse response = httpClient.execute(tokenPost)) {
                    if (response.getStatusLine().getStatusCode() != HttpStatus.OK_200) {
                        throw new IllegalStateException("Should have succeeded");
                    }

                    if (response.getStatusLine().getStatusCode() != HttpStatus.OK_200) {
                        throw new IllegalStateException("Should have succeeded");
                    }
                    macaroon = EntityUtils.toString(response.getEntity());
                }
            }
        }
        return macaroon;
    }
}
