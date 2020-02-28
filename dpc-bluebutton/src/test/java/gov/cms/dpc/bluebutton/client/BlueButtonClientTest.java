package gov.cms.dpc.bluebutton.client;

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
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import gov.cms.dpc.testing.BufferedLoggerHandler;
import org.eclipse.jetty.http.HttpStatus;
import org.hl7.fhir.dstu3.model.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockserver.client.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.matchers.Times;
import org.mockserver.model.Header;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.Parameter;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.sql.Date;
import java.util.Collections;
import java.util.List;
import java.util.MissingResourceException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(BufferedLoggerHandler.class)
class BlueButtonClientTest {
    // A random example patient (Jane Doe)
    private static final String TEST_PATIENT_ID = "-20140000008325";
    private static final String TEST_PATIENT_MBI_HASH = "6a288931dd0a911809e977093b1257e344fb29df3f5eacb622aadade8adcc581";
    // A patient that only has a single EOB record in bluebutton
    private static final String TEST_SINGLE_EOB_PATIENT_ID = "-20140000009893";
    // A patient id that should not exist in bluebutton
    private static final String TEST_NONEXISTENT_PATIENT_ID = "31337";

    // Paths to test resources
    private static final String METADATA_PATH = "bb-test-data/meta.xml";
    private static final String SAMPLE_EOB_PATH_PREFIX = "bb-test-data/eob/";
    private static final String SAMPLE_COVERAGE_PATH_PREFIX = "bb-test-data/coverage/";
    private static final String SAMPLE_PATIENT_PATH_PREFIX = "bb-test-data/patient/";
    private static final String[] TEST_PATIENT_IDS = {"-20140000008325", "-20140000009893"};

    private static BlueButtonClient bbc;
    private static ClientAndServer mockServer;
    private static Config conf;

    @BeforeAll
    static void setupBlueButtonClient() throws IOException {
        conf = getTestConfig();
        final Injector injector = Guice.createInjector(Stage.DEVELOPMENT, new TestModule(), new BlueButtonClientModule<>(getClientConfig()));
        bbc = injector.getInstance(BlueButtonClient.class);

        mockServer = ClientAndServer.startClientAndServer(conf.getInt("test.mockServerPort"));
        createMockServerExpectation("/v1/fhir/metadata", HttpStatus.OK_200, getRawXML(METADATA_PATH), List.of());

        for (String patientId : TEST_PATIENT_IDS) {
            createMockServerExpectation(
                    "/v1/fhir/Patient/" + patientId,
                    HttpStatus.OK_200,
                    getRawXML(SAMPLE_PATIENT_PATH_PREFIX + patientId + ".xml"),
                    List.of()
            );

            createMockServerExpectation(
                    "/v1/fhir/ExplanationOfBenefit",
                    HttpStatus.OK_200,
                    getRawXML(SAMPLE_EOB_PATH_PREFIX + patientId + ".xml"),
                    List.of(Parameter.param("patient", patientId),
                        Parameter.param("excludeSAMHSA", "true"))
            );

            createMockServerExpectation(
                    "/v1/fhir/Coverage",
                    HttpStatus.OK_200,
                    getRawXML(SAMPLE_COVERAGE_PATH_PREFIX + patientId + ".xml"),
                    Collections.singletonList(Parameter.param("beneficiary", "Patient/" + patientId))
            );
        }

        createMockServerExpectation(
                "/v1/fhir/Patient",
                HttpStatus.OK_200,
                getRawXML(SAMPLE_PATIENT_PATH_PREFIX + TEST_PATIENT_ID + "-bundle.xml"),
                Collections.singletonList(Parameter.param("identifier", DPCIdentifierSystem.MBI_HASH.getSystem() + "|" + TEST_PATIENT_MBI_HASH))
        );

        // Create mocks for pages of the results
        for(String startIndex: List.of("10", "20", "30")) {
            createMockServerExpectation(
                "/v1/fhir/ExplanationOfBenefit",
                HttpStatus.OK_200,
                getRawXML(SAMPLE_EOB_PATH_PREFIX + TEST_PATIENT_ID + "_" + startIndex + ".xml"),
                List.of(Parameter.param("patient", TEST_PATIENT_ID),
                        Parameter.param("count", "10"),
                        Parameter.param("startIndex", startIndex),
                        Parameter.param("excludeSAMHSA", "true"))
            );
        }
    }

    @AfterAll
    static void tearDown() {
        mockServer.stop();
    }

    @Test
    void shouldGetFHIRFromPatientID() {
        Patient ret = bbc.requestPatientFromServer(TEST_PATIENT_ID);

        // Verify basic demo patient information
        assertNotNull(ret, "The demo Patient object returned from BlueButtonClient should not be null");

        String patientDataCorrupted = "The demo Patient object data differs from what is expected";
        assertEquals(ret.getBirthDate(), Date.valueOf("2014-06-01"), patientDataCorrupted);
        assertEquals(ret.getGender().getDisplay(), "Unknown", patientDataCorrupted);
        assertEquals(ret.getName().size(), 1, patientDataCorrupted);
        assertEquals(ret.getName().get(0).getFamily(), "Doe", patientDataCorrupted);
        assertEquals(ret.getName().get(0).getGiven().get(0).toString(), "Jane", patientDataCorrupted);
    }

