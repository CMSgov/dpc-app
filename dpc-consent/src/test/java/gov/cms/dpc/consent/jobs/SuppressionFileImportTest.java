package gov.cms.dpc.consent.jobs;

import com.google.inject.*;
import gov.cms.dpc.common.hibernate.consent.DPCConsentManagedSessionFactory;
import gov.cms.dpc.common.consent.entities.ConsentEntity;
import gov.cms.dpc.consent.DPCConsentConfiguration;
import gov.cms.dpc.consent.DPCConsentService;
import gov.cms.dpc.consent.jdbi.ConsentDAO;
import gov.cms.dpc.testing.IntegrationTest;
import gov.cms.dpc.testing.JobTestUtils;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.DropwizardTestSupport;
import io.dropwizard.testing.junit5.DAOTestExtension;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;


@IntegrationTest
public class SuppressionFileImportTest {

    private static final DropwizardTestSupport<DPCConsentConfiguration> APPLICATION = new DropwizardTestSupport<>(DPCConsentService.class, "ci.application.conf",
            ConfigOverride.config("server.applicationConnectors[0].port", "3727")
    );
    private Client client;
    private ConsentDAO consentDAO;
    final Path PATH_1800_COPY = Paths.get("./src/test/resources/synthetic-1800-files/copy");

    @Rule
    public DAOTestExtension database = DAOTestExtension.newBuilder().addEntityClass(ConsentEntity.class).setProperty("webAllowOthers", "false").build();

    @BeforeEach
    void setUp() throws Exception {
        consentDAO = new ConsentDAO(new DPCConsentManagedSessionFactory(database.getSessionFactory()));

        JobTestUtils.resetScheduler();

        APPLICATION.before();

        this.client = new JerseyClientBuilder(APPLICATION.getEnvironment()).build("test");

        SundialJobScheduler.getServletContext().setAttribute("com.google.inject.Injector",
                Guice.createInjector(new AbstractModule(){
                    @Provides
                    protected String provideSuppressionFileDir() {
                        return PATH_1800_COPY.toString();
                    }
                    @Provides
                    protected SessionFactory provideSessionFactory() {
                        return database.getSessionFactory();
                    }
                }
        ));
    }

    @AfterEach
    void shutdown() throws IOException {
        Files.delete(PATH_1800_COPY);
        APPLICATION.after();
    }

    @Test
    void test() throws InterruptedException, IOException {
        copyFiles("./src/test/resources/synthetic-1800-files/valid");

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
        assertEquals(LocalDate.parse("2019-10-29"), ce.getEffectiveDate());
        assertEquals("OPTIN", ce.getPolicyCode());


        ManagedSessionContext.unbind(sessionFactory);
        session.close();
    }

    @Test
    void test_invalidFiles() throws InterruptedException, IOException {
        copyFiles("./src/test/resources/synthetic-1800-files/invalid");

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

        List<String> hicns = consents.stream().map(c -> c.getHicn()).collect(Collectors.toList());

        // From file with invalid name
        assertFalse(hicns.contains("1000001119"));

        // Before invalid record
        assertTrue(hicns.contains("1000001112"));
        // Invalid record
        assertFalse(hicns.contains("1000001113"));
        // After invalid record
        assertTrue(hicns.contains("1000001114"));

        ManagedSessionContext.unbind(sessionFactory);
        session.close();
    }

    void copyFiles(String path) throws IOException {
        Path copyPath = Files.createDirectory(PATH_1800_COPY);
        Stream<Path> paths = Files.walk(Paths.get(path));
        paths.filter(Files::isRegularFile).forEach(f -> {
            Path dest = Paths.get(PATH_1800_COPY.toString(), "/", f.getFileName().toString());
            try {
                Files.copy(f, dest, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                fail(String.format("Cannot copy synthetic 1-800 file %s: %s", f.toString(), e.toString()));
            }
        });
    }
}
