package gov.cms.dpc.api.cli.tokens;

import gov.cms.dpc.api.cli.AbstractAdminCommand;
import io.dropwizard.core.setup.Bootstrap;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.net.URIBuilder;
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
        System.out.printf("Deleting token %s for organization %s%n", tokenID, orgReference);

        final String apiService = namespace.getString(API_HOSTNAME);
        System.out.printf("Connecting to API service at: %s%n", apiService);

        // Delete the token
        try (final CloseableHttpClient httpClient = HttpClients.createDefault()) {
            final URIBuilder builder = new URIBuilder(String.format("%s/delete-token", apiService));
            builder.setParameter("organization", new IdType(orgReference).getIdPart());
            builder.setParameter("token", tokenID);
            final HttpPost tokenDelete = new HttpPost(builder.build());

            try (CloseableHttpResponse response = httpClient.execute(tokenDelete)) {
                if (!HttpStatus.isSuccess(response.getCode())) {
                    System.err.printf("Error deleting token: %s%n", response.getReasonPhrase());
                    System.exit(1);
                }
            }
        }

        System.out.println("Successfully deleted Token");
    }
}
