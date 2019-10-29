package gov.cms.dpc.consent.jobs;

import gov.cms.dpc.common.entities.ConsentEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;

public class SuppressionFileUtils {

    private static final Logger logger = LoggerFactory.getLogger(SuppressionFileUtils.class);

    private SuppressionFileUtils() {}

    protected static boolean is1800File(Path path) {
        return path.getFileName().toString().matches("(P|T)#EFT\\.ON\\.ACO\\.NGD1800\\.DPRF\\.D\\d{6}\\.T\\d{7}");
    }

    protected static Optional<ConsentEntity> entityFromLine(String line) {
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
