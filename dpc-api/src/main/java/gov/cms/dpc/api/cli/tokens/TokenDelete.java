package gov.cms.dpc.api.cli.tokens;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import gov.cms.dpc.api.cli.AbstractAttributionCommand;
import io.dropwizard.setup.Bootstrap;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.eclipse.jetty.http.HttpStatus;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Organization;

public class TokenDelete extends AbstractAttributionCommand {

    public TokenDelete() {
        super("delete", "Delete Organization Token");
    }

    @Override
    public void addAdditionalOptions(Subparser subparser) {

        subparser
                .addArgument("--org", "-o")
                .dest("org-reference")
                .help("Organization entity");
        subparser
                .addArgument("id")
                .dest("token-id")
                .help("ID of Token to delete");
    }

    @Override
    public void run(Bootstrap<?> bootstrap, Namespace namespace) throws Exception {
        // Get the reference
        final String orgReference = namespace.getString("org-reference");
        final String tokenID = namespace.getString("token-id");
        System.out.println(String.format("Deleting token %s for organization %s", tokenID, orgReference));

        final String attributionService = namespace.getString(ATTR_HOSTNAME);

        final IGenericClient client = ctx.newRestfulGenericClient(attributionService);

        final Organization organization = client
                .read()
                .resource(Organization.class)
                .withId(new IdType(orgReference))
                .encodedJson()
                .execute();

        // Delete the token
        try (final CloseableHttpClient httpClient = HttpClients.createDefault()) {
            final URIBuilder builder = new URIBuilder("http://localhost:9900/tasks/delete-token");
            builder.setParameter("organization", organization.getIdElement().getIdPart());
            builder.setParameter("token", tokenID);
            final HttpPost tokenDelete = new HttpPost(builder.build());

            try (CloseableHttpResponse response = httpClient.execute(tokenDelete)) {
                if (!HttpStatus.isSuccess(response.getStatusLine().getStatusCode())) {
                    System.err.println("Error deleting token: " + response.getStatusLine().getReasonPhrase());
                    System.exit(1);
                }
            }
        }

        System.out.println("Successfully deleted Token");
    }
}
