package gov.cms.dpc.api.cli.tokens;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jakewharton.fliptables.FlipTable;
import gov.cms.dpc.api.cli.AbstractAttributionCommand;
import gov.cms.dpc.common.entities.TokenEntity;
import gov.cms.dpc.common.models.TokenResponse;
import gov.cms.dpc.fhir.FHIRMediaTypes;
import io.dropwizard.setup.Bootstrap;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.eclipse.jetty.http.HttpStatus;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Organization;

import java.io.IOException;
import java.util.List;

public class TokenList extends AbstractAttributionCommand {

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
    public void run(Bootstrap<?> bootstrap, Namespace namespace) throws IOException {
        // Get the reference
        final String orgReference = namespace.getString("org-reference");
        System.out.println(String.format("Listing tokens for organization: %s.", orgReference));

        final String attributionService = namespace.getString(ATTR_HOSTNAME);
        final IGenericClient client = ctx.newRestfulGenericClient(attributionService);

        final Organization organization = client
                .read()
                .resource(Organization.class)
                .withId(new IdType(orgReference))
                .encodedJson()
                .execute();

        listTokens(client, organization);
    }

    private void listTokens(IGenericClient client, Organization organization) throws IOException {
        // List all the tokens, switching
        try (final CloseableHttpClient httpClient = HttpClients.createDefault()) {
            final HttpGet tokenGet = new HttpGet(String.format("%s/Token/%s", client.getServerBase(), organization.getIdElement().getIdPart()));

            try (CloseableHttpResponse response = httpClient.execute(tokenGet)) {
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
        final String[] headers = {"Token ID", "Type", "Expires"};

        System.out.println(FlipTable.of(headers, tokens
                .stream()
                .map(token -> new String[]{token.getId(), token.getTokenType().toString(), "token.get()"}).toArray(String[][]::new)));
    }
}
