package gov.cms.dpc.consent.jobs;

import com.google.inject.Inject;
import com.google.inject.Injector;
import gov.cms.dpc.common.entities.ConsentEntity;
import gov.cms.dpc.common.hibernate.attribution.DPCManagedSessionFactory;
import gov.cms.dpc.consent.jdbi.ConsentDAO;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
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
    private SessionFactory sessionFactory;

    private ConsentDAO consentDAO;

    @Inject
    public String suppressionFileDir;

    @Inject
    public SuppressionFileImport() {
        // Manually load the Guice injector. Since the job loads at the beginning of the startup process, Guice is not automatically injected.
        final Injector injector = (Injector) SundialJobScheduler.getServletContext().getAttribute("com.google.inject.Injector");
        injector.injectMembers(this);
        this.consentDAO = new ConsentDAO(new DPCManagedSessionFactory(sessionFactory));
    }

    @Override
    public void doRun() throws JobInterruptException {
        try (Stream<Path> paths = Files.walk(Paths.get(suppressionFileDir))) {
            paths.filter(Files::isRegularFile).forEach(p -> {
                if (Files.isReadable(p) && SuppressionFileUtils.is1800File(p)) {
                    importFile(p);
                }
                try {
                    Files.delete(p);
                } catch (IOException e) {
                    logger.error(String.format("Cannot delete file %s from suppression directory", p.toString()), e);
                }
            });
        } catch (IOException e) {
            logger.error("Cannot read files in suppression directory", e);
        }
    }

    protected void importFile(Path path) {
        BufferedReader reader;
        try {
            reader = Files.newBufferedReader(path);
        } catch (IOException e) {
            logger.error("Cannot import suppression file", e);
            return;
        }

        Session session = sessionFactory.openSession();

        ManagedSessionContext.bind(session);
        Transaction transaction = session.beginTransaction();
        LineIterator lineIter = IOUtils.lineIterator(reader);
        while (lineIter.hasNext()) {
            Optional<ConsentEntity> consent = SuppressionFileUtils.entityFromLine(lineIter.nextLine());
            if (consent.isPresent()) {
                // TODO: Get BFD ID and MBI
                consentDAO.persistConsent(consent.get());
            }
        }
        transaction.commit();
        session.close();
        ManagedSessionContext.unbind(sessionFactory);
    }
}
