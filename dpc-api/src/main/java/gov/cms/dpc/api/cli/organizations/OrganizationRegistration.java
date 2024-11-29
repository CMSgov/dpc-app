package gov.cms.dpc.api.cli.organizations;

import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.util.BundleBuilder;
import gov.cms.dpc.api.cli.AbstractAttributionCommand;
import gov.cms.dpc.fhir.DPCResourceType;
import io.dropwizard.core.setup.Bootstrap;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.hl7.fhir.dstu3.model.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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

    private void registerOrganization(Bundle organization, String attributionService, boolean noToken, String apiService) throws IOException, URISyntaxException {
        System.out.println(String.format("Connecting to Attribution service at: %s", attributionService));
        final IGenericClient client = ctx.newRestfulGenericClient(attributionService);

        UUID organizationID = null;
        try {
            // Get org from bundle
            Organization org = organization
                .getEntry()
                .stream()
                .filter(Bundle.BundleEntryComponent::hasResource)
                .map(Bundle.BundleEntryComponent::getResource)
                .filter(resource -> resource.getResourceType().getPath().equals(DPCResourceType.Organization.getPath()))
                .map(resource -> (Organization) resource)
                .findFirst().get();

            // Get endpoints from bundle
            List<Endpoint> endpoints = organization
                .getEntry()
                .stream()
                .filter(Bundle.BundleEntryComponent::hasResource)
                .map(Bundle.BundleEntryComponent::getResource)
                .filter(resource -> resource.getResourceType().getPath().equals(DPCResourceType.Endpoint.getPath()))
                .map(resource -> (Endpoint) resource)
                .collect(Collectors.toList());

            // Create the org
            MethodOutcome outcome = client.create().resource(org).execute();
            Organization createdOrg = (Organization) outcome.getResource();

            // Update the end points with the new org's id and create them
            BundleBuilder bundleBuilder = new BundleBuilder(client.getFhirContext());
            endpoints.stream()
                .map(ep ->
                    ep.setManagingOrganization(new Reference(new IdType("Organization", createdOrg.getIdPart())))
                )
                .forEach(bundleBuilder::addTransactionCreateEntry);
            client.transaction().withBundle(bundleBuilder.getBundle()).execute();

            organizationID = UUID.fromString(createdOrg.getIdElement().getIdPart());
            System.out.println(String.format("Registered organization: %s", organizationID));

        } catch (Exception e) {
            System.err.println(String.format("Unable to register organization. %s", e.getMessage()));
            System.exit(1);
        }

        // Now, create a token, unless --no-token has been passed
        if (!noToken) {
            System.out.println(String.format("Connecting to API service at: %s", apiService));
            try (final CloseableHttpClient httpClient = HttpClients.createDefault()) {
                final URIBuilder builder = new URIBuilder(String.format("%s/generate-token", apiService));
                builder.setParameter("organization", organizationID.toString());

                final HttpPost httpPost = new HttpPost(builder.build());

                try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                    final String token = EntityUtils.toString(response.getEntity());
                    System.out.println(String.format("Organization token: %s", token));
                }
            }
        }
    }
}
