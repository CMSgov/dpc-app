package gov.cms.dpc.api.cli.tokens;

import com.fasterxml.jackson.core.type.TypeReference;
import com.jakewharton.fliptables.FlipTable;
import gov.cms.dpc.api.cli.AbstractAdminCommand;
import gov.cms.dpc.api.entities.TokenEntity;
import gov.cms.dpc.api.models.CollectionResponse;
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

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class TokenList extends AbstractAdminCommand {

    public TokenList() {
        super("list", "List tokens for registered Organization");
    }

    @Override
    public void addAdditionalOptions(Subparser subparser) {
        subparser
                .addArgument("id")
                .required(true)
                .dest("org-reference")
                .help("ID of Organization to list tokens");
    }

    @Override
    public void run(Bootstrap<?> bootstrap, Namespace namespace) throws IOException, URISyntaxException {
        // Get the reference
        final String orgReference = namespace.getString("org-reference");
        System.out.println(String.format("Listing tokens for organization: %s.", orgReference));

        final String apiService = namespace.getString(API_HOSTNAME);
        System.out.println(String.format("Connecting to API service at: %s", apiService));

        listTokens(apiService, orgReference);
    }

    private void listTokens(String apiService, String organization) throws IOException, URISyntaxException {
        // List all the tokens
        try (final CloseableHttpClient httpClient = HttpClients.createDefault()) {
            final URIBuilder builder = new URIBuilder(String.format("%s/list-tokens", apiService));
            builder.addParameter("organization", new IdType(organization).getIdPart());
            final HttpPost tokenPost = new HttpPost(builder.build());

            try (CloseableHttpResponse response = httpClient.execute(tokenPost)) {
                if (!HttpStatus.isSuccess(response.getStatusLine().getStatusCode())) {
                    System.err.println("Error fetching organization: " + response.getStatusLine().getReasonPhrase());
                    System.exit(1);
                }

                CollectionResponse<TokenEntity> tokens = mapper.readValue(response.getEntity().getContent(), new TypeReference<CollectionResponse<TokenEntity>>() {
                });
                generateTable(new ArrayList<>(tokens.getEntities()));
            }
        }
    }

    private void generateTable(List<TokenEntity> tokens) {
        // Generate the table
        final String[] headers = {"Token ID", "Label", "Type", "Created At", "Expires At"};

        System.out.println(FlipTable.of(headers, tokens
                .stream()
                .map(token -> new String[]{token.getId(),
                        token.getLabel(),
                        token.getTokenType().toString(),
                        token.getCreatedAt().format(DateTimeFormatter.ISO_DATE_TIME),
                        token.getExpiresAt().format(DateTimeFormatter.ISO_DATE_TIME)}).toArray(String[][]::new)));
    }
}
