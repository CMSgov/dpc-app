package gov.cms.dpc.attribution.jobs;

import ca.uhn.fhir.context.FhirContext;
import gov.cms.dpc.attribution.DPCAttributionConfiguration;
import gov.cms.dpc.attribution.DPCAttributionService;
import gov.cms.dpc.fhir.FHIRMediaTypes;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.DropwizardTestSupport;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.eclipse.jetty.http.HttpStatus;
import org.hl7.fhir.dstu3.model.Bundle;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.knowm.sundial.SundialJobScheduler;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;

import static gov.cms.dpc.attribution.SharedMethods.UnmarshallResponse;
import static gov.cms.dpc.attribution.SharedMethods.createAttributionBundle;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Integration test for verifying that the expiration jobs runs correctly.
 * We currently don't have a way of verifying that the job runs when expected, since we can't really override Dropwizard's time source.
 * In the future, we might consider using something like ByteBuddy to intercept all system time calls and see if the job still gets run.
 */
class ExpirationJobTest {

    private static final DropwizardTestSupport<DPCAttributionConfiguration> APPLICATION = new DropwizardTestSupport<>(DPCAttributionService.class, null, ConfigOverride.config("server.applicationConnectors[0].port", "3727"));
    private static final String PROVIDER_ID = "0c527d2e-2e8a-4808-b11d-0fa06baf8254";
    private static final FhirContext ctx = FhirContext.forDstu3();
    private Client client;

    @BeforeEach
    void initDB() throws Exception {
        ExpirationJobTest.resetScheduler();
        APPLICATION.before();
        APPLICATION.getApplication().run("db", "migrate");
        // Seed the database, but use a really early time
        APPLICATION.getApplication().run("seed", "-t 2015-01-01T12:12:12Z");

        this.client = new JerseyClientBuilder(APPLICATION.getEnvironment()).build("test");
    }

    @AfterEach
    void shutdown() {
        APPLICATION.after();
    }

    @Test
    void test() throws IOException, InterruptedException {

        // Manually add a new relationship with a current creation timestamp
        final String newPatientID = "test-new-patient-id";
        final Bundle updateBundle = createAttributionBundle(PROVIDER_ID, newPatientID);

        try (final CloseableHttpClient client = HttpClients.createDefault()) {

            // Submit the bundle
            final HttpPost submitUpdate = new HttpPost("http://localhost:" + APPLICATION.getLocalPort() + "/v1/Group");
            submitUpdate.setHeader("Accept", FHIRMediaTypes.FHIR_JSON);
            submitUpdate.setEntity(new StringEntity(ctx.newJsonParser().encodeResourceToString(updateBundle)));

            try (CloseableHttpResponse response = client.execute(submitUpdate)) {
                assertEquals(HttpStatus.CREATED_201, response.getStatusLine().getStatusCode(), "Should have succeeded");
            }
        }

        this.startJob(this.client, "ExpireAttributions");

        this.stopJob(this.client, "ExpireAttributions");

        // Wait for a couple of seconds to let the job complete
        Thread.sleep(2000);

        // Check how many are left
        try (final CloseableHttpClient client = HttpClients.createDefault()) {

            final HttpGet httpGet = new HttpGet("http://localhost:" + APPLICATION.getLocalPort() + "/v1/Group/" + PROVIDER_ID);
            httpGet.setHeader("Accept", FHIRMediaTypes.FHIR_JSON);

            try (final CloseableHttpResponse response = client.execute(httpGet)) {
                assertEquals(HttpStatus.OK_200, response.getStatusLine().getStatusCode(), "Should have succeeded");
                List<String> beneficiaries = UnmarshallResponse(response.getEntity());
                assertEquals(1, beneficiaries.size(), "Should have only have a single relationship");
            }
        }
    }

    void startJob(Client client, String jobName) {
        Response response = client.target(
                String.format(
                        "http://localhost:%d/%s/tasks/startjob?JOB_NAME=%s",
                        APPLICATION.getAdminPort(), APPLICATION.getEnvironment().getAdminContext().getContextPath(), jobName
                ))
                .request()
                .post(Entity.text(""));

        assertEquals(HttpStatus.OK_200, response.getStatus(), "Job should have started correctly");
    }

    void stopJob(Client client, String jobName) {
        Response response = client.target(
                String.format(
                        "http://localhost:%d/%s/tasks/stopjob?JOB_NAME=%s",
                        APPLICATION.getAdminPort(), APPLICATION.getEnvironment().getAdminContext().getContextPath(), jobName
                ))
                .request()
                .post(Entity.text(""));

        assertEquals(HttpStatus.OK_200, response.getStatus(), "Job should have stopped");
    }

    /**
     * This is a hack to get the tests to pass when running in a larger test suite.
     * The {@link SundialJobScheduler} does not allow a scheduler to be restarted once it has been shutdown.
     * So the fix is to simply reach into the class, set the scheduler field to be null and try again.
     *
     * @throws IllegalAccessException - Thrown if the field can't be modified
     * @throws NoSuchFieldException   - Thrown if the field is misspelled
     */
    private static void resetScheduler() throws IllegalAccessException, NoSuchFieldException {
        final Field scheduler = SundialJobScheduler.class.getDeclaredField("scheduler");
        scheduler.setAccessible(true);
        final Object oldValue = scheduler.get(SundialJobScheduler.class);
        scheduler.set(oldValue, null);
    }
}
