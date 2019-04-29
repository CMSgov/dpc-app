package gov.cms.dpc.api.cli;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.exceptions.NonFhirResponseException;
import ca.uhn.fhir.rest.gclient.ICreateTyped;
import ca.uhn.fhir.rest.gclient.IOperationUntypedWithInput;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import gov.cms.dpc.api.client.ClientUtils;
import gov.cms.dpc.api.models.JobCompletionModel;
import io.dropwizard.cli.Command;
import io.dropwizard.setup.Bootstrap;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import org.eclipse.jetty.http.HttpStatus;
import org.hl7.fhir.dstu3.model.Parameters;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

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
                .setDefault("src/resources/test_associations.csv")
                .help("Association file to use for demo purposes. Defaults to project root");

        // Option for overriding provider id
        subparser
                .addArgument("-p", "--provider")
                .dest("provider-id")
                .type(String.class)
                .setDefault(ClientUtils.PROVIDER_ID)
                .help("Execute as a specific provider");
    }

    @Override
    public void run(Bootstrap<?> bootstrap, Namespace namespace) throws Exception {
        System.out.println("Running demo!");
        final String baseURL = "http://localhost:3002/v1/";

        // Make the initial export request
        // If it's a 404, that's fine, for anything else, fail
        final IOperationUntypedWithInput<Parameters> exportOperation = createExportOperation(namespace, baseURL);
        try {
            exportOperation.execute();
        } catch (ResourceNotFoundException e) {
            System.out.println("Provider is not registered with the system");
        }

        // Sleep for 2 seconds, for presentation reasons
        Thread.sleep(2000);

        this.uploadBundle(namespace, baseURL);

        // Sleep for 2 seconds, for presentation reasons
        Thread.sleep(2000);

        // Retry the export request
        final JobCompletionModel jobResponse = monitorExportRequest(exportOperation);

        System.out.print("\n\nExport job completed successfully.%n%nAvailable files:%n");
        jobResponse.getOutput().forEach(System.out::println);

        System.exit(0);
    }

    private IOperationUntypedWithInput<Parameters> createExportOperation(Namespace namespace, String baseURL) {
        // Submit an export request for a provider which is not known to the system.
        final IGenericClient exportClient = ctx.newRestfulGenericClient(baseURL);

        final String providerID = namespace.getString("provider-id");

        return ClientUtils.createExportOperation(exportClient, providerID);
    }

    private void uploadBundle(Namespace namespace, String baseURL) throws IOException {
        // Read the provider bundle from the given file
        final String seedsFile = getSeedsFile(namespace);
        try (InputStream resource = new FileInputStream(new File(seedsFile))) {
            // Now, submit the bundle
            System.out.println("Uploading Patient roster");
            final IGenericClient rosterClient = ctx.newRestfulGenericClient(baseURL);
            final ICreateTyped rosterSubmission = ClientUtils.createRosterSubmission(rosterClient, resource);
            rosterSubmission.execute();
        }
    }

    private JobCompletionModel monitorExportRequest(IOperationUntypedWithInput<Parameters> exportOperation) throws IOException, InterruptedException {
        System.out.println("Retrying export request");
        String exportURL = "";

        try {
            exportOperation.execute();
        } catch (NonFhirResponseException e) {
            final NonFhirResponseException e1 = e;
            if (e1.getStatusCode() != HttpStatus.NO_CONTENT_204) {
                e.printStackTrace();
                System.exit(1);
            }

            // Get the correct header
            final Map<String, List<String>> headers = e1.getResponseHeaders();

            // Get the headers and check the status
            exportURL = headers.get("content-location").get(0);
            System.out.printf("Export job started. Progress URL: %s%n", exportURL);
        }

        // Poll the job until it's done
        return ClientUtils.awaitExportResponse(exportURL, "Checking job status");
    }

    private static String getSeedsFile(Namespace namespace) {
        return namespace.getString("seed-file");
    }
}
