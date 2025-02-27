package gov.cms.dpc.api.cli.keys;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.cms.dpc.api.cli.AbstractAdminCommand;
import gov.cms.dpc.api.resources.v1.KeyResource;
import io.dropwizard.core.setup.Bootstrap;
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
    private static final String SIGNATURE_FILE = "signature-file";

    public KeyUpload() {
        super("upload", "Upload organization public key");
    }

    @Override
    public void addAdditionalOptions(Subparser subparser) {
        subparser
                .addArgument("id")
                .required(true)
                .dest("org-reference")
                .help("ID of Organization to upload keys tokens");

        subparser
                .addArgument("--label", "-l")
                .dest("key-label")
                .help("Label for public key");

        subparser
                .addArgument("key-file")
                .dest(KEY_FILE)
                .type(String.class)
                .required(true)
                .help("PEM encoded public key to upload");

        subparser
                .addArgument("signature-file")
                .dest(SIGNATURE_FILE)
                .type(String.class)
                .required(true)
                .help("Signature for snippet, produced with corresponding private key");

    }

    @Override
    public void run(Bootstrap<?> bootstrap, Namespace namespace) throws Exception {
        final IdType orgID = new IdType(namespace.getString("org-reference"));
        final String apiService = namespace.getString(API_HOSTNAME);
        System.out.println(String.format("Connecting to API service at: %s", apiService));

        // Read the file and parse it
        final Path keyFilePath = FileSystems.getDefault().getPath(namespace.getString(KEY_FILE));
        final String publicKey = Files.readString(keyFilePath);

        final Path signaturePath = FileSystems.getDefault().getPath(namespace.getString(SIGNATURE_FILE));
        final String signature = Files.readString(signaturePath);

        KeyResource.KeySignature keySignature = new KeyResource.KeySignature(publicKey, signature);

        final Optional<String> label = Optional.ofNullable(namespace.getString("key-label"));
        uploadKey(apiService, orgID.getIdPart(), label, keySignature);
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private void uploadKey(String apiService, String orgID, Optional<String> label, KeyResource.KeySignature keySig) throws IOException, URISyntaxException {
        try (final CloseableHttpClient httpClient = HttpClients.createDefault()) {
            final URIBuilder builder = new URIBuilder(String.format("%s/upload-key", apiService));

            builder.addParameter("organization", orgID);
            label.ifPresent(l -> builder.addParameter("label", l));

            String keySigJson = new ObjectMapper().writeValueAsString(keySig);

            final HttpPost post = new HttpPost(builder.build());
            post.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
            post.addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
            post.setEntity(new StringEntity(keySigJson));

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
