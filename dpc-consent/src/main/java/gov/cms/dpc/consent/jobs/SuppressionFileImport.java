package gov.cms.dpc.consent.jobs;

import com.google.inject.Inject;
import gov.cms.dpc.common.entities.ConsentEntity;
import gov.cms.dpc.consent.jdbi.ConsentDAO;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.knowm.sundial.Job;
import org.knowm.sundial.exceptions.JobInterruptException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;
import java.util.stream.Stream;

public class SuppressionFileImport extends Job {

    private static final Logger logger = LoggerFactory.getLogger(SuppressionFileImport.class);

    private ConsentDAO consentDAO;

    @Inject
    public SuppressionFileImport(ConsentDAO consentDAO) {
        this.consentDAO = consentDAO;
    }

    @Override
    public void doRun() throws JobInterruptException {
        String dir = System.getenv("SUPPRESSION_FILE_DIR");
        try (Stream<Path> paths = Files.walk(Paths.get(dir))) {
            paths.filter(Files::isRegularFile).forEach(p -> {
                if (Files.isReadable(p) && is1800File(p)) {
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

    protected boolean is1800File(Path path) {
        return path.getFileName().toString().matches("(P|T)#EFT\\.ON\\.ACO\\.NGD1800\\.DPRF\\.D\\d{6}\\.T\\d{7}");
    }

    protected void importFile(Path path) {
        BufferedReader reader;
        try {
            reader = Files.newBufferedReader(path);
        } catch (IOException e) {
            logger.error("Cannot import suppression file", e);
            return;
        }

        LineIterator lineIter = IOUtils.lineIterator(reader);
        while (lineIter.hasNext()) {
            Optional<ConsentEntity> consent = entityFromLine(lineIter.nextLine());
            if (consent.isPresent()) {
                // TODO: Get BFD ID and MBI
                consentDAO.persistConsent(consent.get());
            }
        }
    }

    protected Optional<ConsentEntity> entityFromLine(String line) {
        final int hicnStart = 0, hicnEnd = 11,
                effectiveDateStart = 354, effectiveDateEnd = 362,
                sourceCodeStart = 362, sourceCodeEnd = 367,
                prefIndicatorStart = 368, prefIndicatorEnd = 369;

        if (line.startsWith("HDR_BENEDATASHR") || line.startsWith("TRL_BENEDATASHR")) {
            // Header or trailer; no record to be read
            return Optional.empty();
        }

        String sourceCode = line.substring(sourceCodeStart, sourceCodeEnd);
        if (!sourceCode.trim().matches("1-?800")) {
            // If the source is not 1-800-MEDICARE, ignore this record
            return Optional.empty();
        }

        ConsentEntity consent = new ConsentEntity();

        String effectiveDateStr = line.substring(effectiveDateStart, effectiveDateEnd);
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");
        try {
            Date effectiveDate = formatter.parse(effectiveDateStr);
            consent.setEffectiveDate(effectiveDate);
        } catch (ParseException e) {
            logger.warn("Cannot parse date from suppression record", e);
            return Optional.empty();
        }

        String hicn = line.substring(hicnStart, hicnEnd).trim();
        consent.setHicn(hicn);

        String prefIndicator = line.substring(prefIndicatorStart, prefIndicatorEnd);
        consent.setPolicyCode("Y".equals(prefIndicator) ? "OPTIN" : "N".equals(prefIndicator) ? "OPTOUT" : null);

        consent.setPurposeCode("TREAT");
        consent.setLoincCode("64292-6");
        consent.setScopeCode("patient-privacy");

        return Optional.ofNullable(consent);
    }
}
