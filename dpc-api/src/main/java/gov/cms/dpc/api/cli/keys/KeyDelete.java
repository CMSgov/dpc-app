package gov.cms.dpc.api.cli.keys;

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
        System.out.printf("Deleting public key %s for organization %s%n", keyID, orgReference);

        final String apiService = namespace.getString(API_HOSTNAME);
        System.out.printf("Connecting to API service at: %s%n", apiService);

        // Delete the token
        try (final CloseableHttpClient httpClient = HttpClients.createDefault()) {
            final URIBuilder builder = new URIBuilder(String.format("%s/delete-key", apiService));
            builder.setParameter("organization", new IdType(orgReference).getIdPart());
            builder.setParameter("key", keyID);
            final HttpPost keyDelete = new HttpPost(builder.build());

            try (CloseableHttpResponse response = httpClient.execute(keyDelete)) {
                if (!HttpStatus.isSuccess(response.getCode())) {
                    System.err.println("Error deleting key: " + response.getReasonPhrase());
                    System.exit(1);
                }
            }
        }

        System.out.println("Successfully deleted public key");
    }
}
