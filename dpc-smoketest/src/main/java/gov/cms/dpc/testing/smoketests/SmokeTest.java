package gov.cms.dpc.testing.smoketests;

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
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.java.sampler.AbstractJavaSamplerClient;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;
import org.eclipse.jetty.http.HttpStatus;
import org.hl7.fhir.dstu3.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

public class SmokeTest extends AbstractJavaSamplerClient {

    private static final Logger logger = LoggerFactory.getLogger(SmokeTest.class);

    private FhirContext ctx;

    @Override
    public Arguments getDefaultParameters() {
        final Arguments arguments = new Arguments();
        arguments.addArgument("host", "http://localhost:3002/v1");
        arguments.addArgument("attribution-url", "http://localhost:3500/v1");
        arguments.addArgument("seed-file", "src/main/resources/test_associations.csv");

        return arguments;
    }

    @Override
    public void setupTest(JavaSamplerContext context) {
        super.setupTest(context);
    }

    @Override
    public SampleResult runTest(JavaSamplerContext javaSamplerContext) {
        // Create things
        final String organizationID = UUID.randomUUID().toString();
        logger.debug("Creating organization {}", organizationID);
        // Disable validation against Attribution service
        this.ctx = FhirContext.forDstu3();
        ctx.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);
        ctx.getRestfulClientFactory().setConnectTimeout(1800);
        final String attributionURL = javaSamplerContext.getParameter("attribution-url");
        final IGenericClient attributionClient = ctx.newRestfulGenericClient(attributionURL);

        final SampleResult smokeTestResult = new SampleResult();
        smokeTestResult.sampleStart();

        final SampleResult orgRegistrationResult = new SampleResult();
        smokeTestResult.addSubResult(orgRegistrationResult);


        String token = null;
        orgRegistrationResult.sampleStart();
        try {
            token = FHIRHelpers.registerOrganization(attributionClient, ctx.newJsonParser(), organizationID, attributionURL);
            orgRegistrationResult.setSuccessful(true);
        } catch (IOException e) {
            orgRegistrationResult.setSuccessful(false);
        } finally {
            orgRegistrationResult.sampleEnd();
        }

        // Create an authenticated and async client (the async part is ignored by other endpoints)
        final IGenericClient exportClient = ClientUtils.createExportClient(ctx, javaSamplerContext.getParameter("host"), token);

        // Upload a batch of patients and a batch of providers
        logger.debug("Submitting practitioners");
        final SampleResult practitionerSample = new SampleResult();
        practitionerSample.sampleStart();
        final List<String> providerNPIs = this.submitPractitioners(exportClient);
        practitionerSample.sampleEnd();
        practitionerSample.setSuccessful(true);
        smokeTestResult.addSubResult(practitionerSample);

        logger.debug("Submitting patients");
        final SampleResult patientSample = new SampleResult();

        patientSample.sampleStart();
        final Map<String, Reference> patientReferences = this.submitPatients(exportClient);
        patientSample.setSuccessful(true);
        patientSample.sampleEnd();
        smokeTestResult.addSubResult(patientSample);

        // Upload the roster bundle
        logger.debug("Uploading roster");
        try {
            this.createAndUploadRosters(javaSamplerContext.getParameter("seed-file"), exportClient, UUID.fromString(organizationID), patientReferences);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Run the job
        this.handleExportJob(exportClient, providerNPIs, token);
        smokeTestResult.setSuccessful(true);

        logger.info("Test completed");
        return smokeTestResult;
    }

    private void createAndUploadRosters(String seedFile, IGenericClient client, UUID organizationID, Map<String, Reference> patientReferences) throws IOException {
        // Read the provider bundle from the given file
        try (InputStream resource = new FileInputStream(new File(seedFile))) {
            // Now, submit the bundle
            System.out.println("Uploading Patient roster");
            ClientUtils.createRosterSubmission(client, resource, organizationID, patientReferences);
        } catch (Exception e) {
            logger.error("Cannot open seeds file.", e);
            System.exit(1);
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

    private void handleExportJob(IGenericClient exportClient, List<String> providerNPIs, String token) {
        providerNPIs
                .stream()
                .map(npi -> {
                    logger.warn("Finding with NPI: {}", npi);
                    return exportClient
                            .search()
                            .forResource(Group.class)
                            .where(Group.CHARACTERISTIC_VALUE
                                    .withLeft(Group.CHARACTERISTIC.exactly().systemAndCode("", "attributed-to"))
                                    .withRight(Group.VALUE.exactly().systemAndCode(DPCIdentifierSystem.NPPES.getSystem(), npi)))
                            .returnBundle(Bundle.class)
                            .encodedJson()
                            .execute();
                })
                .map(search -> {
                    logger.warn("Total number of groups: {}", search.getTotal());
                    return (Group) search.getEntryFirstRep().getResource();
                })
                .map(group -> {
                    logger.debug("Exporting with Group {}", group.getId());
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
                        final File file = ClientUtils.fetchExportedFiles(entry.getUrl(), token);
                        System.out.println(String.format("Downloaded file to: %s", file.getPath()));
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
