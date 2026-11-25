package gov.cms.dpc.oneecosystem.client;

import ca.uhn.fhir.context.FhirContext;
import gov.cms.dpc.oneecosystem.config.OneEcosystemClientConfiguration;
import gov.cms.dpc.oneecosystem.models.OneEcosystemExportRequest;
import gov.cms.dpc.oneecosystem.models.OneEcosystemExportResponse;
import io.dropwizard.configuration.ConfigurationException;
import io.dropwizard.jackson.Jackson;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.message.BasicClassicHttpResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.stubbing.Answer;
import org.mockito.stubbing.OngoingStubbing;

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

class OneEcosystemClientUnitTest {

    private static final int TOKEN_LIFETIME_SECONDS = 3600;
    private static final FhirContext FHIR_CONTEXT = FhirContext.forR4();
    private static final OneEcosystemExportRequest EXPORT_REQUEST = new OneEcosystemExportRequest(
            List.of("123", "234", "345"), Instant.EPOCH,
            List.of("Patient", "ExplanationOfBenefit", "Coverage"),
            "https://example.test/api/v3/Group/dummyGroupId/$export");
    private static OneEcosystemClientConfiguration config;

    private CloseableHttpClient httpClient;
    private MockedStatic<HttpClients> httpClients;

    @BeforeAll
    static void loadConfig() throws IOException, ConfigurationException {
        config = OneEcosystemTestHelpers.loadConfig("test.application.yml");
    }

    @BeforeEach
    void setup() throws IOException {
        httpClient = mock(CloseableHttpClient.class);
        HttpClientBuilder builder = mock(HttpClientBuilder.class, RETURNS_SELF);
        when(builder.build()).thenReturn(httpClient);
        httpClients = mockStatic(HttpClients.class);
        httpClients.when(HttpClients::custom).thenReturn(builder);
        when(httpClient.execute(any(HttpGet.class), any(HttpClientResponseHandler.class)))
                .thenAnswer(response(200, "{}"));
    }

    @AfterEach
    void tearDown() {
        httpClients.close();
    }

    @Test
    void shouldCallLegacyEndpointWithAccessToken() throws IOException {
        mockTokenRequest("access-token", TOKEN_LIFETIME_SECONDS);
        OneEcosystemClientImpl client = newOEClient();

        assertEquals(200, client.callLegacyEndpoint());

        ArgumentCaptor<HttpGet> apiRequest = ArgumentCaptor.forClass(HttpGet.class);
        verify(httpClient).execute(apiRequest.capture(), any(HttpClientResponseHandler.class));
        assertEquals("Bearer access-token", apiRequest.getValue().getFirstHeader(HttpHeaders.AUTHORIZATION).getValue());
    }

    @Test
    void shouldReuseUnexpiredToken() throws IOException {
        mockTokenRequest("access-token", TOKEN_LIFETIME_SECONDS);
        OneEcosystemClientImpl client = newOEClient();

        assertEquals(200, client.callLegacyEndpoint());
        assertEquals(200, client.callLegacyEndpoint());

        verify(httpClient).execute(
                argThat(request -> request instanceof HttpPost post && "/auth/token".equals(post.getPath())),
                any(HttpClientResponseHandler.class));
        verify(httpClient, times(2)).execute(any(HttpGet.class), any(HttpClientResponseHandler.class));
    }

    @Test
    void shouldRefreshTokenIfAboutToExpire() throws IOException {
        mockTokenRequest("old-token", 5)
                .thenAnswer(tokenResponse("new-token", TOKEN_LIFETIME_SECONDS));
        OneEcosystemClientImpl client = newOEClient();

        assertEquals(200, client.callLegacyEndpoint());
        verify(httpClient, times(2)).execute(
                argThat(request -> request instanceof HttpPost post && "/auth/token".equals(post.getPath())),
                any(HttpClientResponseHandler.class));
        ArgumentCaptor<HttpGet> apiRequest = ArgumentCaptor.forClass(HttpGet.class);
        verify(httpClient).execute(apiRequest.capture(), any(HttpClientResponseHandler.class));
        String authorization = apiRequest.getValue().getFirstHeader(HttpHeaders.AUTHORIZATION).getValue();
        assertEquals("Bearer new-token", authorization);
    }

    @Test
    void shouldSendRequiredParamsForExport() throws IOException, URISyntaxException {
        mockTokenRequest("access-token", TOKEN_LIFETIME_SECONDS);
        var client = newOEClient();
        ArgumentCaptor<HttpPost> apiRequest = ArgumentCaptor.forClass(HttpPost.class);
        when(httpClient.execute(apiRequest.capture(), any(HttpClientResponseHandler.class)))
                .thenAnswer(response(202, null));

        client.export(EXPORT_REQUEST);

        HttpPost post = apiRequest.getValue();
        assertEquals(config.apiBaseUri().resolve("api/v3/$shared-export"), post.getUri());
        assertEquals("Bearer access-token", post.getFirstHeader(HttpHeaders.AUTHORIZATION).getValue());

        var mapper = Jackson.newObjectMapper();
        var body = mapper.readTree(post.getEntity().getContent());
        assertEquals(EXPORT_REQUEST.sourceQuery(), body.path("source_query").asText());

        var query = body.path("queries").get(0);
        assertEquals(mapper.valueToTree(EXPORT_REQUEST.mbis()), query.path("mbis"));
        assertEquals(mapper.valueToTree(EXPORT_REQUEST.resourceTypes()), query.path("resource_types"));

        var filterNames = new ArrayList<String>();
        query.path("filters").forEach(filter -> filterNames.add(filter.path("name").asText()));
        assertTrue(filterNames.containsAll(List.of("_since", "_typeFilter")));
    }

