package gov.cms.dpc.api.cli;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.exceptions.NonFhirResponseException;
import ca.uhn.fhir.rest.gclient.ICreateTyped;
import ca.uhn.fhir.rest.gclient.IOperationUntypedWithInput;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
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

        // Submit an export request for a provider which is not known to the system.
        final String baseURL = "http://localhost:3002/v1/";
        final IGenericClient exportClient = ctx.newRestfulGenericClient(baseURL);

        final String providerID = namespace.getString("provider-id");

        final IOperationUntypedWithInput<Parameters> exportOperation = ClientUtils.createExportOperation(exportClient, providerID);

        // Make the initial export request
        // If it's a 404, that's fine, for anything else, fail
        try {
            exportOperation.execute();
        } catch (BaseServerResponseException e) {
            if (e instanceof ResourceNotFoundException) {
                System.out.println("Provider is not registered with the system");
            } else {
                e.printStackTrace();
                System.exit(1);
            }
        }

        // Sleep for 2 seconds, for presentation reasons
        Thread.sleep(2000);

        // Read the provider bundle from the given file
        final String seedsFile = getSeedsFile(namespace);
        try (InputStream resource = new FileInputStream(new File(seedsFile))) {
            // Now, submit the bundle
            System.out.println("Uploading Patient roster");
            final IGenericClient rosterClient = ctx.newRestfulGenericClient(baseURL);
            final ICreateTyped rosterSubmission = ClientUtils.createRosterSubmission(rosterClient, resource);
            rosterSubmission.execute();

            // Sleep for 2 seconds, for presentation reasons
            Thread.sleep(2000);

            // Retry the export request
            System.out.println("Retrying export request");
            String exportURL = "";

            try {
                exportOperation.execute();
            } catch (BaseServerResponseException e) {
                if (e instanceof NonFhirResponseException) {
                    final NonFhirResponseException e1 = (NonFhirResponseException) e;
                    if (e1.getStatusCode() != HttpStatus.NO_CONTENT_204) {
                        e.printStackTrace();
                        System.exit(1);
                    }

                    // Get the correct header
                    final Map<String, List<String>> headers = e1.getResponseHeaders();

                    // Get the headers and check the status
                    exportURL = headers.get("content-location").get(0);
                    System.out.printf("Export job started. Progress URL: %s\n", exportURL);
                }
            }

            // Poll the job until it's done
            final JobCompletionModel jobResponse = ClientUtils.awaitExportResponse(exportURL, "Checking job status");

            System.out.print("\n\nExport job completed successfully.\n\nAvailable files:\n");
            jobResponse.getOutput().forEach(System.out::println);

            System.exit(0);
        }
    }

    private static String getSeedsFile(Namespace namespace) {
        return namespace.getString("seed-file");
    }
}
