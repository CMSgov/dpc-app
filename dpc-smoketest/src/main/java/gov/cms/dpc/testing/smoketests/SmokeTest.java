package gov.cms.dpc.testing.smoketests;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import gov.cms.dpc.fhir.helpers.FHIRHelpers;
import gov.cms.dpc.testing.APIAuthHelpers;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.java.sampler.AbstractJavaSamplerClient;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.threads.JMeterContextService;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Security;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SmokeTest extends AbstractJavaSamplerClient {

    private static final Logger logger = LoggerFactory.getLogger(SmokeTest.class);
    private static final String KEY_ID = "smoke-test-key";

    private FhirContext ctx;
    private String organizationID;
    private String goldenMacaroon;

    public SmokeTest() {
        // Not used
    }

    @Override
    public Arguments getDefaultParameters() {
        final Arguments arguments = new Arguments();
        arguments.addArgument("host", "http://localhost:3002/v1");
        arguments.addArgument("admin-url", "http://localhost:3002/tasks");
        arguments.addArgument("attribution-url", "http://localhost:3500/v1");
        arguments.addArgument("seed-file", "src/main/resources/test_associations.csv");
        arguments.addArgument("provider-bundle", "provider_bundle.json");
        arguments.addArgument("patient-bundle", "patient_bundle.json");
        arguments.addArgument("organization-id", "");
        arguments.addArgument("client-token", "");
        arguments.addArgument("private-key", "");
        arguments.addArgument("key-id", "");

        return arguments;
    }

    @Override
    public void setupTest(JavaSamplerContext context) {
        super.setupTest(context);
        Security.addProvider(new BouncyCastleProvider());
    }

    @Override
    public void teardownTest(JavaSamplerContext context) {
        final String hostParam = context.getParameter("host");
        // Remove the organization, which should delete it all
        System.out.println(String.format("Deleting organization %s", organizationID));
        // Build admin client for removing the organization
        final IGenericClient client = APIAuthHelpers.buildAdminClient(ctx, hostParam, goldenMacaroon, true);

        try {
            client
                    .delete()
                    .resourceById(new IdType("Organization", this.organizationID))
                    .encodedJson()
                    .execute();
        } catch (Exception e) {
            System.err.println(String.format("Cannot remove organization: %s", e.getMessage()));
            System.exit(-1);
        }

        super.teardownTest(context);
    }

    @Override
    public SampleResult runTest(JavaSamplerContext javaSamplerContext) {
        // Create things
        final String hostParam = javaSamplerContext.getParameter("host");
        final String adminURL = javaSamplerContext.getParameter("admin-url");
        logger.info("Running against {}", hostParam);
        logger.info("Admin URL: {}", adminURL);
        logger.info("Running with {} threads", JMeterContextService.getNumberOfThreads());

        this.organizationID = javaSamplerContext.getParameter("organization-id");
        String clientToken = javaSamplerContext.getParameter("client-token");
        String privateKeyPath = javaSamplerContext.getParameter("private-key");
        final String keyID = javaSamplerContext.getParameter("key-id");

        final SampleResult smokeTestResult = new SampleResult();
        smokeTestResult.setSampleLabel("Smoke Test");
        // False, unless proven otherwise
        smokeTestResult.setSuccessful(false);
        smokeTestResult.sampleStart();

        // Disable validation against Attribution service
        this.ctx = FhirContext.forDstu3();

        // If we're not supplied all the init parameters, create a new org
        Pair<UUID, PrivateKey> keyTuple;
        if (organizationID.equals("") || clientToken.equals("") || privateKeyPath.equals("") || keyID.equals("")) {
            this.organizationID = UUID.randomUUID().toString();

            System.out.println(String.format("Creating organization %s", organizationID));

            try {
                this.goldenMacaroon = APIAuthHelpers.createGoldenMacaroon(adminURL);
            } catch (Exception e) {
                throw new IllegalStateException("Failed creating Macaroon", e);
            }
            // Create admin client for registering organization
            final IGenericClient adminClient = APIAuthHelpers.buildAdminClient(ctx, hostParam, goldenMacaroon, true);

            final SampleResult orgRegistrationResult = new SampleResult();
            smokeTestResult.addSubResult(orgRegistrationResult);

            orgRegistrationResult.sampleStart();
            try {
                clientToken = FHIRHelpers.registerOrganization(adminClient, ctx.newJsonParser(), organizationID, adminURL);
                orgRegistrationResult.setSuccessful(true);
            } catch (Exception e) {
                orgRegistrationResult.setSuccessful(false);
                throw new IllegalStateException("Cannot register org", e);
            } finally {
                orgRegistrationResult.sampleEnd();
            }

            // Create a new public key
            try {
                keyTuple = APIAuthHelpers.generateAndUploadKey(KEY_ID, organizationID, goldenMacaroon, hostParam);
            } catch (IOException | NoSuchAlgorithmException | URISyntaxException e) {
                throw new IllegalStateException("Failed uploading public key", e);
            }
        } else {
            // Parse the private key and create a new ID/PrivateKey tuple
            final Path path = Paths.get(privateKeyPath);
            try (final PEMParser pemParser = new PEMParser(Files.newBufferedReader(path, StandardCharsets.UTF_8))) {
                JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");
                Object object = pemParser.readObject();
                KeyPair kp = converter.getKeyPair((PEMKeyPair) object);
                PrivateKey privateKey = kp.getPrivate();
                if (privateKey == null) {
                    throw new IllegalStateException("Key cannot be null");
                }
                keyTuple = Pair.of(UUID.fromString(keyID), privateKey);
            } catch (IOException e) {
                throw new IllegalArgumentException(String.format("Cannot read private key from: %s", privateKeyPath));
            }
        }
        // Create an authenticated and async client (the async part is ignored by other endpoints)
        final IGenericClient exportClient;

        exportClient = APIAuthHelpers.buildAuthenticatedClient(ctx, hostParam, clientToken, keyTuple.getLeft(), keyTuple.getRight(), true);

        // Upload a batch of patients and a batch of providers
        logger.debug("Submitting practitioners");
        final SampleResult practitionerSample = new SampleResult();
        practitionerSample.setSampleLabel("Practitioner submission");
        practitionerSample.sampleStart();
        final List<String> providerNPIs;
        try {
            providerNPIs = ClientUtils.submitPractitioners(javaSamplerContext.getParameter("provider-bundle"), this.getClass(), ctx, exportClient);
            practitionerSample.setSuccessful(true);
        } catch (Exception e) {
            practitionerSample.setSuccessful(false);
            throw new IllegalStateException("Cannot submit practitioners", e);
        } finally {
            practitionerSample.sampleEnd();
        }

        smokeTestResult.addSubResult(practitionerSample);

        logger.debug("Submitting patients");
        final SampleResult patientSample = new SampleResult();
        patientSample.setSampleLabel("Patient submission");
        patientSample.sampleStart();
        final Map<String, Reference> patientReferences;
        try {
            patientReferences = ClientUtils.submitPatients(javaSamplerContext.getParameter("patient-bundle"), this.getClass(), ctx, exportClient);
            patientSample.setSuccessful(true);
        } catch (Exception e) {
            patientSample.setSuccessful(false);
            throw new IllegalStateException("Cannot submit patients", e);
        } finally {
            patientSample.sampleEnd();
            smokeTestResult.addSubResult(patientSample);
        }


        // Upload the roster bundle
        logger.debug("Uploading roster");
        try {
            ClientUtils.createAndUploadRosters(javaSamplerContext.getParameter("seed-file"), exportClient, UUID.fromString(organizationID), patientReferences);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot upload roster", e);
        }

        // Run the job
        // Create a custom http client to use for monitoring the non-FHIR export request
        try (CloseableHttpClient httpClient = APIAuthHelpers.createCustomHttpClient()
                .trusting()
                .isAuthed(hostParam, clientToken, keyTuple.getKey(), keyTuple.getRight())
                .build()) {
            ClientUtils.handleExportJob(exportClient, providerNPIs, httpClient);
            smokeTestResult.setSuccessful(true);

            logger.info("Test completed");
            return smokeTestResult;
        } catch (IOException e) {
            throw new IllegalStateException("Somehow, could not monitor export response", e);
        }
    }
}
