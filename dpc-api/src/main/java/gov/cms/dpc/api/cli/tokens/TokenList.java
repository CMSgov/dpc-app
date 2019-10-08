package gov.cms.dpc.api.cli.tokens;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jakewharton.fliptables.FlipTable;
import gov.cms.dpc.api.cli.AbstractAdminCommand;
import gov.cms.dpc.api.entities.TokenEntity;
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

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class TokenList extends AbstractAdminCommand {

    private final ObjectMapper mapper;

    public TokenList() {
        super("list", "List tokens for registered Organization");
        this.mapper = new ObjectMapper();
    }

    @Override
    public void addAdditionalOptions(Subparser subparser) {
        subparser
                .addArgument("id")
                .dest("org-reference")
                .help("ID of Organization to list tokens");
    }

    @Override
    public void run(Bootstrap<?> bootstrap, Namespace namespace) throws IOException, URISyntaxException {
        // Get the reference
        final String orgReference = namespace.getString("org-reference");
        System.out.println(String.format("Listing tokens for organization: %s.", orgReference));

        final String attributionService = namespace.getString(API_HOSTNAME);

        listTokens(attributionService, orgReference);
    }

    private void listTokens(String attributionService, String organization) throws IOException, URISyntaxException {
        // List all the tokens, switching
        try (final CloseableHttpClient httpClient = HttpClients.createDefault()) {
            final URIBuilder builder = new URIBuilder(String.format("%s/list-tokens", attributionService));
            builder.addParameter("organization", new IdType(organization).getIdPart());
            final HttpPost tokenPost = new HttpPost(builder.build());

            try (CloseableHttpResponse response = httpClient.execute(tokenPost)) {
                if (!HttpStatus.isSuccess(response.getStatusLine().getStatusCode())) {
                    System.err.println("Error fetching organization: " + response.getStatusLine().getReasonPhrase());
                    System.exit(1);
                }

                List<TokenEntity> tokens = mapper.readValue(response.getEntity().getContent(), new TypeReference<List<TokenEntity>>() {
                });
                generateTable(tokens);
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
