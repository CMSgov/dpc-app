package gov.cms.dpc.oneecosystem.client;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.apache.ApacheHttp5RestfulClientFactory;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import gov.cms.dpc.fhir.configuration.ConnectionPoolConfiguration;
import gov.cms.dpc.fhir.configuration.TimeoutConfiguration;
import gov.cms.dpc.oneecosystem.config.OneEcosystemClientConfiguration;
import gov.cms.dpc.oneecosystem.models.OneEcosystemExportRequest;
import gov.cms.dpc.oneecosystem.models.OneEcosystemExportResponse;
import gov.cms.dpc.oneecosystem.models.OneEcosystemJobStatus;
import gov.cms.dpc.oneecosystem.models.OneEcosystemJobStatusResponse;
import io.dropwizard.jackson.Jackson;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.StringEntity;
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

    private static final String DEFAULT_TYPE_FILTER_JSON = """
            {
              "name": "_typeFilter",
              "value": {
                "resource_type": "ExplanationOfBenefit",
                "query_parameters": [
                  {"name": "_tag", "value": "https://bluebutton.cms.gov/fhir/CodeSystem/System-Type|NationalClaimsHistory,https://bluebutton.cms.gov/fhir/CodeSystem/System-Type|DDPS"}
                ]
              }
            }
            """;

    private final URI apiBaseUri;
    private final String clientId;
    private final String clientSecret;
    private final long tokenRefreshBuffer;
    private final ObjectMapper objectMapper;
    private final CloseableHttpClient httpClient;
    private final IGenericClient fhirClient;

    private volatile String accessToken;
    private volatile Instant tokenExpiration;

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
        URI endpoint = apiBaseUri.resolve("api/v3/$shared-export");
        HttpPost post = new HttpPost(endpoint);
        post.setHeader("Prefer", "respond-async");

        try {
            post.setEntity(new StringEntity(buildExportBody(request), ContentType.APPLICATION_JSON));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to serialize export request", e);
        }

        try {
            return executeAuthenticatedRequest(post, response -> {
                int statusCode = response.getCode();
                var locationHeader = response.getFirstHeader(HttpHeaders.CONTENT_LOCATION);
                String contentLocation = locationHeader != null ? locationHeader.getValue() : null;
                return new OneEcosystemExportResponse(statusCode, contentLocation);
            });
        } catch (IOException e) {
            throw new UncheckedIOException("Export request failed", e);
        }
    }

    @Override
    public OneEcosystemJobStatus jobStatus(String jobId) {
        URI endpoint = apiBaseUri.resolve("api/v3/jobs/" + jobId);
        HttpGet request = new HttpGet(endpoint);

        try {
            return executeAuthenticatedRequest(request, response -> {
                int statusCode = response.getCode();

                OneEcosystemJobStatusResponse body = null;
                if (statusCode == HttpStatus.OK_200) {
                    body = objectMapper.readValue(response.getEntity().getContent(), OneEcosystemJobStatusResponse.class);
                }

                return new OneEcosystemJobStatus(statusCode, body);
            });
        } catch (IOException e) {
            throw new UncheckedIOException("Job status request failed", e);
        }
    }

    /**
     * For testing oauth against BCDA's "dev" env
     *   Should return a non-403 status code to indicate request has been authenticated
     */
    public int callLegacyEndpoint() {
        URI endpoint = apiBaseUri.resolve("api/v3/jobs");
        HttpGet request = new HttpGet(endpoint);
        try {
            return executeAuthenticatedRequest(request, HttpResponse::getCode);
        } catch (IOException e) {
            throw new UncheckedIOException("NEWBE legacy request failed", e);
        }
    }

    private <T> T executeAuthenticatedRequest(
            ClassicHttpRequest request, HttpClientResponseHandler<T> handler) throws IOException {
        request.setHeader(HttpHeaders.ACCEPT, "application/fhir+json");
        request.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + currentToken());
        return httpClient.execute(request, handler);
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

    private String currentToken() {
        Instant expiration = tokenExpiration;
        String token = accessToken;
        if (token !=null && expiration != null && Instant.now().isBefore(expiration)) {
            return token; // fast path, no lock
        }
        synchronized (this) {
            // re-check: another thread may have refreshed while we waited for the lock
            if (accessToken != null && Instant.now().isBefore(tokenExpiration)) {
                return accessToken;
            }
            return requestToken();
        }
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
                accessToken = token.path("access_token").asText();
                tokenExpiration = Instant.now().plusMillis(safeTokenLifetimeMs);
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

    private String buildExportBody(OneEcosystemExportRequest request) throws IOException {
        ObjectNode root = objectMapper.createObjectNode();
        root.set("source_query", TextNode.valueOf(request.sourceQuery()));
        root.set("system", TextNode.valueOf("DPC"));
        root.set("tracking_id", TextNode.valueOf(UUID.randomUUID().toString()));
        root.set("version", TextNode.valueOf("v3"));

        ObjectNode query = root.putArray("queries").addObject();
        request.mbis().forEach(query.putArray("mbis")::add);
        request.resourceTypes().forEach(query.putArray("resource_types")::add);

        ArrayNode filters = query.putArray("filters");

        ObjectNode sinceFilter = filters.addObject();
        sinceFilter.set("name", TextNode.valueOf("_since"));
        sinceFilter.set("value", TextNode.valueOf(request.since().toString()));

        filters.add(objectMapper.readTree(DEFAULT_TYPE_FILTER_JSON));

        return objectMapper.writeValueAsString(root);
    }
}
