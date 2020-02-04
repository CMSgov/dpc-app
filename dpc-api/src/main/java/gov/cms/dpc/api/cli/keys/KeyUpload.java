package gov.cms.dpc.api.cli.keys;

import gov.cms.dpc.api.cli.AbstractAdminCommand;
import io.dropwizard.setup.Bootstrap;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.eclipse.jetty.http.HttpStatus;
import org.hl7.fhir.dstu3.model.IdType;

import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class KeyUpload extends AbstractAdminCommand {

    private static final String KEY_FILE = "key-file";

    public KeyUpload() {
        super("upload", "Upload organization public key");
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
                .dest("key-label")
                .help("Label for public key");

        subparser
                .addArgument("file")
                .dest(KEY_FILE)
                .type(String.class)
                .required(true)
                .help("PEM encoded public key to upload");
    }

    @Override
    public void run(Bootstrap<?> bootstrap, Namespace namespace) throws Exception {
        final IdType orgID = new IdType(namespace.getString("org-reference"));
        final String apiService = namespace.getString(API_HOSTNAME);
        System.out.println(String.format("Connecting to API service at: %s", apiService));

        // Read the file and parse it
        final Path filePath = FileSystems.getDefault().getPath(namespace.getString(KEY_FILE));
        final String pemFile = Files.readString(filePath);

        final Optional<String> label = Optional.ofNullable(namespace.getString("key-label"));
        uploadKey(apiService, orgID.getIdPart(), label, pemFile);
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private void uploadKey(String apiService, String orgID, Optional<String> label, String pem) throws IOException {
        try (final CloseableHttpClient httpClient = HttpClients.createDefault()) {
            final URIBuilder builder = new URIBuilder(String.format("%s/upload-key", apiService));

            builder.addParameter("organization", orgID);
            label.ifPresent(l -> builder.addParameter("label", l));

            final HttpPost post = new HttpPost(builder.build());
            post.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
            post.addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
            post.setEntity(new StringEntity(pem));

            try (CloseableHttpResponse response = httpClient.execute(post)) {
                if (!HttpStatus.isSuccess(response.getStatusLine().getStatusCode())) {
                    System.err.println("Error fetching organization: " + response.getStatusLine().getReasonPhrase());
                    System.exit(1);
                }
                final String token = EntityUtils.toString(response.getEntity());
                System.out.println(String.format("Organization token: %s", token));
            }
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }
}
