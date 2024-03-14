package gov.cms.dpc.api.cli.tokens;

import gov.cms.dpc.api.cli.AbstractAdminCommand;
import io.dropwizard.core.setup.Bootstrap;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.eclipse.jetty.http.HttpStatus;
import org.hl7.fhir.dstu3.model.IdType;

public class TokenDelete extends AbstractAdminCommand {

    public TokenDelete() {
        super("delete", "Delete Organization Token");
    }

    @Override
    public void addAdditionalOptions(Subparser subparser) {

        subparser
                .addArgument("--org", "-o")
                .dest("org-reference")
                .required(true)
                .help("Organization entity");
        subparser
                .addArgument("id")
                .required(true)
                .dest("token-id")
                .help("ID of Token to delete");
    }

    @Override
    public void run(Bootstrap<?> bootstrap, Namespace namespace) throws Exception {
        // Get the reference
        final String orgReference = namespace.getString("org-reference");
        final String tokenID = namespace.getString("token-id");
        System.out.println(String.format("Deleting token %s for organization %s", tokenID, orgReference));

        final String apiService = namespace.getString(API_HOSTNAME);
        System.out.println(String.format("Connecting to API service at: %s", apiService));

        // Delete the token
        try (final CloseableHttpClient httpClient = HttpClients.createDefault()) {
            final URIBuilder builder = new URIBuilder(String.format("%s/delete-token", apiService));
            builder.setParameter("organization", new IdType(orgReference).getIdPart());
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
