package gov.cms.dpc.bluebutton.client;

import ca.uhn.fhir.rest.client.exceptions.FhirClientConnectionException;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Stage;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigRenderOptions;
import gov.cms.dpc.bluebutton.BlueButtonClientModule;
import gov.cms.dpc.bluebutton.config.BBClientConfiguration;
import gov.cms.dpc.fhir.DPCResourceType;
import gov.cms.dpc.testing.BufferedLoggerHandler;
import org.eclipse.jetty.http.HttpStatus;
import org.hl7.fhir.dstu3.model.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockserver.client.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.matchers.Times;
import org.mockserver.model.Header;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.Parameter;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.MissingResourceException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@Disabled("debug -acw")
@ExtendWith(BufferedLoggerHandler.class)
class BlueButtonClientTest {
    // A random example patient (Jane Doe)
    private static final String TEST_PATIENT_ID = "-20140000008325";
    // A patient that only has a single EOB record in bluebutton
    private static final String TEST_SINGLE_EOB_PATIENT_ID = "-20140000009893";
    // A patient id that should not exist in bluebutton
    private static final String TEST_NONEXISTENT_PATIENT_ID = "31337";
    private static final String TEST_EOB_TIMEOUT_PATIENT_ID = "-20140000008326";

    // Paths to test resources
    private static final String METADATA_PATH = "bb-test-data/meta.xml";
    private static final String SAMPLE_EOB_PATH_PREFIX = "bb-test-data/eob/";
    private static final String SAMPLE_COVERAGE_PATH_PREFIX = "bb-test-data/coverage/";
    private static final String SAMPLE_PATIENT_PATH_PREFIX = "bb-test-data/patient/";
    private static final String[] TEST_PATIENT_IDS = {"-20140000008325", "-20140000009893"};

    // lastUpdate date range to test
    private static final DateRangeParam TEST_LAST_UPDATED = new DateRangeParam().setUpperBoundInclusive(new java.util.Date());
    private static final String TEST_LAST_UPDATED_STRING = "le" + TEST_LAST_UPDATED.getUpperBound().getValueAsString();

    private static BlueButtonClient bbc;
    private static ClientAndServer mockServer;

    private static int timeoutDelay;
    private static int maxCount;
    private static String maxCountStr;

    @BeforeAll
    static void setupBlueButtonClient() {
        final BBClientConfiguration config = getClientConfig();

        // Delay necessary in mock server response to force a client timeout
        timeoutDelay = config.getTimeouts().getSocketTimeout() * 2;
        maxCount = config.getMaxResourcesCount();
        maxCountStr = Integer.toString(maxCount);

        final Injector injector = Guice.createInjector(Stage.DEVELOPMENT, new TestModule(), new BlueButtonClientModule<>(config));
        bbc = injector.getInstance(BlueButtonClient.class);

        mockServer = ClientAndServer.startClientAndServer(8083);
    }

    @AfterAll
    static void tearDown() {
        mockServer.stop();
    }

    @BeforeEach
    void resetExpectations() {
        // Reset mocks and mock capability statement
        new MockServerClient("localhost", 8083).reset();
        createMockServerExpectation("/v1/fhir/metadata", HttpStatus.OK_200, getRawXML(METADATA_PATH), List.of());
    }

    @Test
    void shouldGetFHIRFromPatientID() {
        createBasicScenario();

        Bundle ret = bbc.requestPatientFromServer(TEST_PATIENT_ID, TEST_LAST_UPDATED, null);
        // Verify that the bundle has one
        assertNotNull(ret, "The demo Patient object returned from BlueButtonClient should not be null");
        assertEquals(DPCResourceType.Bundle.getPath(), ret.getResourceType().getPath());
        assertEquals(1, ret.getEntry().size());
        assertEquals(DPCResourceType.Patient.getPath(), ret.getEntry().get(0).getResource().getResourceType().getPath());
        final var patient = (Patient) ret.getEntry().get(0).getResource();

        String patientDataCorrupted = "The demo Patient object data differs from what is expected";
        assertEquals(patient.getBirthDate(), java.sql.Date.valueOf("2014-06-01"), patientDataCorrupted);
        assertEquals("Unknown", patient.getGender().getDisplay(), patientDataCorrupted);
        assertEquals(1, patient.getName().size(), patientDataCorrupted);
        assertEquals("Doe", patient.getName().get(0).getFamily(), patientDataCorrupted);
        assertEquals("Jane", patient.getName().get(0).getGiven().get(0).toString(), patientDataCorrupted);
    }

