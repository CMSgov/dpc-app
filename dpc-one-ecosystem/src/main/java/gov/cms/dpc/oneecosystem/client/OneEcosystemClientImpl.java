package gov.cms.dpc.oneecosystem.client;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.apache.ApacheHttp5RestfulClientFactory;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.cms.dpc.fhir.configuration.ConnectionPoolConfiguration;
import gov.cms.dpc.fhir.configuration.TimeoutConfiguration;
import gov.cms.dpc.oneecosystem.config.OneEcosystemClientConfiguration;
import gov.cms.dpc.oneecosystem.models.OneEcosystemExportRequest;
import gov.cms.dpc.oneecosystem.models.OneEcosystemExportResponse;
import gov.cms.dpc.oneecosystem.models.OneEcosystemJobStatus;
import io.dropwizard.jackson.Jackson;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.util.Timeout;
import org.eclipse.jetty.http.HttpStatus;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.hl7.fhir.r4.model.Enumerations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class OneEcosystemClientImpl implements OneEcosystemClient {

    private final URI apiBaseUri;
    private final String clientId;
    private final String clientSecret;
    private final long tokenRefreshBuffer;
    private final ObjectMapper objectMapper;
    private final CloseableHttpClient httpClient;
    private final IGenericClient fhirClient;

    private String accessToken;
    private Instant tokenExpiration;

    private static final Logger logger = LoggerFactory.getLogger(OneEcosystemClientImpl.class);

    public OneEcosystemClientImpl(OneEcosystemClientConfiguration config, FhirContext fhirContext) {
        this.apiBaseUri = config.apiBaseUri();
        logger.info("Attempting to connect to host: {}", apiBaseUri.getHost());

        this.clientId = config.getClientId();
        this.clientSecret = config.getClientSecret();
        this.tokenRefreshBuffer = config.getTokenRefreshBuffer();

        this.httpClient = createHttpClient(config.getTimeouts(), config.getConnectionPoolConfiguration());
        this.objectMapper = Jackson.newObjectMapper();

        ApacheHttp5RestfulClientFactory factory = new ApacheHttp5RestfulClientFactory(fhirContext);
        factory.setHttpClient(httpClient);
        this.fhirClient = factory.newGenericClient(this.apiBaseUri.toString());

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
     *   Should return a non-403 status code to indicate request has been authenticated
     */
    public int callLegacyEndpoint() {
        URI endpoint = apiBaseUri.resolve("api/v3/jobs");
        HttpGet request = new HttpGet(endpoint);
        request.setHeader("Authorization", "Bearer " + currentToken());
        request.setHeader("Accept", "application/fhir+json");
        try {
            return httpClient.execute(request, HttpResponse::getCode);
        } catch (IOException e) {
            throw new UncheckedIOException("NEWBE legacy request failed", e);
        }
    }

    private static CloseableHttpClient createHttpClient(TimeoutConfiguration timeouts, ConnectionPoolConfiguration pool) {
        var connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setDefaultConnectionConfig(ConnectionConfig.custom()
                        .setConnectTimeout(Timeout.ofMilliseconds(timeouts.getConnectionTimeout()))
                        .build())
                .setMaxConnTotal(pool.getPoolMaxTotal())
                .setMaxConnPerRoute(pool.getPoolMaxPerRoute())
                .build();
        RequestConfig defaultConfig = RequestConfig.custom()
                .setResponseTimeout(Timeout.ofMilliseconds(timeouts.getSocketTimeout()))
                .setConnectionRequestTimeout(Timeout.ofMilliseconds(timeouts.getRequestTimeout()))
                .build();
        return HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(defaultConfig)
                .build();
    }

    private synchronized String currentToken() {
        if (accessToken != null && Instant.now().isBefore(tokenExpiration)) {
            return accessToken;
        }

        return requestToken();
    }

    private String requestToken() {
        URI endpoint = apiBaseUri.resolve("/auth/token");
        HttpPost request = new HttpPost(endpoint);
        request.setHeader(HttpHeaders.ACCEPT, "application/json");
        request.setHeader(HttpHeaders.AUTHORIZATION, clientCredentialsBasicAuth());

        try {
            return httpClient.execute(request, response -> {
                if (!HttpStatus.isSuccess(response.getCode())) {
                    throw new IllegalStateException("Error requesting token: " + response.getReasonPhrase());
                }
                var token = objectMapper.readTree(response.getEntity().getContent());

                long tokenLifetimeMs = TimeUnit.SECONDS.toMillis(token.path("expires_in").asLong());
                long safeTokenLifetimeMs = Math.max(tokenLifetimeMs - tokenRefreshBuffer, 0L);
                tokenExpiration = Instant.now().plusMillis(safeTokenLifetimeMs);
                accessToken = token.path("access_token").asText();
                return accessToken;
            });
        } catch (IOException e) {
            throw new UncheckedIOException("token request failed", e);
        }
    }

    /**
     * Get "basic" auth header to pass in credentials for token request
     * <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Guides/Authentication">...</a>
     */
    private String clientCredentialsBasicAuth() {
        String credentials = clientId + ":" + clientSecret;
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }
}
