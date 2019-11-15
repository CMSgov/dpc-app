package gov.cms.dpc.testing;

import io.dropwizard.testing.DropwizardTestSupport;
import org.knowm.sundial.SundialJobScheduler;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.lang.reflect.Field;

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



    /**
     * This is a hack to get the tests to pass when running in a larger test suite.
     * The {@link SundialJobScheduler} does not allow a scheduler to be restarted once it has been shutdown.
     * So the fix is to simply reach into the class, set the scheduler field to be null and try again.
     *
     * @throws IllegalAccessException - Thrown if the field can't be modified
     * @throws NoSuchFieldException   - Thrown if the field is misspelled
     */
    public static void resetScheduler() throws IllegalAccessException, NoSuchFieldException {
        final Field scheduler = SundialJobScheduler.class.getDeclaredField("scheduler");
        scheduler.setAccessible(true);
        final Object oldValue = scheduler.get(SundialJobScheduler.class);
        scheduler.set(oldValue, null);
    }
}
