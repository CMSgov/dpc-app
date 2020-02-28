package gov.cms.dpc.aggregation.exceptions;

import java.util.UUID;

public class SuppressionException extends RuntimeException {

    public enum SuppressionReason {
        OPT_OUT,
        EXPIRED
    }

    public SuppressionException(SuppressionReason reason, String patientID, String message) {
        super(String.format("Patient %s is suppressed from export due to %s reason: %s", reason.toString(), patientID, message));
    }
}
