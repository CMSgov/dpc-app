package gov.cms.dpc.consent.jobs;

import com.google.inject.*;
import gov.cms.dpc.common.entities.ConsentEntity;
import gov.cms.dpc.common.hibernate.attribution.DPCManagedSessionFactory;
import gov.cms.dpc.consent.DPCConsentConfiguration;
import gov.cms.dpc.consent.DPCConsentService;
import gov.cms.dpc.consent.jdbi.ConsentDAO;
import gov.cms.dpc.testing.JobTestUtils;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.DropwizardTestSupport;
import io.dropwizard.testing.junit.DAOTestRule;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.context.internal.ManagedSessionContext;
import org.junit.Rule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.knowm.sundial.SundialJobScheduler;

import javax.ws.rs.client.Client;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Date;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.Assert.*;

public class SuppressionFileImportTest {

    private static final DropwizardTestSupport<DPCConsentConfiguration> APPLICATION = new DropwizardTestSupport<>(DPCConsentService.class, null, ConfigOverride.config("server.applicationConnectors[0].port", "3727"));
    private Client client;
    private ConsentDAO consentDAO;

    @Rule
    public DAOTestRule database = DAOTestRule.newBuilder().addEntityClass(ConsentEntity.class).build();

    @BeforeEach
    void setUp() throws Exception {
        consentDAO = new ConsentDAO(new DPCManagedSessionFactory(database.getSessionFactory()));

        SuppressionFileImportTest.resetScheduler();

        APPLICATION.before();
        APPLICATION.getApplication().run("db", "migrate");

        this.client = new JerseyClientBuilder(APPLICATION.getEnvironment()).build("test");

        final String PATH_1800_ORIG = "./src/test/resources/synthetic-1800-files/original";
        final String PATH_1800_COPY = "./src/test/resources/synthetic-1800-files/copy";

        SundialJobScheduler.getServletContext().setAttribute("com.google.inject.Injector",
                Guice.createInjector(new AbstractModule(){
                    @Provides
                    protected String provideSuppressionFileDir() {
                        return PATH_1800_COPY;
                    }
                    @Provides
                    protected SessionFactory provideSessionFactory() {
                        return database.getSessionFactory();
                    }
                }
        ));

        try (Stream<Path> paths = Files.walk(Paths.get(PATH_1800_ORIG))) {
            paths.filter(Files::isRegularFile).forEach(p -> {
                Path dest = Paths.get(PATH_1800_COPY, "/", p.getFileName().toString());
                try {
                    Files.copy(p, dest, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    fail(String.format("Cannot copy synthetic 1-800 file %s: %s", p.toString(), e.toString()));
                }
            });
        } catch (IOException e) {
            fail(String.format("Cannot read synthetic 1-800 files: %s", e.toString()));
        }
    }

    @AfterEach
    void shutdown() {
        APPLICATION.after();
    }

    @Test
    void test() throws InterruptedException {
        JobTestUtils.startJob(APPLICATION, this.client, "SuppressionFileImport");
        JobTestUtils.stopJob(APPLICATION, this.client, "SuppressionFileImport");

        // Wait for a couple of seconds to let the job complete
        Thread.sleep(2000);

        SessionFactory sessionFactory = database.getSessionFactory();
        Session session = sessionFactory.openSession();
        ManagedSessionContext.bind(session);

        List<ConsentEntity> consents = database.inTransaction(() -> {
            return consentDAO.list();
        });

        assertEquals(39, consents.size());

        ConsentEntity ce = consents.stream().filter(c -> "1000009420".equals(c.getHicn())).findFirst().get();
        assertEquals(Date.valueOf("2019-10-29"), ce.getEffectiveDate());
        assertEquals("OPTIN", ce.getPolicyCode());


        ManagedSessionContext.unbind(sessionFactory);
        session.close();
    }

    /**
     * This is a hack to get the tests to pass when running in a larger test suite.
     * The {@link SundialJobScheduler} does not allow a scheduler to be restarted once it has been shut down.
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
