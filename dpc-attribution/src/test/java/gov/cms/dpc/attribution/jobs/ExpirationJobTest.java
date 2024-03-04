package gov.cms.dpc.attribution.jobs;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor;
import gov.cms.dpc.attribution.DPCAttributionConfiguration;
import gov.cms.dpc.attribution.DPCAttributionService;
import gov.cms.dpc.testing.BufferedLoggerHandler;
import gov.cms.dpc.testing.IntegrationTest;
import gov.cms.dpc.testing.JobTestUtils;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.DropwizardTestSupport;
import org.eclipse.jetty.http.HttpStatus;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Group;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import ru.vyarus.dropwizard.guice.module.context.SharedConfigurationState;

import javax.ws.rs.client.Client;

import static gov.cms.dpc.attribution.AttributionTestHelpers.DEFAULT_ORG_ID;
import static gov.cms.dpc.attribution.SharedMethods.createAttributionBundle;
import static gov.cms.dpc.attribution.SharedMethods.submitAttributionBundle;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Integration test for verifying that the expiration jobs runs correctly.
 * We currently don't have a way of verifying that the job runs when expected, since we can't really override Dropwizard's time source.
 * In the future, we might consider using something like ByteBuddy to intercept all system time calls and see if the job still gets run.
 */
// TODO: dropwizard-sundial
@Disabled
@ExtendWith(BufferedLoggerHandler.class)
@IntegrationTest
class ExpirationJobTest {
    private static final String configPath = "src/test/resources/test.application.yml";
    private static final DropwizardTestSupport<DPCAttributionConfiguration> APPLICATION =
            new DropwizardTestSupport<>(DPCAttributionService.class, configPath,
                    ConfigOverride.config("server.applicationConnectors[0].port", "3727"));
    private static final String PROVIDER_ID = "2322222227";
    private static final FhirContext ctx = FhirContext.forDstu3();
    private Client client;

    @BeforeEach
    void initDB() throws Exception {
        JobTestUtils.resetScheduler();
        APPLICATION.before();
        SharedConfigurationState.clear();
        APPLICATION.getApplication().run("db", "migrate", configPath);
        // Seed the database, but use a really early time
        SharedConfigurationState.clear();
        APPLICATION.getApplication().run("seed", "-t 2015-01-01T12:12:12Z", configPath);

        this.client = new JerseyClientBuilder(APPLICATION.getEnvironment()).build("test");
    }

    @AfterEach
    void shutdown() {
        APPLICATION.after();
    }

    @Test
    void test() throws InterruptedException {

        // Manually add a new relationship with a current creation timestamp
        final Bundle updateBundle = createAttributionBundle(PROVIDER_ID, "0L00L00LL00", DEFAULT_ORG_ID);
        // Submit the attribution bundle
        ctx.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);
        final IGenericClient client = ctx.newRestfulGenericClient("http://localhost:" + APPLICATION.getLocalPort() + "/v1/");

        // Disable logging for tests
        LoggingInterceptor loggingInterceptor = new LoggingInterceptor();
        loggingInterceptor.setLogRequestSummary(false);
        loggingInterceptor.setLogResponseSummary(false);
        client.registerInterceptor(loggingInterceptor);

        final Group group = submitAttributionBundle(client, updateBundle);

        int statusCode = JobTestUtils.startJob(APPLICATION, this.client, "ExpireAttributions");
        assertEquals(HttpStatus.OK_200, statusCode, "Job should have started correctly");

        statusCode = JobTestUtils.stopJob(APPLICATION, this.client, "ExpireAttributions");
        assertEquals(HttpStatus.OK_200, statusCode, "Job should have stopped");

        // Wait for a couple of seconds to let the job complete
        Thread.sleep(2000);

        // Check how many are left

        final Group expiredGroup = client
                .read()
                .resource(Group.class)
                .withId(group.getId())
                .encodedJson()
                .execute();

        assertEquals(1, expiredGroup.getMember().size(), "Should only have a single Member");
    }
}