    @Test
    void shouldGetFHIRFromPatientIDWithoutLastUpdated() {
        createBasicScenario();

        Bundle ret = bbc.requestPatientFromServer(TEST_PATIENT_ID, null, null);
        // Verify that the bundle has one
        assertNotNull(ret, "The demo Patient object returned from BlueButtonClient should not be null");
        assertEquals(DPCResourceType.Bundle.getPath(), ret.getResourceType().getPath());
        assertEquals(1, ret.getEntry().size());
        assertEquals(DPCResourceType.Patient.getPath(), ret.getEntry().get(0).getResource().getResourceType().getPath());
    }

    @Test
    void shouldGetFHIRFromPatientIDWithLastUpdated() {
        createBasicScenario();

        Bundle ret = bbc.requestPatientFromServer(TEST_PATIENT_ID, TEST_LAST_UPDATED, null);
        // Verify that the bundle has one
        assertNotNull(ret, "The demo Patient object returned from BlueButtonClient should not be null");
        assertEquals(DPCResourceType.Bundle.getPath(), ret.getResourceType().getPath());
        assertEquals(1, ret.getEntry().size());
        assertEquals(DPCResourceType.Patient.getPath(), ret.getEntry().get(0).getResource().getResourceType().getPath());
    }

    @Test
    void shouldGetEOBFromPatientID() {
        createBasicScenario();

        Bundle response = bbc.requestEOBFromServer(TEST_PATIENT_ID, TEST_LAST_UPDATED, null);
        assertNotNull(response, "The demo patient should have a non-null EOB bundle");
        assertEquals(10, response.getEntry().size(), "The demo patient's first bundle should have exactly 10 EOBs");
    }

    @Test
    void shouldNotHaveNextBundle() {
        createBasicScenario();

        Bundle response = bbc.requestEOBFromServer(TEST_SINGLE_EOB_PATIENT_ID, TEST_LAST_UPDATED, null);
        assertNotNull(response, "The demo patient should have a non-null EOB bundle");
        assertEquals(1, response.getEntry().size(), "The demo patient should have exactly 1 EOBs");
        assertNull(response.getLink(Bundle.LINK_NEXT), "Should have no next link since all the resources are in the bundle");
    }

    @Test
    void shouldHaveNextBundle() {
        createNextScenario();

        Bundle response = bbc.requestEOBFromServer(TEST_PATIENT_ID, TEST_LAST_UPDATED, null);
        assertNotNull(response, "The demo patient should have a non-null EOB bundle");
        assertNotNull(response.getLink(Bundle.LINK_NEXT), "Should have no next link since all the resources are in the bundle");
        Bundle nextResponse = bbc.requestNextBundleFromServer(response, null);
        assertNotNull(nextResponse, "Should have a next bundle");
        assertEquals(10, nextResponse.getEntry().size());
    }

    @Test
    void shouldReturnBundleContainingOnlyEOBs() {
        createBasicScenario();

        Bundle response = bbc.requestEOBFromServer(TEST_PATIENT_ID, TEST_LAST_UPDATED, null);
        response.getEntry().forEach(entry -> assertEquals(
                entry.getResource().getResourceType().getPath(),
                DPCResourceType.ExplanationOfBenefit.getPath(),
                "EOB bundles returned by the BlueButton client should only contain EOB objects"
        ));
    }

    @Test
    void shouldGetCoverageFromPatientID() {
        createBasicScenario();

        final Bundle response = bbc.requestCoverageFromServer(TEST_PATIENT_ID, TEST_LAST_UPDATED, null);
        assertNotNull(response, "The demo patient should have a non-null Coverage bundle");
        assertEquals(3, response.getEntry().size(), "The demo patient should have exactly 3 Coverage");
    }

    @Test
    void shouldReturnCapabilitiesStatement() {
        final CapabilityStatement statement = bbc.requestCapabilityStatement();
        assertNotNull(statement, "Should be able to request capabilities statement.");
        // We just need a simple test to verify that the statement is returned correctly.
        assertEquals(Enumerations.PublicationStatus.ACTIVE, statement.getStatus(), "Should have ACTIVE status from test metadata");
    }

