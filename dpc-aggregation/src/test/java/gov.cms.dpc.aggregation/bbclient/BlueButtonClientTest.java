package gov.cms.dpc.aggregation.bbclient;

import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.typesafe.config.Config;
import org.hl7.fhir.dstu3.model.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockserver.client.server.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.matchers.Times;
import org.mockserver.model.Header;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.Parameter;

import java.io.*;
import java.sql.Date;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class BlueButtonClientTest {
    // A random example patient (Jane Doe)
    private static final String TEST_PATIENT_ID = "20140000008325";
    // A patient that only has a single EOB record in bluebutton
    private static final String TEST_SINGLE_EOB_PATIENT_ID = "20140000009893";
    // A patient id that should not exist in bluebutton
    private static final String TEST_NONEXISTENT_PATIENT_ID = "31337";

    // Paths to test resources
    private static final String METADATA_PATH = "bb-test-data/meta.xml";
    private static final String SAMPLE_EOB_PATH_PREFIX = "bb-test-data/eob/";
    private static final String SAMPLE_PATIENT_PATH_PREFIX = "bb-test-data/patient/";
    private static final String[] TEST_PATIENT_IDS = {"20140000008325", "20140000009893"};

    private static BlueButtonClient bbc;
    private static ClientAndServer mockServer;
    private static Config conf;

    @BeforeAll
    public static void setupBlueButtonClient() throws IOException {
        final Injector injector = Guice.createInjector(new TestModule(), new BlueButtonClientModule());
        bbc = injector.getInstance(BlueButtonClient.class);
        conf  = injector.getInstance(Config.class);

        mockServer = ClientAndServer.startClientAndServer(conf.getInt("test.mockServerPort"));
        createMockServerExpectation("/v1/fhir/metadata", 200, getRawXML(METADATA_PATH), List.of());

        for(String patientId : TEST_PATIENT_IDS) {
            createMockServerExpectation(
                    "/v1/fhir/Patient/" + patientId,
                    200,
                    getRawXML(SAMPLE_PATIENT_PATH_PREFIX + patientId + ".xml"),
                    List.of()
            );

            createMockServerExpectation(
                    "/v1/fhir/ExplanationOfBenefit",
                    200,
                    getRawXML(SAMPLE_EOB_PATH_PREFIX + patientId + ".xml"),
                    Arrays.asList(Parameter.param("patient", patientId))
            );
        }
    }

    @AfterAll
    public static void tearDown() {
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
    void shouldGetEOBFromPatientID() {
        Bundle response = bbc.requestEOBBundleFromServer(TEST_PATIENT_ID);

        assertNotNull(response, "The demo patient should have a non-null EOB bundle");
        assertEquals(response.getTotal(), 32, "The demo patient should have exactly 32 EOBs");
    }

    @Test
    void shouldReturnBundleContainingOnlyEOBs() {
        Bundle response = bbc.requestEOBBundleFromServer(TEST_PATIENT_ID);

        response.getEntry().forEach((entry) -> {
            assertEquals(
                    entry.getResource().getResourceType(),
                    ResourceType.ExplanationOfBenefit,
                    "EOB bundles returned by the BlueButton client should only contain EOB objects"
            );
        });
    }

    @Test
    void shouldHandlePatientsWithOnlyOneEOB() {
        Bundle response = bbc.requestEOBBundleFromServer(TEST_SINGLE_EOB_PATIENT_ID);

        assertEquals(response.getTotal(), 1, "This demo patient should have exactly 1 EOB");
    }

    @Test
    void shouldThrowExceptionWhenResourceNotFound() {
        assertThrows(
                ResourceNotFoundException.class, () -> {
                    bbc.requestPatientFromServer(TEST_NONEXISTENT_PATIENT_ID);
                },
                "BlueButton client should throw exceptions when asked to retrieve a non-existent patient"
        );

        assertThrows(
                ResourceNotFoundException.class, () -> {
                    bbc.requestEOBBundleFromServer(TEST_NONEXISTENT_PATIENT_ID);
                },
                "BlueButton client should throw exceptions when asked to retrieve EOBs for a non-existent patient"
        );
    }

    /**
     * Helper method that configures the mock server to respond to a given GET request
     *
     * @param path The path segment of the URL that would be received by BlueButton
     * @param respCode The desired HTTP response code
     * @param payload The data that the mock server should return in response to this GET request
     * @param qStringParams The query string parameters that must be present to generate this response
     */
    private static void createMockServerExpectation(String path, int respCode, String payload, List<Parameter> qStringParams){
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

    private static String getRawXML(String path) throws IOException {
        InputStream sampleData = BlueButtonClientTest.class.getClassLoader().getResourceAsStream(path);

        if(sampleData == null) {
            throw new MissingResourceException("Cannot find sample requests", BlueButtonClientTest.class.getName(), path);
        }

        return new String(sampleData.readAllBytes());
    }
}