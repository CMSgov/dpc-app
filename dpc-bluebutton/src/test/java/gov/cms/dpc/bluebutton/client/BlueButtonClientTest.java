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
import java.sql.Date;
import java.util.Collections;
import java.util.List;
import java.util.MissingResourceException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(BufferedLoggerHandler.class)
class BlueButtonClientTest {
    // A random example patient (Jane Doe)
    private static final String TEST_PATIENT_ID = "20140000008325";
    // A patient that only has a single EOB record in bluebutton
    private static final String TEST_SINGLE_EOB_PATIENT_ID = "20140000009893";
    // A patient id that should not exist in bluebutton
    private static final String TEST_NONEXISTENT_PATIENT_ID = "31337";
    // A random example patient's HICN (Jane Doe)
    private static final String TEST_HICN = "1000079035";
    private static final String TEST_HICN_HASH = "96228a57f37efea543f4f370f96f1dbf01c3e3129041dba3ea4367545507c6e7";

    // Paths to test resources
    private static final String METADATA_PATH = "bb-test-data/meta.xml";
    private static final String SAMPLE_EOB_PATH_PREFIX = "bb-test-data/eob/";
    private static final String SAMPLE_COVERAGE_PATH_PREFIX = "bb-test-data/coverage/";
    private static final String SAMPLE_PATIENT_PATH_PREFIX = "bb-test-data/patient/";
    private static final String[] TEST_PATIENT_IDS = {"20140000008325", "20140000009893"};

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
                    Collections.singletonList(Parameter.param("patient", patientId))
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
                getRawXML(SAMPLE_PATIENT_PATH_PREFIX + "hicn-" + TEST_HICN + ".xml"),
                Collections.singletonList(Parameter.param("identifier", "http://bluebutton.cms.hhs.gov/identifier#hicnHash|" + TEST_HICN_HASH))
        );

        // Create mocks for pages of the results
        for(String startIndex: List.of("10", "20", "30")) {
            createMockServerExpectation(
                "/v1/fhir/ExplanationOfBenefit",
                HttpStatus.OK_200,
                getRawXML(SAMPLE_EOB_PATH_PREFIX + TEST_PATIENT_ID + "_" + startIndex + ".xml"),
                List.of(Parameter.param("patient", TEST_PATIENT_ID),
                        Parameter.param("count", "10"),
                        Parameter.param("startIndex", startIndex))
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
    void shouldGetPatientFromHICN() {
        Bundle ret = bbc.searchPatientFromServerByHICN(TEST_HICN);

        // Verify basic demo patient information
        assertNotNull(ret, "The demo Patient bundle returned from BlueButtonClient should not be null");

        Resource resource = ret.getEntry().get(0).getResource();
        assertEquals(ResourceType.Patient, resource.getResourceType());

        Patient patient = (Patient) resource;
        assertEquals("Jane X", patient.getName().get(0).getGivenAsSingleString());
        assertEquals("Doe", patient.getName().get(0).getFamily());
        assertEquals("Female", patient.getGender().getDisplay());
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
    void shouldHashHICN() {
        String hash = BlueButtonClientImpl.hashHICN("1000067585", "b8ebdcc47fdd852b8b0201835c6273a9177806e84f2d9dc4f7ecaff08681e86d74195c6aef2db06d3d44c9d0b8f93c3e6c43d90724b605ac12585b9ab5ee9c3f00d5c0d284e6b8e49d502415c601c28930637b58fdca72476e31c22ad0f24ecd761020d6a4bcd471f0db421d21983c0def1b66a49a230f85f93097e9a9a8e0a4f4f0add775213cbf9ecfc1a6024cb021bd1ed5f4981a4498f294cca51d3939dfd9e6a1045350ddde7b6d791b4d3b884ee890d4c401ef97b46d1e57d40efe5737248dd0c4cec29c23c787231c4346cab9bb973f140a32abaa0a2bd5c0b91162f8d2a7c9d3347aafc76adbbd90ec5bfe617a3584e94bc31047e3bb6850477219a9", 1000);
        assertEquals("b67baee938a551f06605ecc521cc329530df4e088e5a2d84bbdcc047d70faff4", hash);

        hash = BlueButtonClientImpl.hashHICN("test", "b8ebdcc47fdd852b8b0201835c6273a9177806e84f2d9dc4f7ecaff08681e86d74195c6aef2db06d3d44c9d0b8f93c3e6c43d90724b605ac12585b9ab5ee9c3f00d5c0d284e6b8e49d502415c601c28930637b58fdca72476e31c22ad0f24ecd761020d6a4bcd471f0db421d21983c0def1b66a49a230f85f93097e9a9a8e0a4f4f0add775213cbf9ecfc1a6024cb021bd1ed5f4981a4498f294cca51d3939dfd9e6a1045350ddde7b6d791b4d3b884ee890d4c401ef97b46d1e57d40efe5737248dd0c4cec29c23c787231c4346cab9bb973f140a32abaa0a2bd5c0b91162f8d2a7c9d3347aafc76adbbd90ec5bfe617a3584e94bc31047e3bb6850477219a9", 1000);
        assertEquals("4a9da0c7242e3daf90ca3451af9910923f03cce61cb9d23d47b5ea046d0165df", hash);
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
