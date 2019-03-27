package gov.cms.dpc.attribution.jobs;

import gov.cms.dpc.attribution.DPCAttributionConfiguration;
import gov.cms.dpc.attribution.DPCAttributionService;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.DropwizardTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ExpirationJobTest {

    private static final DropwizardTestSupport<DPCAttributionConfiguration> APPLICATION = new DropwizardTestSupport<>(DPCAttributionService.class, null, ConfigOverride.config("server.applicationConnectors[0].port", "3727"));
    private Client client;

    @BeforeAll
    public static void setup() {

    }

    @BeforeEach
    public void initDB() throws Exception {
        APPLICATION.before();
        APPLICATION.getApplication().run("db", "migrate");
        APPLICATION.getApplication().run("seed");

        this.client = new JerseyClientBuilder(APPLICATION.getEnvironment()).build("test");
    }

    @AfterEach
    public void shutdown() {
        APPLICATION.after();
    }

    @Test
    public void test() {
        this.startJob(this.client, "ExpireAttributions");
    }


    public void startJob(Client client, String jobName) {
        Response response = client.target(
                String.format(
                        "http://localhost:%d/%s/tasks/startjob?JOB_NAME=%s",
                        APPLICATION.getAdminPort(), APPLICATION.getEnvironment().getAdminContext().getContextPath(), jobName
                ))
                .request()
                .post(Entity.text(""));

        assertEquals(200, response.getStatus(), "Job should have started correctly");
    }

    public void stopJob(Client client, String jobName) {
        Response response = client.target(
                String.format(
                        "http://localhost:%d/%s/tasks/stopjob?JOB_NAME=%s",
                        APPLICATION.getAdminPort(), APPLICATION.getEnvironment().getAdminContext().getContextPath(), jobName
                ))
                .request()
                .post(Entity.text(""));

        assertEquals(200, response.getStatus(), "Job should have stopped");
    }
}
