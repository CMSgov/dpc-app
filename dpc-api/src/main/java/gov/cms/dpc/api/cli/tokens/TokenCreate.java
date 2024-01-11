package gov.cms.dpc.api.cli.tokens;

import gov.cms.dpc.api.cli.AbstractAdminCommand;
import io.dropwizard.core.setup.Bootstrap;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.eclipse.jetty.http.HttpStatus;
import org.hl7.fhir.dstu3.model.IdType;

import javax.ws.rs.core.MediaType;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class TokenCreate extends AbstractAdminCommand {

    public TokenCreate() {
        super("create", "Create organization token");
    }

    @Override
    public void addAdditionalOptions(Subparser subparser) {
        subparser
                .addArgument("id")
                .required(true)
                .dest("org-reference")
                .help("ID of Organization to list tokens");

        subparser
                .addArgument("--label", "-l")
                .dest("token-label")
                .help("Label for access token");

        subparser
                .addArgument("--expiration", "-e")
                .dest("token-expiration")
                .help("Expiration time for access token (as Local Date)");
    }

    @Override
    public void run(Bootstrap<?> bootstrap, Namespace namespace) throws Exception {
        final IdType orgID = new IdType(namespace.getString("org-reference"));
        final String apiService = namespace.getString(API_HOSTNAME);
        System.out.println(String.format("Connecting to API service at: %s", apiService));
        try (final CloseableHttpClient httpClient = HttpClients.createDefault()) {
            final URIBuilder builder = new URIBuilder(String.format("%s/generate-token", apiService));

            builder.addParameter("organization", orgID.getIdPart());

            final String label = namespace.getString("token-label");
            if (label != null) {
                builder.addParameter("label", label);
            }

            final String expiration = namespace.getString("token-expiration");
            if (expiration != null) {
                final LocalDate offset = LocalDate.parse(expiration);
                builder.addParameter("expiration", offset.atStartOfDay(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
            }

            final HttpPost post = new HttpPost(builder.build());
            post.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
            post.addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);

            try (CloseableHttpResponse response = httpClient.execute(post)) {
                if (!HttpStatus.isSuccess(response.getStatusLine().getStatusCode())) {
                    System.err.println("Error fetching organization: " + response.getStatusLine().getReasonPhrase());
                    System.exit(1);
                }
                final String token = EntityUtils.toString(response.getEntity());
                System.out.println(String.format("Organization token: %s", token));
            }
        }
    }
}
