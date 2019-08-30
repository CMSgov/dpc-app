package gov.cms.dpc.api.cli.organizations;

import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.client.api.IGenericClient;
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
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Organization;
import org.hl7.fhir.dstu3.model.Parameters;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.UUID;

public class OrganizationRegistration extends AbstractAttributionCommand {

    private static final String ORG_FILE = "org-file";

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
                .help("FHIR Organization resource to register with system");
    }

    @Override
    public void run(Bootstrap<?> bootstrap, Namespace namespace) throws Exception {
        System.out.println("Registering Organization");

        // Read the file and parser it
        final Path filePath = FileSystems.getDefault().getPath(namespace.getString(ORG_FILE)).normalize().toAbsolutePath();
        Bundle organization;
        try (FileInputStream fileInputStream = new FileInputStream(new File(filePath.toUri()))) {
            final IParser parser = ctx.newJsonParser();
            organization = (Bundle) parser.parseResource(fileInputStream);
        }

        registerOrganization(organization, namespace.getString(ATTR_HOSTNAME));
    }

    private void registerOrganization(Bundle organization, String attributionService) throws IOException {
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

        } catch (Exception e) {
            System.err.println(String.format("Unable to register organization. %s", e.getMessage()));
            System.exit(1);
        }

        // Now, create a token

        try (final CloseableHttpClient httpClient = HttpClients.createDefault()) {

            final HttpPost httpPost = new HttpPost(String.format("%s/Organization/%s/token", attributionService, organizationID.toString()));
            httpPost.setHeader("Accept", FHIRMediaTypes.FHIR_JSON);

            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                final String token = EntityUtils.toString(response.getEntity());
                System.out.println(String.format("Organization token: %s", token));
            }
        }
    }
}
