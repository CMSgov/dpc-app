package gov.cms.dpc.api.cli;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import gov.cms.dpc.api.client.ClientUtils;
import gov.cms.dpc.fhir.helpers.FHIRHelpers;
import io.dropwizard.cli.Command;
import io.dropwizard.setup.Bootstrap;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import org.hl7.fhir.dstu3.model.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DemoCommand extends Command {
    private final FhirContext ctx;

    public DemoCommand() {
        super("demo", "Demo client");
        ctx = FhirContext.forDstu3();
    }

    @Override
    public void configure(Subparser subparser) {
        // Option for specifying seeds file
        subparser
                .addArgument("-f", "--file")
                .dest("seed-file")
                .type(String.class)
                .setDefault("src/main/resources/test_associations.csv")
                .help("Association file to use for demo purposes. Defaults to project root");

        subparser
                .addArgument("--host")
                .dest("hostname")
                .type(String.class)
                .setDefault("http://localhost:3002/v1")
                .help("Set the hostname (including scheme, port number and path) for running the Demo against");

        subparser
                .addArgument("-a", "--attribution")
                .dest("attribution-server")
                .setDefault("localhost:3500")
                .help("Set the hostname (including port number) of the Attribution Service");
    }

    @Override
    public void run(Bootstrap<?> bootstrap, Namespace namespace) throws Exception {
        System.out.println("Running demo!");
        final String baseURL = buildBaseURL(namespace);
        final String attributionURL = String.format("http://%s/v1", namespace.getString("attribution-server"));

        // Disable validation against Attribution service
        ctx.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);
        ctx.getRestfulClientFactory().setConnectTimeout(1800);
        final IGenericClient attributionClient = ctx.newRestfulGenericClient(attributionURL);
        final String organizationID = UUID.randomUUID().toString();
        System.out.println(String.format("Registering organization with NPI %s", organizationID));
        final String token = FHIRHelpers.registerOrganization(attributionClient, ctx.newJsonParser(), organizationID, attributionURL);

        // Create an authenticated and async client (the async part is ignored by other endpoints)
        final IGenericClient exportClient = ClientUtils.createExportClient(ctx, baseURL, token);

        // Upload a batch of patients and a batch of providers
        final List<String> providerNPIs = ClientUtils.submitPractitioners(this.getClass(), ctx, exportClient);
        final Map<String, Reference> patientReferences = ClientUtils.submitPatients(this.getClass(), ctx, exportClient);

        // Upload the roster bundle
        final String seedsFile = getSeedsFile(namespace);
        ClientUtils.createAndUploadRosters(seedsFile, exportClient, UUID.fromString(organizationID), patientReferences);

        // Run the job
        ClientUtils.handleExportJob(exportClient, providerNPIs, token);

        System.out.println("Export jobs completed");
        System.exit(0);
    }

    private static String getSeedsFile(Namespace namespace) {
        return namespace.getString("seed-file");
    }

    private static String buildBaseURL(Namespace namespace) {
        return namespace.getString("hostname");
    }

}
