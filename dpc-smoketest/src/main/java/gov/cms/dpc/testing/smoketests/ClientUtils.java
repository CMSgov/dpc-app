package gov.cms.dpc.testing.smoketests;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IOperationUntypedWithInput;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import gov.cms.dpc.common.models.JobCompletionModel;
import gov.cms.dpc.common.utils.SeedProcessor;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import gov.cms.dpc.fhir.FHIRExtractors;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.eclipse.jetty.http.HttpStatus;
import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.dstu3.model.codesystems.V3RoleClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Shared methods for testing export jobs
 */
public class ClientUtils {
    private static final Logger logger = LoggerFactory.getLogger(ClientUtils.class);

    private ClientUtils() {
        // Not used
    }

    /* TODO When prod environment has DNS entries for prod.dpc.cms.gov, revert the workarounds in this file
     * - revert adding the overrideURL argument to all methods that have it
     * - remove workaround code in awaitExportResponse()
     * - git diff f2d3abe1f23e4d1ad2f2a01 5d799c57712418de674 <<< green is good
     * see also https://github.com/CMSgov/dpc-app/pull/849
     */

    /**
     * Helper method for initiating and Export job and monitoring its success.
     * @param exportClient - {@link IGenericClient} to use for export request. Ensure this contains the necessary authentication and HttpHeaders
     * @param providerNPIs - {@link List} of {@link String} of provider NPIs to use for exporting
     * @param httpClient   - {@link CloseableHttpClient} to use for executing non-FHIR HTTP requests
     * @param overrideURL  - overrides the url used for checking jobs; only useful when DNS is not active in an environment
     */
    static void handleExportJob(IGenericClient exportClient, List<String> providerNPIs, CloseableHttpClient httpClient, String overrideURL) {
        providerNPIs
                .stream()
                .map(npi -> exportRequestDispatcher(exportClient, npi))
                .map(search -> (Group) search.getEntryFirstRep().getResource())
                .map(group -> jobCompletionLambda(exportClient, httpClient, group, overrideURL))
                .peek(jobResponse -> {
                    if (jobResponse.getError().size() > 0)
                        throw new IllegalStateException("Export job completed, but with errors");
                })
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
    private static void createRosterSubmission(IGenericClient client, InputStream resource, Bundle providerBundle, UUID organizationID, Map<String, Reference> patientReferences) throws IOException {
        final FhirContext ctx = FhirContext.forDstu3();

        final Map<String, List<Pair<String, String>>> providerMap = SeedProcessor.extractProviderMap(resource);
        final Map<String, String> providerNPIUUIDMap = providerBundle
                .getEntry()
                .stream()
                .map(Bundle.BundleEntryComponent::getResource)
                .map(Practitioner.class::cast)
                .collect(Collectors.toMap(FHIRExtractors::getProviderNPI, p -> FHIRExtractors.getEntityUUID(p.getId()).toString()));

        // Find the entry for the given key (yes, I know this is bad)
        providerMap
                .entrySet()
                .forEach(providerRoster -> {
                    final Group attributionRoster = SeedProcessor.generateAttributionGroup(providerRoster, organizationID, patientReferences);

                    // Create the attestation
                    final Provenance provenance = createAttestation(organizationID, providerNPIUUIDMap.get(providerRoster.getKey()));

                    // Now, submit the bundle
                    client
                            .create()
                            .resource(attributionRoster)
                            .withAdditionalHeader("X-Provenance", ctx.newJsonParser().encodeResourceToString(provenance))
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
    private static JobCompletionModel awaitExportResponse(String jobLocation, String statusMessage, CloseableHttpClient client, String overrideURL) throws IOException, InterruptedException {
        // Use the traditional HTTP Client to check the job status
        JobCompletionModel jobResponse = null;
        // TODO When DNS is set for prod, revert this workaround code; see TODO at the top of this class
        String jobLocationURL = jobLocation;
        if (jobLocation.startsWith("https://prod.dpc.cms.gov/api/v1")) {
            jobLocationURL = overrideURL.substring(0, overrideURL.indexOf("/api/v1")) + jobLocation.substring("https://prod.dpc.cms.gov".length());
            logger.info("patched job url " + jobLocationURL);
        }
        final HttpGet jobGet = new HttpGet(jobLocationURL);
        boolean done = false;

        while (!done) {
            Thread.sleep(1000);
            logger.debug(statusMessage);
            try (CloseableHttpResponse response = client.execute(jobGet)) {
                final int statusCode = response.getStatusLine().getStatusCode();
                done = statusCode == HttpStatus.OK_200 || statusCode > 300;
                if (done) {
                    final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
                    // If we're done, make sure we completed successfully, otherwise, throw an error
                    if (statusCode > 300) {
                        throw new IllegalStateException(String.format("Awaiting export results failed with status %d: %s", statusCode, EntityUtils.toString(response.getEntity())));
                    }
                    String responseBody = "";
                    try {
                        Package pack = JsonFormat.class.getPackage();
                        logger.info("JsonFormat class: {}", pack);

                        responseBody = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
                        jobResponse = mapper.readValue(responseBody, JobCompletionModel.class);
                    } catch (JsonParseException | NoSuchFieldError e) {
                        logger.error("Failed to parse job status response: {}", responseBody);
                        throw e;
                    }
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
    private static JobCompletionModel monitorExportRequest(IOperationUntypedWithInput<Parameters> exportOperation, CloseableHttpClient client, String overrideURL) throws IOException, InterruptedException {
        System.out.println("Retrying export request");

        // Return a MethodOutcome in order to get the response headers.
        final MethodOutcome outcome = exportOperation.returnMethodOutcome().execute();
        // Get the correct header
        final Map<String, List<String>> headers = outcome.getResponseHeaders();

        // Get the headers and check the status
        final String exportURL = headers.get("content-location").get(0);
        logger.info("Export job started. Progress URL: {}", exportURL);

        // Poll the job until it's done
        return awaitExportResponse(exportURL, "Checking job status", client, overrideURL);
    }

    private static Bundle exportRequestDispatcher(IGenericClient exportClient, String npi) {
        return exportClient
                .search()
                .forResource(Group.class)
                .where(Group.CHARACTERISTIC_VALUE
                        .withLeft(Group.CHARACTERISTIC.exactly().code("attributed-to"))
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
            if(file.length() == 0){
                throw new IllegalStateException(String.format("Downloaded file was empty. file path:  %s", file.getPath()));
            }
        } catch (IOException e) {
            throw new RuntimeException("Cannot output file", e);
        }
    }
    
    private static JobCompletionModel jobCompletionLambda(IGenericClient exportClient, CloseableHttpClient client, Group group, String overrideURL) {
        final IOperationUntypedWithInput<Parameters> exportOperation = createExportOperation(exportClient, group.getId());
        try {
            return monitorExportRequest(exportOperation, client, overrideURL);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(String.format("Error monitoring export groupID: %s", group.getId()), e);
        }
    }

    private static <T extends BaseResource> Bundle bundleSubmitter(Class<?> baseClass, Class<T> clazz, String filename, IParser parser, IGenericClient client) throws IOException {

        try (InputStream resource = baseClass.getClassLoader().getResourceAsStream(filename)) {
            final Parameters parameters = parser.parseResource(Parameters.class, resource);

            final Bundle bundle = (Bundle) parameters.getParameterFirstRep().getResource();

            bundle
                    .getEntry()
                    .stream()
                    .map(Bundle.BundleEntryComponent::getResource)
                    .filter(entry -> entry.getClass().equals(clazz))
                    .forEach(entry -> client
                            .create()
                            .resource(entry)
                            .encodedJson()
                            .execute());

            // Fetch the new bundle, so we make sure we get the IDs that we're after
            return client
                    .search()
                    .forResource(clazz)
                    .returnBundle(Bundle.class)
                    .encodedJson()
                    .execute();
        }
    }

    static Map<String, Reference> submitPatients(String patientBundleFilename, Class<?> baseClass, FhirContext ctx, IGenericClient exportClient) throws IOException {
        final Bundle patientBundle;

        System.out.println("Submitting patients");
        patientBundle = bundleSubmitter(baseClass, Patient.class, patientBundleFilename, ctx.newJsonParser(), exportClient);

        final Map<String, Reference> patientReferences = new HashMap<>();
        patientBundle
                .getEntry()
                .stream()
                .map(Bundle.BundleEntryComponent::getResource)
                .map(resource -> (Patient) resource)
                .forEach(patient -> patientReferences.put(patient.getIdentifierFirstRep().getValue(), new Reference(patient.getId())));

        return patientReferences;
    }

    static Bundle submitPractitioners(String providerBundleFilename, Class<?> baseClass, FhirContext ctx, IGenericClient exportClient) throws IOException {
        final Bundle providerBundle;

        System.out.println("Submitting practitioners");
        providerBundle = bundleSubmitter(baseClass, Practitioner.class, providerBundleFilename, ctx.newJsonParser(), exportClient);

        return providerBundle;
    }

    static void createAndUploadRosters(String seedsFile, Bundle providerBundle, IGenericClient client, UUID organizationID, Map<String, Reference> patientReferences) throws IOException {
        // Read the provider bundle from the given file
        try (InputStream resource = new FileInputStream(new File(seedsFile))) {
            System.out.println("Uploading Patient roster");
            createRosterSubmission(client, resource, providerBundle, organizationID, patientReferences);
        }
    }

    private static Provenance createAttestation(UUID organizationID, String practitioner) {
        final Provenance provenance = new Provenance();
        provenance.setRecorded(Date.from(Instant.now()));

        provenance.addReason().setSystem("http://hl7.org/fhir/v3/ActReason").setCode("TREAT");

        // Add an agent
        final Provenance.ProvenanceAgentComponent agent = new Provenance.ProvenanceAgentComponent();
        agent.setWho(new Reference(new IdType("Organization", organizationID.toString())));
        agent.setOnBehalfOf(new Reference(new IdType("Practitioner", practitioner)));

        final Coding roleCode = new Coding();
        roleCode.setSystem(V3RoleClass.AGNT.getSystem());
        roleCode.setCode(V3RoleClass.AGNT.toCode());

        final CodeableConcept roleConcept = new CodeableConcept();
        roleConcept.addCoding(roleCode);
        agent.setRole(Collections.singletonList(roleConcept));
        provenance.addAgent(agent);

        return provenance;
    }
}
