package gov.cms.dpc.api.cli.tokens;

import gov.cms.dpc.api.cli.AbstractAttributionCommand;
import gov.cms.dpc.fhir.FHIRMediaTypes;
import io.dropwizard.setup.Bootstrap;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.hl7.fhir.dstu3.model.IdType;

public class TokenCreate extends AbstractAttributionCommand {

    TokenCreate() {
        super("create", "Create organization token");
    }

    @Override
    public void addAdditionalOptions(Subparser subparser) {
        subparser
                .addArgument("id")
                .dest("org-reference")
                .help("ID of Organization to list tokens");
    }

    @Override
    public void run(Bootstrap<?> bootstrap, Namespace namespace) throws Exception {
        final IdType orgID = new IdType(namespace.getString("org-reference"));
        final String attributionService = namespace.getString(ATTR_HOSTNAME);
        try (final CloseableHttpClient httpClient = HttpClients.createDefault()) {

            final HttpPost httpPost = new HttpPost(String.format("%s/Organization/%s/token", attributionService, orgID.getIdPart()));
            httpPost.setHeader("Accept", FHIRMediaTypes.FHIR_JSON);

            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                final String token = EntityUtils.toString(response.getEntity());
                System.out.println(String.format("Organization token: %s", token));
            }
        }
    }
}
