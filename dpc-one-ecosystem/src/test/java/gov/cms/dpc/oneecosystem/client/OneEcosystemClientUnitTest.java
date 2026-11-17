package gov.cms.dpc.oneecosystem.client;

import ca.uhn.fhir.context.FhirContext;
import gov.cms.dpc.oneecosystem.config.OneEcosystemClientConfiguration;
import io.dropwizard.configuration.ConfigurationException;
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

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

class OneEcosystemClientUnitTest {

    private static final int TOKEN_LIFETIME_SECONDS = 3600;
    private static final FhirContext FHIR_CONTEXT = FhirContext.forR4();
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
        when(httpClient.execute(
                argThat(request -> request instanceof HttpPost post && "/auth/token".equals(post.getPath())),
                any(HttpClientResponseHandler.class)))
                .thenAnswer(tokenResponse("access-token", TOKEN_LIFETIME_SECONDS));

        OneEcosystemClientImpl client = newClient();
        assertEquals(200, client.callLegacyEndpoint());

        ArgumentCaptor<HttpGet> apiRequest = ArgumentCaptor.forClass(HttpGet.class);
        verify(httpClient).execute(apiRequest.capture(), any(HttpClientResponseHandler.class));
        assertEquals("Bearer access-token", apiRequest.getValue().getFirstHeader(HttpHeaders.AUTHORIZATION).getValue());
    }

    @Test
    void shouldReuseUnexpiredToken() throws IOException {
        when(httpClient.execute(
                argThat(request -> request instanceof HttpPost post && "/auth/token".equals(post.getPath())),
                any(HttpClientResponseHandler.class)))
                .thenAnswer(tokenResponse("access-token", TOKEN_LIFETIME_SECONDS));

        OneEcosystemClientImpl client = newClient();
        assertEquals(200, client.callLegacyEndpoint());
        assertEquals(200, client.callLegacyEndpoint());

        verify(httpClient).execute(
                argThat(request -> request instanceof HttpPost post && "/auth/token".equals(post.getPath())),
                any(HttpClientResponseHandler.class));
        verify(httpClient, times(2)).execute(any(HttpGet.class), any(HttpClientResponseHandler.class));
    }

    @Test
    void shouldRefreshTokenIfAboutToExpire() throws IOException {
        when(httpClient.execute(
                argThat(request -> request instanceof HttpPost post && "/auth/token".equals(post.getPath())),
                any(HttpClientResponseHandler.class)))
                .thenAnswer(tokenResponse("old-token", 5))
                .thenAnswer(tokenResponse("new-token", TOKEN_LIFETIME_SECONDS));

        OneEcosystemClientImpl client = newClient();
        assertEquals(200, client.callLegacyEndpoint());

        verify(httpClient, times(2)).execute(
                argThat(request -> request instanceof HttpPost post && "/auth/token".equals(post.getPath())),
                any(HttpClientResponseHandler.class));
        ArgumentCaptor<HttpGet> apiRequest = ArgumentCaptor.forClass(HttpGet.class);
        verify(httpClient).execute(apiRequest.capture(), any(HttpClientResponseHandler.class));
        String authorization = apiRequest.getValue().getFirstHeader(HttpHeaders.AUTHORIZATION).getValue();
        assertEquals("Bearer new-token", authorization);
    }

    private OneEcosystemClientImpl newClient() {
        return new OneEcosystemClientImpl(config, FHIR_CONTEXT);
    }

    private Answer<String> tokenResponse(String token, int expiresIn) {
        return response(200, "{\"access_token\":\"%s\",\"expires_in\":%d}".formatted(token, expiresIn));
    }

    private <T> Answer<T> response(int status, String body) {
        return invocation -> {
            try (var response = new BasicClassicHttpResponse(status)) {
                response.setEntity(new StringEntity(body, ContentType.APPLICATION_JSON));
                return invocation.<HttpClientResponseHandler<T>>getArgument(1).handleResponse(response);
            }
        };
    }
}
