package gov.cms.dpc.api.client;

import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.ICreateTyped;
import ca.uhn.fhir.rest.gclient.IOperationUntypedWithInput;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import gov.cms.dpc.api.models.JobCompletionModel;
import gov.cms.dpc.common.utils.SeedProcessor;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.eclipse.jetty.http.HttpStatus;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * Shared methods for testing export jobs
 * Used by the {@link gov.cms.dpc.api.cli.DemoCommand} and the EndtoEndRequestTest classes.
 */
public class ClientUtils {

    public static final String PROVIDER_ID = "8D80925A-027E-43DD-8AED-9A501CC4CD91";
    private static final Logger logger = LoggerFactory.getLogger(ClientUtils.class);

    private ClientUtils() {
        // Not used
    }

    /**
     * Helper method for creating an export request using the FHIR client
     *
     * @param client     - {@link IGenericClient} client to use for request
     * @param providerID - {@link String} provider ID to request data for
     * @return - {@link IOperationUntypedWithInput} export request, ready to execute
     */
    public static IOperationUntypedWithInput<Parameters> createExportOperation(IGenericClient client, String providerID) {
        return client
                .operation()
                .onInstance(new IdDt("Group", providerID))
                .named("$export")
                .withNoParameters(Parameters.class)
                .encodedJson()
                .useHttpGet();
    }

    /**
     * Helper method for creating a roster {@link Bundle} and corresponding FHIR Post
     *
     * @param client   - {@link IGenericClient} client to use for request
     * @param resource - {@link InputStream} representing test associations file
     * @return - {@link ICreateTyped} FHIR Post operation, ready to execute
     * @throws IOException - throws if unable to read the file
     */
    public static ICreateTyped createRosterSubmission(IGenericClient client, InputStream resource) throws IOException {
        final SeedProcessor seedProcessor = new SeedProcessor(resource);

        final Map<String, List<Pair<String, String>>> providerMap = seedProcessor.extractProviderMap();

        // Find the entry for the given key (yes, I know this is bad)
        final Map.Entry<String, List<Pair<String, String>>> providerRoster = providerMap
                .entrySet()
                .stream()
                .filter((entry) -> entry.getKey().equals(PROVIDER_ID))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Cannot find matching provider"));

        final Bundle providerBundle = seedProcessor.generateRosterBundle(providerRoster);

        // Now, submit the bundle
        // TODO: Currently, the MethodOutcome response does not propagate the created flag, so we can't directly check that the operation succeeded.
        // Instead, we rely on the fact that an error is not thrown.
        return client
                .create()
                .resource(providerBundle)
                .encodedJson();
    }

    /**
     * Helper method to wait for an export request to complete, simply polls the given endpoint every second.
     *
     * @param jobLocation   - {@link String} URL where client can get job status
     * @param statusMessage - {@link String} status message to print on each iteration
     * @return - {@link JobCompletionModel} Completed job response
     * @throws IOException          - throws if the HTTP request fails
     * @throws InterruptedException - throws if the thread is interrupted
     */
    public static JobCompletionModel awaitExportResponse(String jobLocation, String statusMessage) throws IOException, InterruptedException {
        // Use the traditional HTTP Client to check the job status
        JobCompletionModel jobResponse = null;
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            final HttpGet jobGet = new HttpGet(jobLocation);
            boolean done = false;

            while (!done) {
                Thread.sleep(1000);
                logger.debug(statusMessage);
                try (CloseableHttpResponse response = client.execute(jobGet)) {
                    final int statusCode = response.getStatusLine().getStatusCode();
                    done = statusCode == HttpStatus.OK_200 || statusCode > 300;
                    if (done) {
                        final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
                        jobResponse = mapper.readValue(response.getEntity().getContent(), JobCompletionModel.class);
                    }
                }
            }
        }
        return jobResponse;
    }

    /**
     * Helper method to download a file from the {@link gov.cms.dpc.api.resources.v1.DataResource}
     * Uses the {@link File#createTempFile(String, String)} method to create the file handle
     *
     * @param fileID - {@link String} full URL of the file to download
     * @return - {@link File} file handle where the data is stored
     * @throws IOException - throws if the HTTP request or file writing fails
     */
    public static File fetchExportedFiles(String fileID) throws IOException {
        try (CloseableHttpClient client = HttpClients.createDefault()) {

            final File tempFile = File.createTempFile("dpc", ".ndjson");

            final HttpGet fileGet = new HttpGet(fileID);
            try (CloseableHttpResponse fileResponse = client.execute(fileGet)) {

                fileResponse.getEntity().writeTo(new FileOutputStream(tempFile));

                return tempFile;
            }
        }
    }
}
