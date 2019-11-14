package gov.cms.dpc.testing.smoketests;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IOperationUntypedWithInput;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import gov.cms.dpc.common.models.JobCompletionModel;
import gov.cms.dpc.common.utils.SeedProcessor;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import gov.cms.dpc.fhir.FHIRExtractors;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.eclipse.jetty.http.HttpStatus;
import org.hl7.fhir.dstu3.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Shared methods for testing export jobs
 */
public class ClientUtils {
    private static final Logger logger = LoggerFactory.getLogger(ClientUtils.class);

    private ClientUtils() {
        // Not used
    }

    /**
     * Helper method for initiating and Export job and monitoring its success.
     *
     * @param exportClient - {@link IGenericClient} to use for export request. Ensure this contains the necessary authentication and HttpHeaders
     * @param providerNPIs - {@link List} of {@link String} of provider NPIs to use for exporting
     * @param httpClient   - {@link CloseableHttpClient} to use for executing non-FHIR HTTP requests
     */
    static void handleExportJob(IGenericClient exportClient, List<String> providerNPIs, CloseableHttpClient httpClient) {
        providerNPIs
                .stream()
                .map(npi -> exportRequestDispatcher(exportClient, npi))
                .map(search -> (Group) search.getEntryFirstRep().getResource())
                .map(group -> jobCompletionLambda(exportClient, httpClient, group))
                .forEach(jobResponse -> jobResponse.getOutput().forEach(entry -> {
                    jobResponseHandler(httpClient, entry);
                }));
    }

