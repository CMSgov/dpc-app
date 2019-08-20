package gov.cms.dpc.testing.smoketests;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import gov.cms.dpc.api.client.ClientUtils;
import gov.cms.dpc.fhir.helpers.FHIRHelpers;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.java.sampler.AbstractJavaSamplerClient;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.threads.JMeterContextService;
import org.hl7.fhir.dstu3.model.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
        final String hostParam = javaSamplerContext.getParameter("host");
        logger.info("Running against {}", hostParam);
        logger.info("Running with {} threads", JMeterContextService.getNumberOfThreads());

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
        final IGenericClient exportClient = ClientUtils.createExportClient(ctx, hostParam, token);

        // Upload a batch of patients and a batch of providers
        logger.debug("Submitting practitioners");
        final SampleResult practitionerSample = new SampleResult();
        practitionerSample.sampleStart();
        final List<String> providerNPIs = ClientUtils.submitPractitioners(this.getClass(), ctx, exportClient);
        practitionerSample.sampleEnd();
        practitionerSample.setSuccessful(true);
        smokeTestResult.addSubResult(practitionerSample);

        logger.debug("Submitting patients");
        final SampleResult patientSample = new SampleResult();

        patientSample.sampleStart();
        final Map<String, Reference> patientReferences = ClientUtils.submitPatients(this.getClass(), ctx, exportClient);
        patientSample.setSuccessful(true);
        patientSample.sampleEnd();
        smokeTestResult.addSubResult(patientSample);

        // Upload the roster bundle
        logger.debug("Uploading roster");
        try {
            ClientUtils.createAndUploadRosters(javaSamplerContext.getParameter("seed-file"), exportClient, UUID.fromString(organizationID), patientReferences);
        } catch (IOException e) {
            throw new RuntimeException("Cannot upload roster", e);
        }

        // Run the job
        ClientUtils.handleExportJob(exportClient, providerNPIs, token);
        smokeTestResult.setSuccessful(true);

        logger.info("Test completed");
        return smokeTestResult;
    }
}
