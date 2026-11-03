package gov.cms.dpc.oneecosystem.client;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.apache.ApacheHttp5RestfulClientFactory;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.jackson.Jackson;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.hl7.fhir.r4.model.Enumerations;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.time.Instant;

public class OneEcosystemClientImpl {

    private final URI apiBaseUrl;
    private final ObjectMapper objectMapper;
    private final CloseableHttpClient httpClient;
    private final IGenericClient fhirClient;

    private String accessToken;
    private Instant tokenExpiration;

    public OneEcosystemClientImpl(URI apiBaseUrl, String clientId, String clientSecret) {
        this.apiBaseUrl = apiBaseUrl;
        this.httpClient = HttpClients.createDefault();
        this.objectMapper = Jackson.newObjectMapper();

        FhirContext context = FhirContext.forR4();
        ApacheHttp5RestfulClientFactory factory = new ApacheHttp5RestfulClientFactory(context);
        factory.setHttpClient(httpClient);
        context.setRestfulClientFactory(factory);
        this.fhirClient = context.newRestfulGenericClient(this.apiBaseUrl.toString());
    }

    /**
     * Copied from BlueButtonClientImpl's requestCapabilityStatement
     *   kept interface a boolean for simplifying what dpc see - "Is it healthy?" is a binary check
     */
    public boolean health() {
        CapabilityStatement capabilityStatement = fhirClient
            .capabilities()
            .ofType(CapabilityStatement.class)
            .execute();
        return capabilityStatement.getStatus() == Enumerations.PublicationStatus.ACTIVE;
    }

    /**
     * For testing oauth against BCDA's "dev" env
     * The caller must close the response after reading it to release the connection.
     */
    public ClassicHttpResponse callLegacyEndpoint() {
        HttpGet request = new HttpGet(apiBaseUrl.resolve("v3/jobs"));
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

        // request a token...
        // reset tokenExpiration
        return accessToken;
    }
}
