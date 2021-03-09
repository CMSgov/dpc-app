package gov.cms.dpc.aggregation.util;

import gov.cms.dpc.aggregation.engine.OutcomeReason;
import org.bouncycastle.jcajce.provider.digest.SHA256;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.OperationOutcome;
import org.hl7.fhir.dstu3.model.StringType;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

public final class AggregationUtils {

    private AggregationUtils() {
        // static methods only
    }

    public static byte[] generateChecksum(File file) throws IOException {
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            return new SHA256.Digest().digest(fileInputStream.readAllBytes());
        }
    }

    public static OperationOutcome toOperationOutcome(OutcomeReason failReason, String patientID) {
        final var patientLocation = List.of(new StringType("Patient"), new StringType("id"), new StringType(patientID));
        final var outcome = new OperationOutcome();
        outcome.addIssue()
                .setSeverity(OperationOutcome.IssueSeverity.ERROR)
                .setCode(OperationOutcome.IssueType.EXCEPTION)
                .setDetails(new CodeableConcept().setText(failReason.detail))
                .setLocation(patientLocation);
        return outcome;
    }
}
