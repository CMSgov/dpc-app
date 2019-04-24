package gov.cms.dpc.api.models;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Implements the model for the JSON response for a complete job. See https://hl7.org/fhir/2018May/async.html for details.
 */
public class JobCompletionModel {

    // FIXME(nickrobison): This needs to the time that the export request actually started, rather than just when it was retrieved.
    private OffsetDateTime transactionTime;
    private String request;

    /**
     * Boolean value indicating whether downloading the generated files will require an authentication token
     */
    private final boolean secure = false;

    /**
     * List of output entries with one entry for each generated file
     */
    private List<OutputEntryModel> output;

    // FIXME(nickrobison): Should return errors as well. OperationOutcomes, serialized as NDJSON
    private final List<String> error = new ArrayList<>();

    public JobCompletionModel() {

    }

    public JobCompletionModel(OffsetDateTime transactionTime, String request, List<OutputEntryModel> output) {
        this.transactionTime = transactionTime;
        this.request = request;
        this.output = output;
    }

    public OffsetDateTime getTransactionTime() {
        return transactionTime;
    }

    public void setTransactionTime(OffsetDateTime transactionTime) {
        this.transactionTime = transactionTime;
    }

    public String getRequest() {
        return request;
    }

    public void setRequest(String request) {
        this.request = request;
    }

    public boolean isSecure() {
        return secure;
    }

    public List<OutputEntryModel> getOutput() {
        return output;
    }

    public void setOutput(List<OutputEntryModel> output) {
        this.output = output;
    }

    public List<String> getError() {
        return error;
    }
}