    @Test
    void shouldGetFHIRFromPatientMbiHash() {
        Bundle ret = bbc.requestPatientFromServerByMbiHash(TEST_PATIENT_MBI_HASH);

        assertNotNull(ret, "Bundle returned from BlueButtonClient should not be null");
        assertTrue(ret.hasEntry(), "Bundle should have an entry");
        assertEquals(1, ret.getEntry().size(), "Entry should have size 1");
        Patient pt = (Patient) ret.getEntryFirstRep().getResource();

        String patientDataCorrupted = "The demo Patient object data differs from what is expected";
        assertEquals(pt.getBirthDate(), Date.valueOf("2014-06-01"), patientDataCorrupted);
        assertEquals(pt.getGender().getDisplay(), "Unknown", patientDataCorrupted);
        assertEquals(pt.getName().size(), 1, patientDataCorrupted);
        assertEquals(pt.getName().get(0).getFamily(), "Doe", patientDataCorrupted);
        assertEquals(pt.getName().get(0).getGiven().get(0).toString(), "Jane", patientDataCorrupted);
    }

    @Test
    void shouldGetEOBFromPatientID() {
        Bundle response = bbc.requestEOBFromServer(TEST_PATIENT_ID);

        assertNotNull(response, "The demo patient should have a non-null EOB bundle");
        assertEquals(32, response.getTotal(), "The demo patient should have exactly 32 EOBs");
    }

    @Test
    void shouldNotHaveNextBundle() {
        Bundle response = bbc.requestEOBFromServer(TEST_SINGLE_EOB_PATIENT_ID);

        assertNotNull(response, "The demo patient should have a non-null EOB bundle");
        assertEquals(1, response.getTotal(), "The demo patient should have exactly 1 EOBs");
        assertNull(response.getLink(Bundle.LINK_NEXT), "Should have no next link since all the resources are in the bundle");
    }

    @Test
    void shouldHaveNextBundle() {
        Bundle response = bbc.requestEOBFromServer(TEST_PATIENT_ID);

        assertNotNull(response, "The demo patient should have a non-null EOB bundle");
        assertNotNull(response.getLink(Bundle.LINK_NEXT), "Should have no next link since all the resources are in the bundle");
        Bundle nextResponse = bbc.requestNextBundleFromServer(response);
        assertNotNull(nextResponse, "Should have a next bundle");
        assertEquals(10, nextResponse.getEntry().size());
    }

    @Test
    void shouldReturnBundleContainingOnlyEOBs() {
        Bundle response = bbc.requestEOBFromServer(TEST_PATIENT_ID);

        response.getEntry().forEach((entry) -> assertEquals(
                entry.getResource().getResourceType(),
                ResourceType.ExplanationOfBenefit,
                "EOB bundles returned by the BlueButton client should only contain EOB objects"
        ));
    }

    @Test
    void shouldGetCoverageFromPatientID() {
        final Bundle response = bbc.requestCoverageFromServer(TEST_PATIENT_ID);

        assertNotNull(response, "The demo patient should have a non-null Coverage bundle");
        assertEquals(3, response.getTotal(), "The demo patient should have exactly 3 Coverage");
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
        final Bundle response = bbc.requestEOBFromServer(TEST_SINGLE_EOB_PATIENT_ID);
        assertEquals(1, response.getTotal(), "This demo patient should have exactly 1 EOB");
    }

    @Test
    void shouldThrowExceptionWhenResourceNotFound() {
        assertThrows(
                ResourceNotFoundException.class,
                () -> bbc.requestPatientFromServer(TEST_NONEXISTENT_PATIENT_ID),
                "BlueButton client should throw exceptions when asked to retrieve a non-existent patient"
        );

        assertThrows(
                ResourceNotFoundException.class,
                () -> bbc.requestEOBFromServer(TEST_NONEXISTENT_PATIENT_ID),
                "BlueButton client should throw exceptions when asked to retrieve EOBs for a non-existent patient"
        );
    }

    @Test
    void shouldHashMbi() throws GeneralSecurityException {
        // Cases from BFD tests https://github.com/CMSgov/beneficiary-fhir-data/blob/master/apps/bfd-pipeline/bfd-pipeline-rif-load/src/test/java/gov/cms/bfd/pipeline/rif/load/RifLoaderTest.java
        String hash = bbc.hashMbi("123456789A");
        assertEquals("d95a418b0942c7910fb1d0e84f900fe12e5a7fd74f312fa10730cc0fda230e9a", hash);

        hash = bbc.hashMbi("3456789");
        assertEquals("ec49dc08f8dd8b4e189f623ab666cfc8b81f201cc94fe6aef860a4c3bd57f278", hash);
    }

    @Test
    void shouldNotHashMbi() throws GeneralSecurityException {
        String hash = bbc.hashMbi(null);
        assertEquals("", hash);

        hash = bbc.hashMbi("");
        assertEquals("", hash);
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
        new MockServerClient("localhost", conf.getInt("test.mockServerPort"))
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
                                .withDelay(TimeUnit.SECONDS, 1)
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

    private static String getRawXML(String path) throws IOException {
        InputStream sampleData = BlueButtonClientTest.class.getClassLoader().getResourceAsStream(path);

        if (sampleData == null) {
            throw new MissingResourceException("Cannot find sample requests", BlueButtonClientTest.class.getName(), path);
        }

        return new String(sampleData.readAllBytes(), StandardCharsets.UTF_8);
    }
}
