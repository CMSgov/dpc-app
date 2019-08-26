package gov.cms.dpc.api.cli;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import gov.cms.dpc.fhir.FHIRMediaTypes;
import io.dropwizard.cli.Command;
import io.dropwizard.setup.Bootstrap;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Organization;
import org.hl7.fhir.dstu3.model.Parameters;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class OrgRegistrationCommand extends Command {
    private static final String ORG_FILE = "org-file";
    private static final String ORG_ID = "org-id";
    private static final String ATTR_HOSTNAME = "hostname";
    private final FhirContext ctx;

    public OrgRegistrationCommand() {
        super("register", "Register Organization");
        this.ctx = FhirContext.forDstu3();
        // Disable server validation, since the Attribution Service doesn't have a capabilities statement
        this.ctx.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);
    }

    @Override
    public void configure(Subparser subparser) {
        // Location of FHIR organization file
        subparser
                .addArgument("-f", "--file")
                .dest(ORG_FILE)
                .type(String.class)
                .help("FHIR Organization resource to register with system");

        // Address of the Attribution Service, which handles organization registration
        subparser
                .addArgument("--host")
                .dest(ATTR_HOSTNAME)
                .setDefault("http://localhost:3500/v1")
                .help("Address of the Attribution Service, which handles organization registration");

        subparser
                .addArgument("--id", "-i")
                .dest(ORG_ID)
                .type(String.class)
                .setDefault("0c527d2e-2e8a-4808-b11d-0fa06baf8254")
                .help("Organization ID to use for registration");
    }

    @Override
    public void run(Bootstrap<?> bootstrap, Namespace namespace) throws Exception {
        System.out.println("Registering Organization");

        // Read the file and parser it
        Bundle organization;
        try (FileInputStream fileInputStream = new FileInputStream(new File(namespace.getString(ORG_FILE)))) {
            final IParser parser = ctx.newJsonParser();
            organization = (Bundle) parser.parseResource(fileInputStream);
        }

        registerOrganization(organization, namespace.getString(ATTR_HOSTNAME), namespace.getString(ORG_ID));
    }

    void registerOrganization(Bundle organization, String attributionService, String organizationID) throws IOException {
        final IGenericClient client = ctx.newRestfulGenericClient(attributionService);

        final Parameters parameters = new Parameters();

        parameters
                .addParameter().setResource(organization);

        try {
            client
                    .operation()
                    .onType(Organization.class)
                    .named("submit")
                    .withParameters(parameters)
                    .encodedJson()
                    .execute();
        } catch (Exception e) {
            System.err.println(String.format("Unable to register organization. %s", e.getMessage()));
        }

        // Now, create a token

        try (final CloseableHttpClient httpClient = HttpClients.createDefault()) {

            final HttpPost httpPost = new HttpPost(String.format("%s/Organization/%s/token", attributionService, organizationID));
            httpPost.setHeader("Accept", FHIRMediaTypes.FHIR_JSON);

            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                final String token = EntityUtils.toString(response.getEntity());
                System.out.println(String.format("Organization token: %s", token));
            }
        }
    }
}
