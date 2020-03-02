package gov.cms.dpc.aggregation.exceptions;

import java.util.Objects;

public class SuppressionException extends RuntimeException {

    public static final long serialVersionUID = 42L;

    public enum SuppressionReason {
        OPT_OUT,
        INACTIVE
    }

    private final SuppressionReason reason;
    private final String patientID;

    public SuppressionException(SuppressionReason reason, String patientID, String message) {
        super(String.format("Patient %s is suppressed from export due to %s reason: %s", patientID, reason.toString(), message));
        this.patientID = patientID;
        this.reason = reason;
    }

    public SuppressionReason getReason() {
        return reason;
    }

    public String getPatientID() {
        return patientID;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SuppressionException)) return false;
        SuppressionException exception = (SuppressionException) o;
        return reason == exception.reason &&
                Objects.equals(patientID, exception.patientID);
    }

    @Override
    public int hashCode() {
        return Objects.hash(reason, patientID);
    }
}
