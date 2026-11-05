package gov.cms.dpc.oneecosystem.client;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.apache.ApacheHttp5RestfulClientFactory;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.cms.dpc.oneecosystem.models.OneEcosystemExportRequest;
import gov.cms.dpc.oneecosystem.models.OneEcosystemExportResponse;
import gov.cms.dpc.oneecosystem.models.OneEcosystemJobStatus;
import io.dropwizard.jackson.Jackson;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpHeaders;
import org.eclipse.jetty.http.HttpStatus;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.hl7.fhir.r4.model.Enumerations;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class OneEcosystemClientImpl implements OneEcosystemClient {

    private static final int REQUEST_TIMEOUT_SECONDS = 60;
    private final URI apiBaseUrl;
    private final String clientId;
    private final String clientSecret;
    private final ObjectMapper objectMapper;
    private final CloseableHttpClient httpClient;
    private final IGenericClient fhirClient;

    private String accessToken;
    private Instant tokenExpiration;

    public OneEcosystemClientImpl(String apiHost, String clientId, String clientSecret) {
        this.apiBaseUrl = URI.create("https://" + apiHost); // need to figure out if I should pass in protocol // just make it work
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.httpClient = HttpClients.createDefault();
        this.objectMapper = Jackson.newObjectMapper();

        FhirContext context = FhirContext.forR4();
        ApacheHttp5RestfulClientFactory factory = new ApacheHttp5RestfulClientFactory(context);
        factory.setHttpClient(httpClient);
        context.setRestfulClientFactory(factory);
        this.fhirClient = context.newRestfulGenericClient(this.apiBaseUrl.toString());

        requestToken();
    }

    /**
     * Copied from BlueButtonClientImpl's requestCapabilityStatement
     *   kept interface a boolean for simplifying what dpc see - "Is it healthy?" is a binary check
     */
    @Override
    public boolean healthy() {
        CapabilityStatement capabilityStatement = fhirClient
            .capabilities()
            .ofType(CapabilityStatement.class)
            .execute();
        return capabilityStatement.getStatus() == Enumerations.PublicationStatus.ACTIVE;
    }

    @Override
    public OneEcosystemExportResponse export(OneEcosystemExportRequest request) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public OneEcosystemJobStatus jobStatus(UUID jobId) {
        throw new UnsupportedOperationException("not implemented");
    }

    /**
     * For testing oauth against BCDA's "dev" env
     * The caller must close the response after reading it to release the connection.
     */
    public ClassicHttpResponse callLegacyEndpoint() {
        URI endpoint = apiBaseUrl.resolve("api/v3/jobs");
        HttpGet request = new HttpGet(endpoint);
        request.setHeader("Authorization", "Bearer " + currentToken());
        request.setHeader("Accept", "application/fhir+json");
        try {
            return httpClient.executeOpen(null, request, null);
        } catch (IOException e) {
            throw new UncheckedIOException("NEWBE legacy request failed", e);
        }
    }

    // Serialize token acquisition so concurrent callers share a single replacement token.
    private synchronized String currentToken() {
        if (accessToken != null && Instant.now().isBefore(tokenExpiration)) {
            return accessToken;
        }

        accessToken = requestToken();
        return accessToken;
    }

    private String requestToken() {
        URI endpoint = apiBaseUrl.resolve("/auth/token");
        HttpPost request = new HttpPost(endpoint);
        request.setConfig(RequestConfig.custom()
            .setResponseTimeout(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build());
        request.setHeader(HttpHeaders.ACCEPT, "application/json");
        request.setHeader(HttpHeaders.AUTHORIZATION, clientCredentialsBasicAuth());

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            if (!HttpStatus.isSuccess(response.getCode())) {
                throw new IllegalStateException("Error requesting token: " + response.getReasonPhrase());
            }
            var token = objectMapper.readTree(response.getEntity().getContent());
            tokenExpiration = Instant.now().plusSeconds(
                token.path("expires_in").asLong() - REQUEST_TIMEOUT_SECONDS
            );
            accessToken = token.path("access_token").asText();
            return accessToken;
        } catch (IOException e) {
            throw new UncheckedIOException("token request failed", e);
        }
    }

    // https://developer.mozilla.org/en-US/docs/Web/HTTP/Guides/Authentication
    // "basic" auth for BCDA to provide creds
    private String clientCredentialsBasicAuth() {
        String credentials = clientId + ":" + clientSecret;
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }
}
