package gov.cms.dpc.api.cli.organizations;

import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import gov.cms.dpc.api.cli.AbstractAttributionCommand;
import io.dropwizard.core.setup.Bootstrap;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.net.URIBuilder;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Organization;
import org.hl7.fhir.dstu3.model.Parameters;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.UUID;

public class OrganizationRegistration extends AbstractAttributionCommand {

    protected static final String ORG_FILE = "org-file";

    public OrganizationRegistration() {
        super("register", "Register Organization");
    }

    @Override
    public void addAdditionalOptions(Subparser subparser) {
        // Location of FHIR organization file
        subparser
                .addArgument("-f", "--file")
                .dest(ORG_FILE)
                .type(String.class)
                .required(true)
                .help("FHIR Organization resource to register with system");

        subparser
                .addArgument("--no-token")
                .dest("no-token")
                .type(Boolean.class)
                .action(Arguments.storeTrue())
                .help("Skip generating access token when registering organization");

        subparser
                .addArgument("-a", "--api")
                .dest("api-service")
                .type(String.class)
                .setDefault("http://localhost:9911/tasks")
                .help("URL of API service for generating client token (must include /tasks endpoint). Only required if generating a token");
    }

    @Override
    public void run(Bootstrap<?> bootstrap, Namespace namespace) throws Exception {
        System.out.println("Registering Organization");

        // Read the file and parse it
        final Path filePath = FileSystems.getDefault().getPath(namespace.getString(ORG_FILE)).normalize().toAbsolutePath();
        Bundle organization;
        try (FileInputStream fileInputStream = new FileInputStream(new File(filePath.toUri()))) {
            final IParser parser = ctx.newJsonParser();
            organization = (Bundle) parser.parseResource(fileInputStream);
        }

        final boolean noToken = Boolean.parseBoolean(namespace.getString("no-token"));
        final String apiService = namespace.getString("api-service");

        registerOrganization(organization, namespace.getString(ATTR_HOSTNAME), noToken, apiService);
    }

    private void registerOrganization(Bundle organization, String attributionService, boolean noToken, String apiService) throws IOException, URISyntaxException, ParseException {
        System.out.printf("Connecting to Attribution service at: %s%n", attributionService);
        final IGenericClient client = ctx.newRestfulGenericClient(attributionService);

        final Parameters parameters = new Parameters();

        parameters
                .addParameter().setResource(organization);

        UUID organizationID = null;
        try {
            final Organization createdOrg = client
                    .operation()
                    .onType(Organization.class)
                    .named("submit")
                    .withParameters(parameters)
                    .returnResourceType(Organization.class)
                    .encodedJson()
                    .execute();

            organizationID = UUID.fromString(createdOrg.getIdElement().getIdPart());
            System.out.printf("Registered organization: %s%n", organizationID);

        } catch (Exception e) {
            System.err.printf("Unable to register organization. %s%n", e.getMessage());
            System.exit(1);
        }

        // Now, create a token, unless --no-token has been passed
        if (!noToken) {
            System.out.printf("Connecting to API service at: %s%n", apiService);
            try (final CloseableHttpClient httpClient = HttpClients.createDefault()) {
                final URIBuilder builder = new URIBuilder(String.format("%s/generate-token", apiService));
                builder.setParameter("organization", organizationID.toString());

                final HttpPost httpPost = new HttpPost(builder.build());

                try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                    final String token = EntityUtils.toString(response.getEntity());
                    System.out.printf("Organization token: %s%n", token);
                }
            }
        }
    }
}
