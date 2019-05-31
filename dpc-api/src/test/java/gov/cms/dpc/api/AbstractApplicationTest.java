package gov.cms.dpc.api;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import gov.cms.dpc.api.annotations.IntegrationTest;
import gov.cms.dpc.api.client.ClientUtils;
import gov.cms.dpc.api.models.JobCompletionModel;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.DropwizardTestSupport;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.eclipse.jetty.http.HttpStatus;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@IntegrationTest
public class AbstractApplicationTest {

    protected static final DropwizardTestSupport<DPCAPIConfiguration> APPLICATION = new DropwizardTestSupport<>(DPCAPIService.class, null, ConfigOverride.config("server.applicationConnectors[0].port", "7777"));
    public static final String ATTRIBUTION_TRUNCATE_TASK = "http://localhost:9902/tasks/truncate";
    protected FhirContext ctx;

    protected AbstractApplicationTest() {
    }

    protected String getBaseURL() {
        return String.format("http://localhost:%d/v1/", APPLICATION.getLocalPort());
    }

    @BeforeAll
    public static void setup() throws IOException {
        truncateDatabase();
        APPLICATION.before();
    }

    @BeforeEach
    public void eachSetup() throws IOException {
        ctx = FhirContext.forDstu3();

        // Check health
        checkHealth();
    }

    @AfterEach
    public void eachShutdown() throws IOException {
        checkHealth();
    }

    @AfterAll
    public static void shutdown() {
        APPLICATION.after();
    }

    private static void truncateDatabase() throws IOException {

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            final HttpPost post = new HttpPost(ATTRIBUTION_TRUNCATE_TASK);

            try (CloseableHttpResponse execute = client.execute(post)) {
                assertEquals(HttpStatus.OK_200, execute.getStatusLine().getStatusCode(), "Should have truncated database");
            }
        }
    }

    private void checkHealth() throws IOException {
        // URI of the API Service Healthcheck
        final String healthURI = String.format("http://localhost:%s/healthcheck", APPLICATION.getAdminPort());
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            final HttpGet healthCheck = new HttpGet(healthURI);

            try (CloseableHttpResponse execute = client.execute(healthCheck)) {
                assertEquals(HttpStatus.OK_200, execute.getStatusLine().getStatusCode(), "Should be healthy");
            }
        }
    }

    protected <T extends IBaseResource> void validateResourceFile(Class<T> clazz, JobCompletionModel response, ResourceType resourceType, int expectedSize) throws IOException {
        final String fileID = response
                .getOutput()
                .stream()
                .filter(output -> output.getType() == resourceType)
                .map(JobCompletionModel.OutputEntry::getUrl)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Should have at least 1 patient resource"));

        final File tempFile = ClientUtils.fetchExportedFiles(fileID);

        // Read the file back in and parse the patients
        final IParser parser = ctx.newJsonParser();

        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(tempFile, StandardCharsets.UTF_8))) {
            final List<T> entries = bufferedReader.lines()
                    .map((line) -> clazz.cast(parser.parseResource(line)))
                    .collect(Collectors.toList());

            assertEquals(expectedSize, entries.size(), String.format("Should have %d entries in the resource", expectedSize));
        }
    }
}


