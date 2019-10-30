package gov.cms.dpc.consent.jobs;

import gov.cms.dpc.consent.entities.ConsentEntity;
import gov.cms.dpc.consent.exceptions.InvalidSuppressionRecordException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.regex.Pattern;

public class SuppressionFileUtils {

    private static final Logger logger = LoggerFactory.getLogger(SuppressionFileUtils.class);

    static final Pattern FILENAME_PATTERN = Pattern.compile("(P|T)#EFT\\.ON\\.ACO\\.NGD1800\\.DPRF\\.D\\d{6}\\.T\\d{7}");
    static final Pattern HICN_PATTERN = Pattern.compile("\\d{9}[A-Za-z0-9]{0,2}");

    private SuppressionFileUtils() {}

    protected static boolean is1800File(Path path) {
        return FILENAME_PATTERN.matcher(path.getFileName().toString()).matches();
    }

    protected static Optional<ConsentEntity> entityFromLine(String line) throws InvalidSuppressionRecordException {
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
            // If the source is blank, ignore this record
            if (StringUtils.isBlank(sourceCode)) {
                return Optional.empty();
            }
            throw new InvalidSuppressionRecordException("Unexpected beneficiary data sharing source code");
        }

        ConsentEntity consent = new ConsentEntity();

        String effectiveDateStr = line.substring(effectiveDateStart, effectiveDateEnd);
        LocalDate effectiveDate = LocalDate.parse(effectiveDateStr, DateTimeFormatter.ofPattern("yyyyMMdd"));
        try {
            consent.setEffectiveDate(effectiveDate);
        } catch (DateTimeParseException e) {
            throw new InvalidSuppressionRecordException("Cannot parse date from suppression record", e);
        }

        String hicn = line.substring(hicnStart, hicnEnd).trim();
        if (!HICN_PATTERN.matcher(hicn).matches()) {
            throw new InvalidSuppressionRecordException("HICN does not match expected format");
        }
        consent.setHicn(hicn);

        // Convert beneficiary data sharing preference indicator into value used by Consent resource
        String prefIndicator = line.substring(prefIndicatorStart, prefIndicatorEnd);
        switch (prefIndicator) {
            case "Y":
                consent.setPolicyCode("OPTIN");
                break;
            case "N":
                consent.setPolicyCode("OPTOUT");
                break;
            default:
                throw new InvalidSuppressionRecordException("Beneficiary data sharing preference does not match expected value of 'Y' or 'N'");
        }

        consent.setPurposeCode("TREAT");
        consent.setLoincCode("64292-6");
        consent.setScopeCode("patient-privacy");

        return Optional.ofNullable(consent);
    }
}
