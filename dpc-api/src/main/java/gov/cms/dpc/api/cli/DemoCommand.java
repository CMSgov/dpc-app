package gov.cms.dpc.api.cli;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.exceptions.NonFhirResponseException;
import ca.uhn.fhir.rest.gclient.IOperationUntypedWithInput;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import gov.cms.dpc.api.models.JobCompletionModel;
import gov.cms.dpc.common.utils.SeedProcessor;
import io.dropwizard.cli.Command;
import io.dropwizard.setup.Bootstrap;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.eclipse.jetty.http.HttpStatus;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Parameters;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

public class DemoCommand extends Command {

    private static final String PROVIDER_ID = "8D80925A-027E-43DD-8AED-9A501CC4CD91";
    private final FhirContext ctx;

    public DemoCommand() {
        super("demo", "Demo client");
        ctx = FhirContext.forDstu3();
    }

    @Override
    public void configure(Subparser subparser) {
        subparser
                .addArgument("-f", "--file")
                .dest("seed-file")
                .type(String.class)
                .setDefault("src/resources/test_associations.csv")
                .help("Association file to use for demo purposes. Defaults to project root");
    }

    @Override
    public void run(Bootstrap<?> bootstrap, Namespace namespace) throws Exception {
        System.out.println("Running demo!");

        // Submit an export request for a provider which is not known to the system.
        final String baseURL = "http://localhost:3002/v1/";
        final IGenericClient exportClient = ctx.newRestfulGenericClient(baseURL);

        final IOperationUntypedWithInput<Parameters> exportOperation = exportClient
                .operation()
                .onInstance(new IdDt("Group", PROVIDER_ID))
                .named("$export")
                .withNoParameters(Parameters.class)
                .encodedJson()
                .useHttpGet();

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

        // Read the provider bundle from the given file
        Bundle providerBundle;
        final String seedsFile = getSeedsFile(namespace);
        try (InputStream resource = new FileInputStream(new File(seedsFile))) {

            final SeedProcessor seedProcessor = new SeedProcessor(resource);

            final Map<String, List<Pair<String, String>>> providerMap = seedProcessor.extractProviderMap();

            // Find the entry for the given key (yes, I know this is bad)
            final Map.Entry<String, List<Pair<String, String>>> providerRoster = providerMap
                    .entrySet()
                    .stream()
                    .filter((entry) -> entry.getKey().equals(PROVIDER_ID))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Cannot find matching provider"));

            providerBundle = seedProcessor.generateRosterBundle(providerRoster);
        }

        // Now, submit the bundle
        System.out.println("Uploading Patient roster");
        final IGenericClient rosterClient = ctx.newRestfulGenericClient(baseURL);

        // FIXME: Currently, the MethodOutcome response does not propagate the created flag, so we can't directly check that the operation succeeded.
        // Instead, we rely on the fact that an error is not thrown.
        rosterClient
                .create()
                .resource(providerBundle)
                .encodedJson()
                .execute();

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

        // Use the traditional HTTP Client to check the job status
        JobCompletionModel jobResponse = null;
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            final HttpGet jobGet = new HttpGet(exportURL);
            boolean done = false;

            while (!done) {
                Thread.sleep(1000);
                System.out.println("Checking Job status");
                try (CloseableHttpResponse response = client.execute(jobGet)) {
                    final int statusCode = response.getStatusLine().getStatusCode();
                    done = statusCode == HttpStatus.OK_200 || statusCode > 300;
                    if (done) {

                        if (statusCode > 300) {
                            System.out.printf("Received error: %s", response.getStatusLine().getReasonPhrase());
                            System.exit(1);
                        }
                        final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
                        jobResponse = mapper.readValue(response.getEntity().getContent(), JobCompletionModel.class);
                    }
                }
            }
        }

        System.out.print("Export job completed successfully.\nAvailable files:\n");
        jobResponse.getOutput().forEach(System.out::println);

        System.exit(0);
    }

    private static String getSeedsFile(Namespace namespace) {
        return namespace.getString("seed-file");
    }
}