    @Test
    void shouldHandlePatientsWithOnlyOneEOB() {
        createBasicScenario();

        final Bundle response = bbc.requestEOBFromServer(TEST_SINGLE_EOB_PATIENT_ID, TEST_LAST_UPDATED, null);
        assertEquals(1, response.getEntry().size(), "This demo patient should have exactly 1 EOB");
    }

    @Test
    void shouldThrowExceptionWhenResourceNotFound() {
        assertThrows(
                ResourceNotFoundException.class,
                () -> bbc.requestPatientFromServer(TEST_NONEXISTENT_PATIENT_ID, null, null),
                "BlueButton client should throw exceptions when asked to retrieve a non-existent patient"
        );

        assertThrows(
                ResourceNotFoundException.class,
                () -> bbc.requestEOBFromServer(TEST_NONEXISTENT_PATIENT_ID, null, null),
                "BlueButton client should throw exceptions when asked to retrieve EOBs for a non-existent patient"
        );
    }

    @Test
    void shouldThrowOnMultipleTimeoutsGettingResource() {
        createFetchBundleTimeOutScenario();

        assertThrows(
            FhirClientConnectionException.class,
            () -> bbc.requestEOBFromServer(TEST_EOB_TIMEOUT_PATIENT_ID, null, null),
            "BlueButton client should throw exception when request times out on all counts"
        );
    }

    @Test
    void shouldReturnOnSingleTimeoutGettingResource() {
        createFetchBundleTimeOutAndRecoverScenario();

        final Bundle response = bbc.requestEOBFromServer(TEST_EOB_TIMEOUT_PATIENT_ID, TEST_LAST_UPDATED, null);
        assertEquals(10, response.getEntry().size(), "This demo patient should have exactly 10 EOBs");
    }

    @Test
    void shouldThrowOnMultipleTimeoutsGettingNext() {
        createNextBundleTimeOutScenario();

        Bundle bundle = buildBundleWithNextEoB(TEST_EOB_TIMEOUT_PATIENT_ID, 10, 10);
        assertThrows(
            FhirClientConnectionException.class,
            () -> bbc.requestNextBundleFromServer(bundle, null),
            "BlueButton client should throw exception when request times out on all counts getting next bundle"
        );
    }

    @Test
    void shouldReturnOnSingleTimeoutGettingNext() {
        createNextBundleTimeOutAndRecoverScenario();

        Bundle response = bbc.requestNextBundleFromServer(
            buildBundleWithNextEoB(TEST_EOB_TIMEOUT_PATIENT_ID, maxCount, maxCount),
            null
        );
        assertEquals(10, response.getEntry().size(), "This demo patient should have exactly 10 EOBs");
    }

    /**
     * Helper method that configures the mock server to respond to a given GET request
     *
     * @param path          The path segment of the URL that would be received by BlueButton
     * @param respCode      The desired HTTP response code
     * @param payload       The data that the mock server should return in response to this GET request
     * @param qStringParams The query string parameters that must be present to generate this response
     */
    private static void createMockServerExpectation(String path, int respCode, String payload, List<Parameter> qStringParams) {
        createMockServerExpectation(path, respCode, payload, qStringParams, false);
    }

    /**
     * Helper method that configures the mock server to respond to a given GET request
     *
     * @param path          The path segment of the URL that would be received by BlueButton
     * @param respCode      The desired HTTP response code
     * @param payload       The data that the mock server should return in response to this GET request
     * @param qStringParams The query string parameters that must be present to generate this response
     * @param timeout         Should the server wait long enough to timeout?
     */
    private static void createMockServerExpectation(String path, int respCode, String payload, List<Parameter> qStringParams, boolean timeout) {
        int delay = timeout ? timeoutDelay : 1;

        new MockServerClient("localhost", 8083)
            .when(
                HttpRequest.request()
                    .withMethod("GET")
                    .withPath(path)
                    .withQueryStringParameters(qStringParams),
                Times.unlimited()
            )
            .respond(
                org.mockserver.model.HttpResponse.response()
                    .withStatusCode(respCode)
                    .withHeader(
                        new Header("Content-Type", "application/fhir+xml;charset=UTF-8")
                    )
                    .withBody(payload)
                    .withDelay(TimeUnit.MILLISECONDS, delay)
            );
    }

