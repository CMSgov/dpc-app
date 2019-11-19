package gov.cms.dpc.consent.jobs;

import com.google.inject.Inject;
import com.google.inject.Injector;
import gov.cms.dpc.common.hibernate.consent.DPCConsentManagedSessionFactory;
import gov.cms.dpc.common.consent.entities.ConsentEntity;
import gov.cms.dpc.consent.exceptions.InvalidSuppressionRecordException;
import gov.cms.dpc.consent.jdbi.ConsentDAO;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.context.internal.ManagedSessionContext;
import org.knowm.sundial.Job;
import org.knowm.sundial.SundialJobScheduler;
import org.knowm.sundial.exceptions.JobInterruptException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.stream.Stream;

public class SuppressionFileImport extends Job {

    private static final Logger logger = LoggerFactory.getLogger(SuppressionFileImport.class);

    @Inject
    private DPCConsentManagedSessionFactory managedSessionFactory;

    private ConsentDAO consentDAO;

    @Inject
    public String suppressionFileDir;

    @Inject
    public SuppressionFileImport() {
        // Manually load the Guice injector. Since the job loads at the beginning of the startup process, Guice is not automatically injected.
        final Injector injector = (Injector) SundialJobScheduler.getServletContext().getAttribute("com.google.inject.Injector");
        injector.injectMembers(this);
        this.consentDAO = new ConsentDAO(managedSessionFactory);
    }

    @Override
    public void doRun() throws JobInterruptException {
        if (StringUtils.isBlank(suppressionFileDir)) {
            logger.error("No suppression file directory set for import.");
            return;
        }

        try (Stream<Path> paths = Files.walk(Paths.get(suppressionFileDir))) {
            paths.filter(Files::isRegularFile).forEach(p -> {
                if (Files.isReadable(p) && SuppressionFileUtils.is1800File(p)) {
                    importFile(p);
                }
                try {
                    Files.delete(p);
                } catch (IOException e) {
                    logger.error("Cannot delete file {} from suppression directory", p.getFileName().toString(), e);
                }
            });
        } catch (IOException e) {
            logger.error("Cannot read files in suppression directory", e);
        }
    }

    private void importFile(Path path) {
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            importRecords(reader, path.getFileName().toString());
        } catch (IOException e) {
            logger.error("Cannot import suppression file", e);
            return;
        }
    }

    private void importRecords(BufferedReader reader, String filename) {
        SessionFactory sessionFactory = managedSessionFactory.getSessionFactory();
        Session session = sessionFactory.openSession();

        ManagedSessionContext.bind(session);
        Transaction transaction = session.beginTransaction();

        try {
            buildAndSaveConsentRecords(reader, filename);
            transaction.commit();
        } catch (Exception e) {
            logger.error("Cannot commit suppression file import transaction", e);
            transaction.rollback();
        } finally {
            session.close();
            ManagedSessionContext.unbind(sessionFactory);
        }
    }

    private void buildAndSaveConsentRecords(BufferedReader reader, String filename) {
        LineIterator lineIter = IOUtils.lineIterator(reader);
        int lineNum = 0;

        while (lineIter.hasNext()) {
            lineNum++;
            try {
                Optional<ConsentEntity> consent = SuppressionFileUtils.entityFromLine(lineIter.nextLine(), filename, lineNum);
                if (consent.isPresent()) {
                    // TODO: Get BFD ID and MBI
                    consentDAO.persistConsent(consent.get());
                }
            } catch (InvalidSuppressionRecordException e) {
                logger.error("Invalid suppression record", e);
                continue;
            }
        }
    }
}