    /**
     * Helper method for creating an export request using the FHIR client.
     *
     * @param exportClient - {@link IGenericClient} client to use for the request.
     * @param rosterID     - {@link String} Roster ID to request data for
     * @return - {@link IOperationUntypedWithInput} export request, ready to execute
     */
    private static IOperationUntypedWithInput<Parameters> createExportOperation(IGenericClient exportClient, String rosterID) {
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
     * @param client            - {@link IGenericClient} client to use for request
     * @param resource          - {@link InputStream} representing test associations file
     * @param organizationID    - {@link UUID} of Organization to submit under
     * @param patientReferences - {@link Map} of patient's associated to a given provider
     * @throws IOException - throws if unable to read the file
     */
    private static void createRosterSubmission(IGenericClient client, InputStream resource, UUID organizationID, Map<String, Reference> patientReferences) throws IOException {

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
     * @param client        - {@link CloseableHttpClient} to use for non-FHIR requests
     * @return - {@link JobCompletionModel} Completed job response
     * @throws IOException          - throws if the HTTP request fails
     * @throws InterruptedException - throws if the thread is interrupted
     */
    private static JobCompletionModel awaitExportResponse(String jobLocation, String statusMessage, CloseableHttpClient client) throws IOException, InterruptedException {
        // Use the traditional HTTP Client to check the job status
        JobCompletionModel jobResponse = null;
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
        return jobResponse;
    }

    /**
     * Helper method to download a file from the DataResource
     * Uses the {@link File#createTempFile(String, String)} method to create the file handle
     *
     * @param fileID - {@link String} full URL of the file to download
     * @param client - {@link CloseableHttpClient} to use for non-FHIR requests
     * @return - {@link File} file handle where the data is stored
     * @throws IOException - throws if the HTTP request or file writing fails
     */
    private static File fetchExportedFiles(String fileID, CloseableHttpClient client) throws IOException {

        final File tempFile = File.createTempFile("dpc", ".ndjson");

        final HttpGet fileGet = new HttpGet(fileID);
        try (CloseableHttpResponse fileResponse = client.execute(fileGet)) {

            try (FileOutputStream outStream = new FileOutputStream(tempFile)) {
                fileResponse.getEntity().writeTo(outStream);
            }

            return tempFile;
        }
    }

    /**
     * Helper method for polling the Job Status endpoint until completion.
     * When complete, returns the {@link JobCompletionModel}
     *
     * @param exportOperation - {@link IOperationUntypedWithInput} to execute
     * @param client          - {@link CloseableHttpClient} to use for non-FHIR requests
     * @return - {@link JobCompletionModel}
     * @throws IOException          - throws if something bad happens
     * @throws InterruptedException - throws if someone cuts in line
     */
    private static JobCompletionModel monitorExportRequest(IOperationUntypedWithInput<Parameters> exportOperation, CloseableHttpClient client) throws IOException, InterruptedException {
        System.out.println("Retrying export request");

        // Return a MethodOutcome in order to get the response headers.
        final MethodOutcome outcome = exportOperation.returnMethodOutcome().execute();
        // Get the correct header
        final Map<String, List<String>> headers = outcome.getResponseHeaders();

        // Get the headers and check the status
        final String exportURL = headers.get("content-location").get(0);
        System.out.printf("Export job started. Progress URL: %s%n", exportURL);


        // Poll the job until it's done
        return awaitExportResponse(exportURL, "Checking job status", client);
    }

    private static Bundle exportRequestDispatcher(IGenericClient exportClient, String npi) {
        return exportClient
                .search()
                .forResource(Group.class)
                .where(Group.CHARACTERISTIC_VALUE
                        .withLeft(Group.CHARACTERISTIC.exactly().systemAndCode("", "attributed-to"))
                        .withRight(Group.VALUE.exactly().systemAndCode(DPCIdentifierSystem.NPPES.getSystem(), npi)))
                .returnBundle(Bundle.class)
                .encodedJson()
                .execute();
    }

    private static void jobResponseHandler(CloseableHttpClient client, JobCompletionModel.OutputEntry entry) {
        System.out.println(entry.getUrl());
        try {
            final File file = fetchExportedFiles(entry.getUrl(), client);
            System.out.println(String.format("Downloaded file to: %s", file.getPath()));
        } catch (IOException e) {
            throw new RuntimeException("Cannot output file", e);
        }
    }

    private static JobCompletionModel jobCompletionLambda(IGenericClient exportClient, CloseableHttpClient client, Group group) {
        final IOperationUntypedWithInput<Parameters> exportOperation = createExportOperation(exportClient, group.getId());
        try {
            return monitorExportRequest(exportOperation, client);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Error monitoring export", e);
        }
    }

    private static <T extends BaseResource> Bundle bundleSubmitter(Class<?> baseClass, Class<T> clazz, String filename, IParser parser, IGenericClient client) throws IOException {

        try (InputStream resource = baseClass.getClassLoader().getResourceAsStream(filename)) {
            final Bundle bundle = parser.parseResource(Bundle.class, resource);

            final Parameters parameters = new Parameters();
            parameters.addParameter().setResource(bundle);

            return client
                    .operation()
                    .onType(clazz)
                    .named("submit")
                    .withParameters(parameters)
                    .returnResourceType(Bundle.class)
                    .encodedJson()
                    .execute();
        }
    }

    static Map<String, Reference> submitPatients(String patientBundleFilename, Class<?> baseClass, FhirContext ctx, IGenericClient exportClient) {
        final Bundle patientBundle;

        try {
            System.out.println("Submitting patients");
            patientBundle = bundleSubmitter(baseClass, Patient.class, patientBundleFilename, ctx.newJsonParser(), exportClient);
        } catch (Exception e) {
            throw new RuntimeException("Cannot submit patients.", e);
        }

        final Map<String, Reference> patientReferences = new HashMap<>();
        patientBundle
                .getEntry()
                .stream()
                .map(Bundle.BundleEntryComponent::getResource)
                .map(resource -> (Patient) resource)
                .forEach(patient -> patientReferences.put(patient.getIdentifierFirstRep().getValue(), new Reference(patient.getId())));

        return patientReferences;
    }

    static List<String> submitPractitioners(String providerBundleFilename, Class<?> baseClass, FhirContext ctx, IGenericClient exportClient) {
        final Bundle providerBundle;

        try {
            System.out.println("Submitting practitioners");
            providerBundle = bundleSubmitter(baseClass, Practitioner.class, providerBundleFilename, ctx.newJsonParser(), exportClient);
        } catch (Exception e) {
            throw new RuntimeException("Cannot submit providers.", e);
        }

        // Get the provider NPIs
        return providerBundle
                .getEntry()
                .stream()
                .map(Bundle.BundleEntryComponent::getResource)
                .map(resource -> (Practitioner) resource)
                .map(FHIRExtractors::getProviderNPI)
                .collect(Collectors.toList());
    }

    static void createAndUploadRosters(String seedsFile, IGenericClient client, UUID organizationID, Map<String, Reference> patientReferences) throws IOException {
        // Read the provider bundle from the given file
        try (InputStream resource = new FileInputStream(new File(seedsFile))) {
            // Now, submit the bundle
            System.out.println("Uploading Patient roster");
            createRosterSubmission(client, resource, organizationID, patientReferences);
        }
    }
}
