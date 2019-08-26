package gov.cms.dpc.api.cli;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jakewharton.fliptables.FlipTable;
import gov.cms.dpc.common.models.TokenResponse;
import gov.cms.dpc.fhir.FHIRMediaTypes;
import io.dropwizard.cli.Command;
import io.dropwizard.setup.Bootstrap;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.eclipse.jetty.http.HttpStatus;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Organization;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class OrgTokenListCommand extends Command {

    private static final String ATTR_HOSTNAME = "hostname";
    private final FhirContext ctx;

    OrgTokenListCommand() {
        super("tokens", "List tokens for registered Organization");
        this.ctx = FhirContext.forDstu3();
        this.ctx.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);
    }

    @Override
    public void configure(Subparser subparser) {
        // Address of the Attribution Service, which handles organization deletion
        subparser
                .addArgument("--host")
                .dest(ATTR_HOSTNAME)
                .setDefault("http://localhost:3500/v1")
                .help("Address of the Attribution Service, which handles organization registration");

        subparser
                .addArgument("--id")
                .dest("org-reference")
                .help("ID of Organization to delete");
    }

    @Override
    public void run(Bootstrap<?> bootstrap, Namespace namespace) throws IOException {
        // Get the reference
        final String orgReference = namespace.getString("org-reference");
        System.out.println(String.format("Listing tokens for organization %s", orgReference));

        final String attributionService = namespace.getString(ATTR_HOSTNAME);

        final IGenericClient client = ctx.newRestfulGenericClient(attributionService);

        final Organization organization = client
                .read()
                .resource(Organization.class)
                .withId(new IdType(orgReference))
                .encodedJson()
                .execute();

        // List all the tokens, switching
        try (final CloseableHttpClient httpClient = HttpClients.createDefault()) {
            final HttpGet tokenGet = new HttpGet(String.format("%s/Organization/%s/token", client.getServerBase(), organization.getIdElement().getIdPart()));
            tokenGet.setHeader("Accept", FHIRMediaTypes.FHIR_JSON);

            try (CloseableHttpResponse response = httpClient.execute(tokenGet)) {
                if (!HttpStatus.isSuccess(response.getStatusLine().getStatusCode())) {
                    System.err.println("Error fetching organization: " + response.getStatusLine().getReasonPhrase());
                    System.exit(1);
                }

                final ObjectMapper mapper = new ObjectMapper();

                List<TokenResponse> tokens = mapper.readValue(response.getEntity().getContent(), new TypeReference<List<TokenResponse>>() {});

                // Generate the table
                final String[] headers = {"Token ID", "Type", "Expires"};

                System.out.println(FlipTable.of(headers, tokens
                        .stream()
                        .map(token -> new String[]{token.getId(), token.getType().toString(), token.getExpires()}).toArray(String[][]::new)));
            }
        }

    }
}
