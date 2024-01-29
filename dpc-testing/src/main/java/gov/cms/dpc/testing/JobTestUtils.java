package gov.cms.dpc.testing;

import io.dropwizard.testing.DropwizardTestSupport;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;

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
