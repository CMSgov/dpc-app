package gov.cms.dpc.testing.smoketests;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import com.github.nitram509.jmacaroons.MacaroonVersion;
import com.github.nitram509.jmacaroons.MacaroonsBuilder;
import com.google.common.base.Splitter;
import gov.cms.dpc.common.utils.NPIUtil;
import gov.cms.dpc.fhir.FHIRExtractors;
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
import org.hl7.fhir.dstu3.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.Security;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class SmokeTest extends AbstractJavaSamplerClient {

    private static final Logger logger = LoggerFactory.getLogger(SmokeTest.class);
    private static final String KEY_ID = "smoke-test-key";

    private FhirContext fhirContext;
    private String organizationID;
    private String goldenMacaroon;
    private String apiHost;
    private String apiAdminUrl;
    private String providerBundleFileLoc;
    private String patientBundleFileLoc;
    private String seedFileLoc;

    public SmokeTest() {
        // Not used
    }

    @Override
    public Arguments getDefaultParameters() {
        final Arguments arguments = new Arguments();
        arguments.addArgument("host", "http://localhost:3002/v1");
        arguments.addArgument("admin-url", "http://localhost:3002/tasks");
        arguments.addArgument("attribution-url", "http://localhost:3500/v1");
        arguments.addArgument("seed-file", "src/main/resources/test_associations-dpr.csv");
        arguments.addArgument("provider-bundle", "provider_bundle.json");
        arguments.addArgument("patient-bundle", "patient_bundle-dpr.json");
        arguments.addArgument("organization-ids", "");

        return arguments;
    }

    @Override
    public void setupTest(JavaSamplerContext context) {
        super.setupTest(context);
        Security.addProvider(new BouncyCastleProvider());
        apiHost = context.getParameter("host");
        apiAdminUrl = context.getParameter("admin-url");
        fhirContext = FhirContext.forDstu3();
        goldenMacaroon = createGoldenMacaroon();
        organizationID = getTestOrganizationId(context);
        providerBundleFileLoc = context.getParameter("provider-bundle");
        patientBundleFileLoc = context.getParameter("patient-bundle");
        seedFileLoc = context.getParameter("seed-file");
        prepareEnvForTest(organizationID, apiHost);
    }

    @Override
    public void teardownTest(JavaSamplerContext context) {
        final IGenericClient client = APIAuthHelpers.buildAdminClient(fhirContext, apiHost, goldenMacaroon, true, true);
        try {
            logger.info("Post Test Cleanup. Deleting organization: {} host: {}", organizationID, apiHost);
            deleteOrg(organizationID,client);
        } catch (Exception e) {
            logger.error(String.format("Cannot delete organization: {}", organizationID, e));
            System.exit(1);
        }
        super.teardownTest(context);
    }

    @Override
    public SampleResult runTest(JavaSamplerContext context) {
        logger.info("Running against {}", apiHost);
        logger.info("Admin URL: {}", apiAdminUrl);
        logger.info("Running with {} threads", JMeterContextService.getNumberOfThreads());

        final SampleResult smokeTestSampler = new SampleResult();
        smokeTestSampler.setSampleLabel("Smoke Test");
        smokeTestSampler.setSuccessful(false);
        smokeTestSampler.sampleStart();

        String clientToken = registerOrg(organizationID, smokeTestSampler);
        Pair<UUID, PrivateKey> keyTuple = generateAndUploadKey();
        final IGenericClient exportClient = APIAuthHelpers.buildAuthenticatedClient(fhirContext, apiHost, clientToken, keyTuple.getLeft(), keyTuple.getRight(), true, true);

        Bundle providerBundle = submitPractitionerBundle(exportClient,smokeTestSampler);
        Map<String, Reference> patientReferences = submitPatientBundle(exportClient,smokeTestSampler);
        submitRosters(exportClient,smokeTestSampler, providerBundle, patientReferences);
        exportDataAndHandleResults(exportClient,providerBundle,clientToken,keyTuple);

        smokeTestSampler.setSuccessful(true);
        smokeTestSampler.sampleEnd();
        logger.info("Test completed");
        return smokeTestSampler;
    }

    private  String getTestOrganizationId(JavaSamplerContext javaSamplerContext){
        String orgIdsString = javaSamplerContext.getParameter("organization-ids");
        if(orgIdsString == null){
            throw new IllegalArgumentException("Missing organization-ids argument.");
        }
        List<String> orgIds = Splitter.on(',').splitToList(orgIdsString);
        int currThreadNum = javaSamplerContext.getJMeterContext().getThreadNum();
        if(currThreadNum+1 > orgIds.size()){
            throw new IllegalArgumentException("Not enough test org ids provided. The number of threads must be less than or equal to the number of test org ids.");
        }
        return orgIds.get(currThreadNum);
    }
    private void exportDataAndHandleResults(IGenericClient exportClient, Bundle providerBundle, String clientToken, Pair<UUID,PrivateKey> keyTuple){
        // Create a custom http client to use for monitoring the non-FHIR export request
        try (CloseableHttpClient httpClient = APIAuthHelpers.createCustomHttpClient()
                .trusting()
                .isAuthed(apiHost, clientToken, keyTuple.getKey(), keyTuple.getRight())
                .build()) {

            List<String> providerNPIs = providerBundle
                    .getEntry()
                    .stream()
                    .map(Bundle.BundleEntryComponent::getResource)
                    .map(resource -> (Practitioner) resource)
                    .map(FHIRExtractors::getProviderNPI)
                    .collect(Collectors.toList());

            ClientUtils.handleExportJob(exportClient, providerNPIs, httpClient, apiAdminUrl);
        } catch (IOException e) {
            throw new IllegalStateException("Somehow, could not monitor export response", e);
        }
    }

    private void submitRosters(IGenericClient client, SampleResult parentSampler, Bundle providerBundle, Map<String, Reference> patientReferences){
        logger.debug("Uploading roster");
        try {
            ClientUtils.createAndUploadRosters(seedFileLoc, providerBundle, client, UUID.fromString(organizationID), patientReferences);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot upload roster", e);
        }finally {

        }
    }

    private Map<String, Reference>  submitPatientBundle(IGenericClient client, SampleResult parentSampler){
        logger.debug("Submitting patients");
        final SampleResult patientSample = new SampleResult();
        patientSample.setSampleLabel("Patient Submission");
        patientSample.setSuccessful(false);
        patientSample.sampleStart();

        try {
            final Map<String, Reference> patientReferences = ClientUtils.submitPatients(patientBundleFileLoc, this.getClass(), fhirContext, client);
            patientSample.setSuccessful(true);
            return patientReferences;
        } catch (Exception e) {
            throw new IllegalStateException("Cannot submit patients", e);
        } finally {
            patientSample.sampleEnd();
            parentSampler.addSubResult(patientSample);
        }
    }

    private Bundle submitPractitionerBundle(IGenericClient client, SampleResult parentSampler){
        logger.debug("Submitting practitioners");
        final SampleResult practitionerUploadSampler = new SampleResult();
        practitionerUploadSampler.setSampleLabel("Practitioner Submission");
        practitionerUploadSampler.setSuccessful(false);
        practitionerUploadSampler.sampleStart();
        Bundle providerBundle;
        try {
            providerBundle = ClientUtils.submitPractitioners(providerBundleFileLoc, this.getClass(), fhirContext, client);
            practitionerUploadSampler.setSuccessful(true);
            return providerBundle;
        } catch (Exception e) {
            throw new IllegalStateException("Cannot submit practitioners", e);
        } finally {
            practitionerUploadSampler.sampleEnd();
            parentSampler.addSubResult(practitionerUploadSampler);
        }
    }

    private Pair<UUID,PrivateKey> generateAndUploadKey(){
        try {
            return APIAuthHelpers.generateAndUploadKey(KEY_ID, organizationID, goldenMacaroon, apiHost);
        } catch (IOException | URISyntaxException | GeneralSecurityException e) {
            throw new IllegalStateException("Failed uploading public key", e);
        }
    }

    private String registerOrg(String organizationID, SampleResult parentSampleResult){
        final IGenericClient adminClient = APIAuthHelpers.buildAdminClient(fhirContext, apiHost, goldenMacaroon, true, true);
        final SampleResult sampleResult = new SampleResult();
        sampleResult.setSampleLabel("Register Org");
        sampleResult.setSuccessful(false);
        sampleResult.sampleStart();

        try {
            String npi = NPIUtil.generateNPI();
            String clientToken =  FHIRHelpers.registerOrganization(adminClient, fhirContext.newJsonParser(), organizationID, npi, apiAdminUrl);
            sampleResult.setSuccessful(true);
            return clientToken;
        } catch (Exception e) {
            throw new IllegalStateException("Cannot register org", e);
        } finally {
            sampleResult.sampleEnd();
            parentSampleResult.addSubResult(sampleResult);
        }
    }

    private  void prepareEnvForTest(String organizationID, String host){
        final IGenericClient adminClient = APIAuthHelpers.buildAdminClient(fhirContext, host, goldenMacaroon, true, true);
        logger.info("Preparing for smoke tests.");
        logger.info("Retrieving organization with id: {}",organizationID);
        Organization org = getOrg(organizationID,goldenMacaroon);
        if(org != null){
            try {
                logger.info("Organization with id {} was found. Deleting organization.",organizationID);
                deleteOrg(organizationID,adminClient);
            }catch (Exception e){
                logger.error("Could not delete org {} while preparing for test.",organizationID, adminClient);
                System.exit(1);
            }
        }else{
            logger.info("No conflicting organization with id {} was found. Ready for smoke testing.",organizationID);
        }
    }

    private Organization getOrg(String orgId, String goldenMacaroon){

        final String orgSpecificGoldenMacaroon = MacaroonsBuilder
                .modify(MacaroonsBuilder.deserialize(goldenMacaroon).get(0))
                .add_first_party_caveat(String.format("organization_id = %s", orgId))
                .getMacaroon().serialize(MacaroonVersion.SerializationVersion.V2_JSON);

        IGenericClient orgSpecificAdminClient = APIAuthHelpers.buildAdminClient(fhirContext, apiHost, orgSpecificGoldenMacaroon, true, true);

        try {
            return orgSpecificAdminClient
                    .read()
                    .resource(Organization.class)
                    .withId(orgId)
                    .encodedJson()
                    .execute();
        } catch (ResourceNotFoundException e) {
            return null;
        }
    }

    private void deleteOrg(String orgId, IGenericClient client){
            client.delete()
                    .resourceById(new IdType("Organization", orgId))
                    .encodedJson()
                    .execute();
    }

    private String createGoldenMacaroon(){
        try {
            return APIAuthHelpers.createGoldenMacaroon(apiAdminUrl);
        } catch (Exception e) {
            throw new IllegalStateException("Failed creating Macaroon", e);
        }
    }
}