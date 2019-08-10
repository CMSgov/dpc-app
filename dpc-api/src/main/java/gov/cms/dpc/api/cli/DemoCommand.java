package gov.cms.dpc.api.cli;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import ca.uhn.fhir.rest.client.exceptions.NonFhirResponseException;
import ca.uhn.fhir.rest.gclient.IOperationUntypedWithInput;
import gov.cms.dpc.api.client.ClientUtils;
import gov.cms.dpc.api.models.JobCompletionModel;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import gov.cms.dpc.fhir.FHIRExtractors;
import gov.cms.dpc.fhir.helpers.FHIRHelpers;
import io.dropwizard.cli.Command;
import io.dropwizard.setup.Bootstrap;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import org.eclipse.jetty.http.HttpStatus;
import org.hl7.fhir.dstu3.model.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static gov.cms.dpc.api.client.ClientUtils.createExportOperation;

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
        final List<String> providerNPIs = this.submitPractitioners(exportClient);
        final Map<String, Reference> patientReferences = this.submitPatients(exportClient);

        // Upload the roster bundle
        this.createAndUploadRosters(namespace, exportClient, UUID.fromString(organizationID), patientReferences);

        // Run the job
        this.handleExportJob(exportClient, providerNPIs, token);

        System.out.println("Export jobs completed");
        System.exit(0);
    }

    private void createAndUploadRosters(Namespace namespace, IGenericClient client, UUID organizationID, Map<String, Reference> patientReferences) throws IOException {
        // Read the provider bundle from the given file
        final String seedsFile = getSeedsFile(namespace);
        try (InputStream resource = new FileInputStream(new File(seedsFile))) {
            // Now, submit the bundle
            System.out.println("Uploading Patient roster");
            ClientUtils.createRosterSubmission(client, resource, organizationID, patientReferences);
        }
    }

    private JobCompletionModel monitorExportRequest(IOperationUntypedWithInput<Parameters> exportOperation, String token) throws IOException, InterruptedException {
        System.out.println("Retrying export request");
        String exportURL = "";

        try {
            exportOperation.execute();
        } catch (NonFhirResponseException e) {
            if (e.getStatusCode() != HttpStatus.NO_CONTENT_204) {
                e.printStackTrace();
                System.exit(1);
            }

            // Get the correct header
            final Map<String, List<String>> headers = e.getResponseHeaders();

            // Get the headers and check the status
            exportURL = headers.get("content-location").get(0);
            System.out.printf("Export job started. Progress URL: %s%n", exportURL);
        }

        // Poll the job until it's done
        return ClientUtils.awaitExportResponse(exportURL, "Checking job status", token);
    }

    private static String getSeedsFile(Namespace namespace) {
        return namespace.getString("seed-file");
    }

    private static String buildBaseURL(Namespace namespace) {
        return namespace.getString("hostname");
    }

    private <T extends BaseResource> Bundle bundleSubmitter(Class<T> clazz, String filename, IParser parser, IGenericClient client) throws IOException {

        try (InputStream resource = this.getClass().getClassLoader().getResourceAsStream(filename)) {
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

    private void handleExportJob(IGenericClient exportClient, List<String> providerNPIs, String  token) {
        providerNPIs
                .stream()
                .map(npi -> exportClient
                        .search()
                        .forResource(Group.class)
                        .where(Group.CHARACTERISTIC_VALUE
                                .withLeft(Group.CHARACTERISTIC.exactly().systemAndCode("", "attributed-to"))
                                .withRight(Group.VALUE.exactly().systemAndCode(DPCIdentifierSystem.NPPES.getSystem(), npi)))
                        .returnBundle(Bundle.class)
                        .encodedJson()
                        .execute())
                .map(search -> (Group) search.getEntryFirstRep().getResource())
                .map(group -> {
                    final IOperationUntypedWithInput<Parameters> exportOperation = createExportOperation(exportClient, group.getId());
                    try {
                        return monitorExportRequest(exportOperation, token);
                    } catch (IOException | InterruptedException e) {
                        throw new RuntimeException("Error monitoring export", e);
                    }
                })
                .forEach(jobResponse -> jobResponse.getOutput().forEach(entry -> {
                    System.out.println(entry.getUrl());
                    try {
                        ClientUtils.fetchExportedFiles(entry.getUrl());
                    } catch (IOException e) {
                        throw new RuntimeException("Cannot output file", e);
                    }
                }));
    }

    private Map<String, Reference> submitPatients(IGenericClient exportClient) {
        final Bundle patientBundle;

        try {
            System.out.println("Submitting patients");
            patientBundle = bundleSubmitter(Patient.class, "patient_bundle.json", ctx.newJsonParser(), exportClient);
        } catch (Exception e) {
            throw new RuntimeException("Cannot submit patients.", e);
        }

        final Map<String, Reference> patientReferences = new HashMap<>();
        patientBundle
                .getEntry()
                .stream()
                .map(Bundle.BundleEntryComponent::getResource)
                .map(resource -> (Patient) resource)
                .forEach(patient -> {
                    patientReferences.put(patient.getIdentifierFirstRep().getValue(), new Reference(patient.getId()));
                });

        return patientReferences;
    }

    private List<String> submitPractitioners(IGenericClient exportClient) {
        final Bundle providerBundle;

        try {
            System.out.println("Submitting practitioners");
            providerBundle = bundleSubmitter(Practitioner.class, "provider_bundle.json", ctx.newJsonParser(), exportClient);
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
}
