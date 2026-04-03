package gov.cms.dpc.aggregation.util;

import gov.cms.dpc.aggregation.engine.OutcomeReason;
import org.bouncycastle.jcajce.provider.digest.SHA256;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.OperationOutcome;
import org.hl7.fhir.dstu3.model.StringType;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.zip.GZIPInputStream;

public final class AggregationUtils {

    private AggregationUtils() {
        // static methods only
    }

    /**
     * Reads the contents of the given compressed file and returns its checksum.
     * @param file A gzipped file.
     * @return Checksum
     * @throws IOException If the file isn't gzip compressed or can't be read.
     */
    public static byte[] generateChecksum(File file) throws IOException {
        try (
            FileInputStream fileInputStream = new FileInputStream(file);
            GZIPInputStream gzipInputStream = new GZIPInputStream(fileInputStream);
        ) {
            return generateChecksum(gzipInputStream);
        }
    }

    /**
     * Reads the contents of the given input stream to the end and returns its checksum.
     * @param inputStream The {@link InputStream} to read.
     * @return Checksum
     * @throws IOException If the stream can't be read.
     */
    public static byte[] generateChecksum(InputStream inputStream) throws IOException {
        return new SHA256.Digest().digest(inputStream.readAllBytes());
    }

    /**
     * Returns an {@link OperationOutcome} of type Exception.
     * @param failReason The reason the operation failed
     * @param patientID The patient's ID
     * @return {@link OperationOutcome}
     */
    public static OperationOutcome toOperationOutcome(OutcomeReason failReason, String patientID) {
        return toOperationOutcome(failReason, patientID, OperationOutcome.IssueType.EXCEPTION);
    }

    /**
     * Returns an {@link OperationOutcome} of type Exception.
     * @param failReason The reason the operation failed
     * @param patientID The patient's ID
     * @param issueType The {@link OperationOutcome.IssueType} that lead to the {@link OperationOutcome}
     * @return {@link OperationOutcome}
     */
    public static OperationOutcome toOperationOutcome(OutcomeReason failReason, String patientID, OperationOutcome.IssueType issueType) {
        final var patientLocation = List.of(new StringType("Patient"), new StringType("id"), new StringType(patientID));
        final var outcome = new OperationOutcome();
        outcome.addIssue()
            .setSeverity(OperationOutcome.IssueSeverity.ERROR)
            .setCode(issueType)
            .setDetails(new CodeableConcept().setText(failReason.detail))
            .setLocation(patientLocation);
        return outcome;
    }
}
