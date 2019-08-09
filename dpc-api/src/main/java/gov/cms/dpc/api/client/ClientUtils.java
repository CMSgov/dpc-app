package gov.cms.dpc.api.client;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.rest.client.api.IClientInterceptor;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.IHttpRequest;
import ca.uhn.fhir.rest.client.api.IHttpResponse;
import ca.uhn.fhir.rest.gclient.IOperationUntypedWithInput;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import gov.cms.dpc.api.models.JobCompletionModel;
import gov.cms.dpc.common.utils.SeedProcessor;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.eclipse.jetty.http.HttpStatus;
import org.hl7.fhir.dstu3.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static gov.cms.dpc.fhir.FHIRHeaders.PREFER_HEADER;
import static gov.cms.dpc.fhir.FHIRHeaders.PREFER_RESPOND_ASYNC;

/**
 * Shared methods for testing export jobs
 * Used by the {@link gov.cms.dpc.api.cli.DemoCommand} class.
 */
public class ClientUtils {
    private static final Logger logger = LoggerFactory.getLogger(ClientUtils.class);

    private ClientUtils() {
        // Not used
    }

    /**
     * Helper method to create a FHIR client has the headers setup for export operations.
     *
     * @param context       - FHIR context to use
     * @param serverBaseURL - the base URL for the FHIR endpoint
     * @return {@link IGenericClient} for FHIR requests
     * @see #createExportOperation(IGenericClient, String)
     */
    public static IGenericClient createExportClient(FhirContext context, String serverBaseURL, String accessToken) {
        final IGenericClient exportClient = context.newRestfulGenericClient(serverBaseURL);
        // Add a header the hard way
        final var addPreferInterceptor = new IClientInterceptor() {
            @Override
            public void interceptRequest(IHttpRequest iHttpRequest) {
                iHttpRequest.addHeader(PREFER_HEADER, PREFER_RESPOND_ASYNC);
                if (accessToken != null) {
                    iHttpRequest.addHeader(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", accessToken));
                }
            }

            @Override
            public void interceptResponse(IHttpResponse iHttpResponse) {
                // Not used
            }
        };
        exportClient.registerInterceptor(addPreferInterceptor);
        return exportClient;
    }

    /**
     * Helper method for creating an export request using the FHIR client.
     *
     * @param exportClient - {@link IGenericClient} client to use for the request.
     * @param rosterID     - {@link String} Roster ID to request data for
     * @return - {@link IOperationUntypedWithInput} export request, ready to execute
     */
    public static IOperationUntypedWithInput<Parameters> createExportOperation(IGenericClient exportClient, String rosterID) {
        return exportClient
                .operation()
                .onInstance(new IdType(rosterID))
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
     * @throws IOException - throws if unable to read the file
     */
    public static void createRosterSubmission(IGenericClient client, InputStream resource, UUID organizationID, Map<String, Reference> patientReferences) throws IOException {

        final Map<String, List<Pair<String, String>>> providerMap = SeedProcessor.extractProviderMap(resource);

        // Find the entry for the given key (yes, I know this is bad)
        providerMap
                .entrySet()
                .forEach(providerRoster -> {

                    final Group attributionRoster = SeedProcessor.generateAttributionGroup(providerRoster, organizationID, patientReferences);

                    // Now, submit the bundle
                    client
                            .create()
                            .resource(attributionRoster)
                            .encodedJson()
                            .execute();
                });
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
    public static JobCompletionModel awaitExportResponse(String jobLocation, String statusMessage, String token) throws IOException, InterruptedException {
        // Use the traditional HTTP Client to check the job status
        JobCompletionModel jobResponse = null;
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            final HttpGet jobGet = new HttpGet(jobLocation);
            jobGet.setHeader(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token));
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
