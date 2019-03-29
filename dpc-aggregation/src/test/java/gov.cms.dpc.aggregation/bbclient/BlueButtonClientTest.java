package gov.cms.dpc.aggregation.bbclient;

import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.hl7.fhir.dstu3.model.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockserver.client.server.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.matchers.Times;
import org.mockserver.model.Header;
import org.mockserver.model.HttpRequest;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.util.Base64;
import java.util.List;
import java.util.MissingResourceException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class BlueButtonClientTest {
    // A random example patient (Jane Doe)
    private static final String TEST_PATIENT_ID = "20140000008325";
    // A patient that only has a single EOB record in bluebutton
    private static final String TEST_SINGLE_EOB_PATIENT_ID = "20140000009893";
    // A patient with very many (592) EOB records in bluebutton
    private static final String TEST_LARGE_EOB_PATIENT_ID = "20140000001827";
    // A patient id that should not exist in bluebutton
    private static final String TEST_NONEXISTENT_PATIENT_ID = "31337";

    private static final String CSV = "testBlueButtonRequests.csv";

    private static BlueButtonClient bbc;

    //@InjectMocks
    //private static BlueButtonClientModule blueButtonClientModule;

    //@Mock
    private static HttpClient mockHttpClient;

    private static ClientAndServer mockServer;



    @BeforeAll
    public static void setupMockHttpClient() throws IOException {

    }

    @BeforeAll
    public static void setupBlueButtonClient() throws IOException {
        //blueButtonClientModule = new BlueButtonClientModule();
        mockServer = ClientAndServer.startClientAndServer(1080); // TODO: verify this starts/stops only once during tests

        final InputStream resource = BlueButtonClientTest.class.getClassLoader().getResourceAsStream(CSV);
        if (resource == null) {
            throw new MissingResourceException("Can not find sample requests", BlueButtonClientTest.class.getName(), CSV);
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource, StandardCharsets.UTF_8))) {
            for (String line; (line = reader.readLine()) != null; ) {
                final String[] splits = line.split(",");
                createMockServerExpectation(splits[0], Integer.parseInt(splits[1]), splits[2]);

            }
        }

        final Injector injector = Guice.createInjector(new TestModule(), new BlueButtonClientModule());
        bbc = injector.getInstance(BlueButtonClient.class);
    }

    @AfterAll
    public static void tearDown() {
        mockServer.stop();
    }

//    @Test
//    void mockitoTest() throws IOException {
//        HttpGet sampleGetRequest = new HttpGet("https://fhir.backend.bluebutton.hhsdevcloud.us/v1/fhir/Patient/20140000008325");
//        HttpResponse resp = mockHttpClient.execute(sampleGetRequest);
//
//        assertEquals(resp.getStatusLine().getStatusCode(),888);
//    }

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
    void shouldHandlePatientsWithVeryManyEOBs() {
        Bundle response = bbc.requestEOBBundleFromServer(TEST_LARGE_EOB_PATIENT_ID);

        assertEquals(response.getTotal(), 592, "This demo patient should have exactly 592 EOBs");
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

    private static void addMockBlueButtonInteraction(HttpClient mockHttpClient, String requestURI, int statusCode, String payload) throws IOException {
        StatusLine statusLine = Mockito.mock(StatusLine.class);
        Mockito.when(statusLine.getStatusCode())
                .thenReturn(statusCode);

        HttpEntity httpEntity = Mockito.mock(HttpEntity.class);
        Mockito.when(httpEntity.getContent())
                .thenReturn(new ByteArrayInputStream(payload.getBytes()));

        HttpResponse httpResponse = Mockito.mock(HttpResponse.class);
        Mockito.when(httpResponse.getStatusLine())
                .thenReturn(statusLine);
        Mockito.when(httpResponse.getEntity())
                .thenReturn(httpEntity);

        Mockito.when(mockHttpClient.execute(Mockito.argThat(new HttpGetMatcher(new HttpGet(requestURI)))))
                .thenReturn(httpResponse);
    }

    private static void createMockServerExpectation(String path, int respCode, String payload){
        new MockServerClient("localhost", 1080)
                .when(
                        HttpRequest.request()
                        .withMethod("GET")
                        .withPath(path),
                        Times.exactly(1)
                )
                .respond(
                        org.mockserver.model.HttpResponse.response()
                        .withStatusCode(respCode)
                        .withHeader(
                            new Header("Content-Type", "application/fhir+xml;charset=UTF-8")
                        )
                        .withBody(Base64.getDecoder().decode(payload))
                        .withDelay(TimeUnit.SECONDS, 1)
                );
    }

}