    @Test
    void shouldIncludeTypeFilterInPayloadForEob() throws IOException {
        mockTokenRequest("access-token", TOKEN_LIFETIME_SECONDS);
        var client = newOEClient();
        ArgumentCaptor<HttpPost> apiRequest = ArgumentCaptor.forClass(HttpPost.class);
        when(httpClient.execute(apiRequest.capture(), any(HttpClientResponseHandler.class)))
                .thenAnswer(response(202, null));

        var eobRequest = new OneEcosystemExportRequest(EXPORT_REQUEST.mbis(), EXPORT_REQUEST.since(),
                List.of("ExplanationOfBenefit"), EXPORT_REQUEST.sourceQuery());
        client.export(eobRequest);

        var body = Jackson.newObjectMapper().readTree(apiRequest.getValue().getEntity().getContent());
        assertTrue(body.path("queries").get(0).path("filters").findValuesAsText("name").contains("_typeFilter"));
    }

    @Test
    void shouldReturnAcceptedExportResponse() throws IOException {
        mockTokenRequest("access-token", TOKEN_LIFETIME_SECONDS);
        var client = newOEClient();
        String contentLocation = "https://example.test/api/v3/jobs/1234asdf";
        when(httpClient.execute(any(HttpPost.class), any(HttpClientResponseHandler.class)))
                .thenAnswer(response(202, null, contentLocation));

        var result = client.export(EXPORT_REQUEST);

        assertEquals(new OneEcosystemExportResponse(202, contentLocation), result);
    }

    @Test
    void shouldReadCompletedJobOutputUrls() throws IOException {
        String jobId = "theJobId";
        mockTokenRequest("access-token", TOKEN_LIFETIME_SECONDS);

        when(httpClient.execute(any(HttpGet.class), any(HttpClientResponseHandler.class)))
                .thenAnswer(response(200, """
                    {
                      "transactionTime": "2026-08-01T13:00:00Z",
                      "request": "https://dpc.cms.gov/api/v3/Group/test-group/$export",
                      "requiresAccessToken": true,
                      "output": [
                        {"type": "Patient", "url": "https://example.test/data/theJobId/patient.ndjson"},
                        {"type": "Coverage", "url": "https://example.test/data/theJobId/coverage.ndjson"},
                        {"type": "ExplanationOfBenefit", "url": "https://example.test/data/theJobId/eob.ndjson"}
                      ],
                      "error": [],
                      "JobID": "theJobId"
                    }
                    """));

        var status = newOEClient().jobStatus(jobId);

        assertEquals(200, status.statusCode());
        assertEquals(jobId, status.body().JobID());
        assertEquals(List.of("Patient", "Coverage", "ExplanationOfBenefit"), status.body().output().stream()
                .map(output -> output.type()).toList());
        assertEquals(List.of(
                "https://example.test/data/theJobId/patient.ndjson",
                "https://example.test/data/theJobId/coverage.ndjson",
                "https://example.test/data/theJobId/eob.ndjson"), status.body().output().stream()
                .map(output -> output.url()).toList());
    }

    @Test
    void shouldForward202CodeForPendingJob() throws IOException {
        mockTokenRequest("access-token", TOKEN_LIFETIME_SECONDS);
        when(httpClient.execute(any(HttpGet.class), any(HttpClientResponseHandler.class)))
                .thenAnswer(response(202, "Doesn't matter (e.g. Job is still processing)"));

        var status = newOEClient().jobStatus(UUID.randomUUID().toString());

        assertEquals(202, status.statusCode());
        assertNull(status.body());
    }

    private OneEcosystemClientImpl newOEClient() {
        return new OneEcosystemClientImpl(config, FHIR_CONTEXT);
    }

    private OngoingStubbing<String> mockTokenRequest(String token, int expiresIn) throws IOException {
        return when(httpClient.<String>execute(
                argThat(request -> request instanceof HttpPost post && "/auth/token".equals(post.getPath())),
                any(HttpClientResponseHandler.class)))
                .thenAnswer(tokenResponse(token, expiresIn));
    }

    private Answer<String> tokenResponse(String token, int expiresIn) {
        return response(200, "{\"access_token\":\"%s\",\"expires_in\":%d}".formatted(token, expiresIn));
    }

    private <T> Answer<T> response(int status, String body) {
        return response(status, body, null);
    }

    private <T> Answer<T> response(int status, String body, String contentLocation) {
        return invocation -> {
            try (var response = new BasicClassicHttpResponse(status)) {
                if (contentLocation != null) {
                    response.setHeader(HttpHeaders.CONTENT_LOCATION, contentLocation);
                }
                if (body != null) {
                    response.setEntity(new StringEntity(body, ContentType.APPLICATION_JSON));
                }
                return invocation.<HttpClientResponseHandler<T>>getArgument(1).handleResponse(response);
            }
        };
    }
}