    private static BBClientConfiguration getClientConfig() {
        final String options = getTestConfig().getConfig("bbclient").root().render(ConfigRenderOptions.concise());

        try {
            return new ObjectMapper().readValue(options, BBClientConfiguration.class);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Config getTestConfig() {
        return ConfigFactory.load("test.application.conf");
    }

    private static String getRawXML(String path) {
        try(InputStream sampleData = BlueButtonClientTest.class.getClassLoader().getResourceAsStream(path)) {
            if (sampleData == null) {
                throw new MissingResourceException("Cannot find sample requests", BlueButtonClientTest.class.getName(), path);
            }

            return new String(sampleData.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            fail("Failed opening path: %s".formatted(path));
            return "";
        }
    }

    // Creates a test bundle with a next link so we don't have to rely on pre-created test data
    private static Bundle buildBundleWithNextEoB(String patientId, int count, int startIndex) {
        String nextUrl = "http://localhost:8083/v1/fhir/ExplanationOfBenefit?patient=%s&_count=%d&startIndex=%d&excludeSAMHSA=true"
            .formatted(patientId, count, startIndex);

        return new Bundle()
            .addLink(
                new Bundle.BundleLinkComponent(
                    new StringType(Bundle.LINK_NEXT),
                    new UriType(nextUrl)
                )
            );
    }

    // Sets up basic Patient, EoB and Coverage mocks for our test patients
    private void createBasicScenario() {
        for (String patientId : TEST_PATIENT_IDS) {
            createMockServerExpectation(
                "/v1/fhir/Patient",
                HttpStatus.OK_200,
                getRawXML(SAMPLE_PATIENT_PATH_PREFIX + patientId + ".xml"),
                List.of(
                    Parameter.param("_id", patientId),
                    Parameter.param("_lastUpdated", TEST_LAST_UPDATED_STRING))
            );

            createMockServerExpectation(
                "/v1/fhir/Patient",
                HttpStatus.OK_200,
                getRawXML(SAMPLE_PATIENT_PATH_PREFIX + patientId + ".xml"),
                Collections.singletonList(Parameter.param("_id", patientId))
            );

            createMockServerExpectation(
                "/v1/fhir/ExplanationOfBenefit",
                HttpStatus.OK_200,
                getRawXML(SAMPLE_EOB_PATH_PREFIX + patientId + ".xml"),
                List.of(
                    Parameter.param("patient", patientId),
                    Parameter.param("excludeSAMHSA", "true"),
                    Parameter.param("_lastUpdated", TEST_LAST_UPDATED_STRING))
            );

            createMockServerExpectation(
                "/v1/fhir/Coverage",
                HttpStatus.OK_200,
                getRawXML(SAMPLE_COVERAGE_PATH_PREFIX + patientId + ".xml"),
                List.of(
                    Parameter.param("beneficiary", "Patient/" + patientId),
                    Parameter.param("_lastUpdated", TEST_LAST_UPDATED_STRING))
            );
        }

        // Create bundle for next link of default test patient
        createMockServerExpectation(
            "/v1/fhir/ExplanationOfBenefit",
            HttpStatus.OK_200,
            getRawXML(SAMPLE_EOB_PATH_PREFIX + TEST_PATIENT_ID + "_10.xml"),
            List.of(Parameter.param("patient", TEST_PATIENT_ID),
                Parameter.param("startIndex", maxCountStr),
                Parameter.param("excludeSAMHSA", "true"))
        );
    }

    // Includes the default scenario, and then adds a mock for the "next" bundle of the default patient
    void createNextScenario() {
        createBasicScenario();

        // Create bundle for next link of default test patient
        createMockServerExpectation(
            "/v1/fhir/ExplanationOfBenefit",
            HttpStatus.OK_200,
            getRawXML(SAMPLE_EOB_PATH_PREFIX + TEST_PATIENT_ID + "_" + maxCountStr + ".xml"),
            List.of(Parameter.param("patient", TEST_PATIENT_ID),
                Parameter.param("startIndex", maxCountStr),
                Parameter.param("excludeSAMHSA", "true"))
        );
    }

    // Create mock scenario for EoB requests that should time out
    void createFetchBundleTimeOutScenario() {
        // All should timeout
        createMockServerExpectation(
            "/v1/fhir/ExplanationOfBenefit",
            HttpStatus.OK_200,
            "Not needed",
            List.of(
                Parameter.param("patient", TEST_EOB_TIMEOUT_PATIENT_ID),
                Parameter.param("_count", maxCountStr),
                Parameter.param("excludeSAMHSA", "true")
            ),
            true
        );

        createMockServerExpectation(
            "/v1/fhir/ExplanationOfBenefit",
            HttpStatus.OK_200,
            "Not needed",
            List.of(
                Parameter.param("patient", TEST_EOB_TIMEOUT_PATIENT_ID),
                Parameter.param("_count", Integer.toString(maxCount /2)),
                Parameter.param("excludeSAMHSA", "true")
            ),
            true
        );
    }

    // Create mock scenario for EoB requests that should time out and recover
    void createFetchBundleTimeOutAndRecoverScenario() {
        // First should timeout
        createMockServerExpectation(
            "/v1/fhir/ExplanationOfBenefit",
            HttpStatus.OK_200,
            "Not needed",
            List.of(
                Parameter.param("patient", TEST_EOB_TIMEOUT_PATIENT_ID),
                Parameter.param("_count", maxCountStr),
                Parameter.param("excludeSAMHSA", "true")
            ),
            true
        );

        createMockServerExpectation(
            "/v1/fhir/ExplanationOfBenefit",
            HttpStatus.OK_200,
            getRawXML(SAMPLE_EOB_PATH_PREFIX + TEST_EOB_TIMEOUT_PATIENT_ID + ".xml"),
            List.of(
                Parameter.param("patient", TEST_EOB_TIMEOUT_PATIENT_ID),
                Parameter.param("_count", Integer.toString(maxCount /2)),
                Parameter.param("excludeSAMHSA", "true")
            )
        );
    }

    // Mock scenario for EoB next link that should time out for all counts
    void createNextBundleTimeOutScenario() {
        createMockServerExpectation(
            "/v1/fhir/ExplanationOfBenefit",
            HttpStatus.OK_200,
            "Not needed",
            List.of(
                Parameter.param("patient", TEST_EOB_TIMEOUT_PATIENT_ID),
                Parameter.param("_count", maxCountStr),
                Parameter.param("startIndex", maxCountStr),
                Parameter.param("excludeSAMHSA", "true")
            ),
            true
        );

        createMockServerExpectation(
            "/v1/fhir/ExplanationOfBenefit",
            HttpStatus.OK_200,
            "Not needed",
            List.of(
                Parameter.param("patient", TEST_EOB_TIMEOUT_PATIENT_ID),
                Parameter.param("_count", Integer.toString(maxCount /2)),
                Parameter.param("startIndex", maxCountStr),
                Parameter.param("excludeSAMHSA", "true")
            ),
            true
        );
    }

    // Mock scenario for EoB next link that should time out for first request and recover on second
    void createNextBundleTimeOutAndRecoverScenario() {
        createMockServerExpectation(
            "/v1/fhir/ExplanationOfBenefit",
            HttpStatus.OK_200,
            "Not needed",
            List.of(
                Parameter.param("patient", TEST_EOB_TIMEOUT_PATIENT_ID),
                Parameter.param("_count", maxCountStr),
                Parameter.param("startIndex", maxCountStr),
                Parameter.param("excludeSAMHSA", "true")
            ),
            true
        );

        createMockServerExpectation(
            "/v1/fhir/ExplanationOfBenefit",
            HttpStatus.OK_200,
            getRawXML(SAMPLE_EOB_PATH_PREFIX + TEST_EOB_TIMEOUT_PATIENT_ID + "_10.xml"),
            List.of(
                Parameter.param("patient", TEST_EOB_TIMEOUT_PATIENT_ID),
                Parameter.param("_count", Integer.toString(maxCount /2)),
                Parameter.param("startIndex", maxCountStr),
                Parameter.param("excludeSAMHSA", "true")
            )
        );
    }
}
