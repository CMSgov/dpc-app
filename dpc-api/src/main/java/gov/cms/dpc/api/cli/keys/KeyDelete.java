package gov.cms.dpc.api.cli.keys;

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

public class KeyDelete extends AbstractAdminCommand {

    private static final String KEY_ID = "key-id";
    private static final String ORG_REFERENCE = "org-reference";

    public KeyDelete() {
        super("delete", "Delete public key for registered Organization.");
    }

    @Override
    public void addAdditionalOptions(Subparser subparser) {
        subparser
                .addArgument("--org", "-o")
                .dest(ORG_REFERENCE)
                .required(true)
                .help("Organization entity");
        subparser
                .addArgument("id")
                .required(true)
                .dest(KEY_ID)
                .help("ID of Public Key to delete");
    }

    @Override
    public void run(Bootstrap<?> bootstrap, Namespace namespace) throws Exception {
        // Get the reference
        final String orgReference = namespace.getString(ORG_REFERENCE);
        final String keyID = namespace.getString(KEY_ID);
        System.out.println(String.format("Deleting public key %s for organization %s", keyID, orgReference));

        final String apiService = namespace.getString(API_HOSTNAME);
        System.out.println(String.format("Connecting to API service at: %s", apiService));

        // Delete the token
        try (final CloseableHttpClient httpClient = HttpClients.createDefault()) {
            final URIBuilder builder = new URIBuilder(String.format("%s/delete-key", apiService));
            builder.setParameter("organization", new IdType(orgReference).getIdPart());
            builder.setParameter("key", keyID);
            final HttpPost keyDelete = new HttpPost(builder.build());

            try (CloseableHttpResponse response = httpClient.execute(keyDelete)) {
                if (!HttpStatus.isSuccess(response.getStatusLine().getStatusCode())) {
                    System.err.println("Error deleting key: " + response.getStatusLine().getReasonPhrase());
                    System.exit(1);
                }
            }
        }

        System.out.println("Successfully deleted public key");
    }
}
