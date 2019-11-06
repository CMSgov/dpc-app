package gov.cms.dpc.consent.jobs;

import gov.cms.dpc.bluebutton.client.BlueButtonClient;
import gov.cms.dpc.consent.entities.ConsentEntity;
import gov.cms.dpc.consent.exceptions.InvalidSuppressionRecordException;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Patient;

import java.io.*;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import static java.nio.file.Files.newInputStream;

public class SuppressionFileReader extends BufferedReader {

    static final Pattern HICN_PATTERN = Pattern.compile("\\d{9}[A-Za-z0-9]{0,2}");

    private String filename;
    private BlueButtonClient bfdClient;
    protected String line;
    protected ConsentEntity consent;
    private int lineNum = 0;

    public SuppressionFileReader(Path filePath, BlueButtonClient bfdClient) throws IOException {
        super(new InputStreamReader(newInputStream(filePath)));
        this.filename = filePath.getFileName().toString();
        this.bfdClient = bfdClient;
    }

    protected Optional<ConsentEntity> entityFromLine() throws InvalidSuppressionRecordException, IOException {
        line = super.readLine();
        lineNum++;
        consent = new ConsentEntity();

        final int sourceCodeStart = 362, sourceCodeEnd = 367;

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
            throw new InvalidSuppressionRecordException(String.format("Unexpected beneficiary data sharing source code %s", sourceCode), filename, lineNum);
        }

        setDates();
        setIdentifiers();
        setCodes();

        return Optional.ofNullable(consent);
    }

    protected void setDates() {
        final int effectiveDateStart = 354, effectiveDateEnd = 362;
        String effectiveDateStr = line.substring(effectiveDateStart, effectiveDateEnd);
        LocalDate effectiveDate = LocalDate.parse(effectiveDateStr, DateTimeFormatter.ofPattern("yyyyMMdd"));
        try {
            consent.setEffectiveDate(effectiveDate);
        } catch (DateTimeParseException e) {
            throw new InvalidSuppressionRecordException("Cannot parse date from suppression record", filename, lineNum, e);
        }
    }

    protected void setIdentifiers() {
        final int hicnStart = 0, hicnEnd = 11;
        String hicn = line.substring(hicnStart, hicnEnd).trim();
        if (!HICN_PATTERN.matcher(hicn).matches()) {
            throw new InvalidSuppressionRecordException("HICN does not match expected format", filename, lineNum);
        }
        consent.setHicn(hicn);

        Bundle patientBundle = bfdClient.searchPatientFromServerByHICN(hicn);
        if (patientBundle.isEmpty()) {
            return;
        }

        Patient patient = (Patient) patientBundle.getEntry().get(0).getResource();
        List<Identifier> identifiers = patient.getIdentifier();
        Optional<Identifier> beneId = identifiers.stream().filter(i -> "https://bluebutton.cms.gov/resources/variables/bene_id".equals(i.getSystem())).findFirst();
        if (beneId.isEmpty()) {
            throw new InvalidSuppressionRecordException("No beneficiary ID found in BFD Patient record", filename, lineNum);
        }
        consent.setBfdPatientId(beneId.get().getValue());

        Optional<Identifier> mbi = identifiers.stream().filter(i -> "http://hl7.org/fhir/sid/us-mbi".equals(i.getSystem())).findFirst();
        if (mbi.isEmpty()) {
            throw new InvalidSuppressionRecordException("No MBI found in BFD Patient record", filename, lineNum);
        }
        consent.setMbi(mbi.get().getValue());
    }

    protected void setCodes() {
        final int prefIndicatorStart = 368, prefIndicatorEnd = 369;
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
                throw new InvalidSuppressionRecordException("Beneficiary data sharing preference does not match expected value of 'Y' or 'N'", filename, lineNum);
        }

        consent.setPurposeCode("TREAT");
        consent.setLoincCode("64292-6");
        consent.setScopeCode("patient-privacy");
    }
}
