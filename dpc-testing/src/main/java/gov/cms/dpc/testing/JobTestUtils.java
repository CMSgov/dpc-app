package gov.cms.dpc.testing;

import io.dropwizard.testing.DropwizardTestSupport;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;

public class JobTestUtils {

    private JobTestUtils() {}

    public static int startJob(DropwizardTestSupport<?> application, Client client, String jobName) {
        Response response = client.target(
                String.format(
                        "http://localhost:%d/%s/tasks/startjob?JOB_NAME=%s",
                        application.getAdminPort(), application.getEnvironment().getAdminContext().getContextPath(), jobName
                ))
                .request()
                .post(Entity.text(""));

        return response.getStatus();
    }

    public static int stopJob(DropwizardTestSupport<?> application, Client client, String jobName) {
        Response response = client.target(
                String.format(
                        "http://localhost:%d/%s/tasks/stopjob?JOB_NAME=%s",
                        application.getAdminPort(), application.getEnvironment().getAdminContext().getContextPath(), jobName
                ))
                .request()
                .post(Entity.text(""));

        return response.getStatus();
    }
}
