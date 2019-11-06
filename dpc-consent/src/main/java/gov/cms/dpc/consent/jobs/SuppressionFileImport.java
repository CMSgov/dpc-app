package gov.cms.dpc.consent.jobs;

import com.google.inject.Inject;
import com.google.inject.Injector;
import gov.cms.dpc.bluebutton.client.BlueButtonClient;
import gov.cms.dpc.bluebutton.client.BlueButtonClientManager;
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
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Patient;
import org.knowm.sundial.Job;
import org.knowm.sundial.SundialJobScheduler;
import org.knowm.sundial.exceptions.JobInterruptException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.Id;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class SuppressionFileImport extends Job {

    private static final Logger logger = LoggerFactory.getLogger(SuppressionFileImport.class);
    static final Pattern FILENAME_PATTERN = Pattern.compile("(P|T)#EFT\\.ON\\.ACO\\.NGD1800\\.DPRF\\.D\\d{6}\\.T\\d{7}");

    @Inject
    private DPCConsentManagedSessionFactory managedSessionFactory;

    private ConsentDAO consentDAO;

    @Inject
    public String suppressionFileDir;

    @Inject
    public BlueButtonClient bfdClient;

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
                if (Files.isReadable(p) && is1800File(p)) {
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

    protected static boolean is1800File(Path path) {
        return FILENAME_PATTERN.matcher(path.getFileName().toString()).matches();
    }

    private void importFile(Path path) {
        try (SuppressionFileReader reader = new SuppressionFileReader(path, bfdClient)) {
            importRecords(reader);
        } catch (IOException e) {
            logger.error("Cannot import suppression file", e);
            return;
        }
    }

    private void importRecords(SuppressionFileReader reader) {
        SessionFactory sessionFactory = managedSessionFactory.getSessionFactory();
        Session session = sessionFactory.openSession();

        ManagedSessionContext.bind(session);
        Transaction transaction = session.beginTransaction();

        try {
            buildAndSaveConsentRecords(reader);
            transaction.commit();
        } catch (Exception e) {
            logger.error("Cannot commit suppression file import transaction", e);
            transaction.rollback();
        } finally {
            session.close();
            ManagedSessionContext.unbind(sessionFactory);
        }
    }

    private void buildAndSaveConsentRecords(SuppressionFileReader reader) throws IOException {
        while (reader.ready()) {
            try {
                Optional<ConsentEntity> consent = reader.entityFromLine();
                if (consent.isPresent()) {
                    consentDAO.persistConsent(consent.get());
                }
            } catch (InvalidSuppressionRecordException e) {
                logger.error("Invalid suppression record", e);
                continue;
            }
        }
    }
}
