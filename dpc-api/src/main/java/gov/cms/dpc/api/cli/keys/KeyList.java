package gov.cms.dpc.api.cli.keys;

import com.fasterxml.jackson.core.type.TypeReference;
import com.jakewharton.fliptables.FlipTable;
import gov.cms.dpc.api.cli.AbstractAdminCommand;
import gov.cms.dpc.api.entities.PublicKeyEntity;
import gov.cms.dpc.api.models.CollectionResponse;
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

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class KeyList extends AbstractAdminCommand {

    public KeyList() {
        super("list", "List public keys for registered Organization.");
    }

    @Override
    public void addAdditionalOptions(Subparser subparser) {
        subparser
                .addArgument("id")
                .required(true)
                .dest("org-reference")
                .help("ID of Organization to list keys");
    }

    @Override
    public void run(Bootstrap<?> bootstrap, Namespace namespace) throws Exception {
        // Get the reference
        final String orgReference = namespace.getString("org-reference");
        System.out.printf("Listing keys for organization: %s.%n", orgReference);

        final String apiService = namespace.getString(API_HOSTNAME);
        System.out.printf("Connecting to API service at: %s%n", apiService);
        listKeys(apiService, orgReference);
    }

    private void listKeys(String apiService, String organization) throws IOException, URISyntaxException {
        try (final CloseableHttpClient httpClient = HttpClients.createDefault()) {
            final URIBuilder builder = new URIBuilder(String.format("%s/list-keys", apiService));
            builder.addParameter("organization", new IdType(organization).getIdPart());
            final HttpPost tokenPost = new HttpPost(builder.build());

            try (CloseableHttpResponse response = httpClient.execute(tokenPost)) {
                if (!HttpStatus.isSuccess(response.getCode())) {
                    System.err.println("Error fetching organization: " + response.getReasonPhrase());
                    System.exit(1);
                }

                CollectionResponse<PublicKeyEntity> keys = mapper.readValue(response.getEntity().getContent(), new TypeReference<>() {
                });
                generateTable(new ArrayList<>(keys.getEntities()));
            }
        }
    }

    private void generateTable(List<PublicKeyEntity> keys) {
        final String[] headers = {"Key ID", "Label", "Created At"};

        System.out.println(FlipTable.of(headers, keys
                .stream()
                .map(key -> new String[]{key.getId().toString(),
                        key.getLabel(),
                        key.getCreatedAt().format(DateTimeFormatter.ISO_DATE_TIME)}).toArray(String[][]::new)));
    }
}
