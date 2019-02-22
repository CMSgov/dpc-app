package gov.cms.dpc.web.models;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public class JobCompletionModel {


    // FIXME(nickrobison): This needs to the time that the export request actually started, rather than just when it was retrieved.
    private OffsetDateTime transactionTime;
    private String request;
    private final boolean requiresAccessToken = false;
    private List<String> output;
    // FIXME(nickrobison): Should return errors as well. OperationOutcomes, serialized as NDJSON
    private final List<String> error = new ArrayList<>();

    public JobCompletionModel() {

    }

    public JobCompletionModel(OffsetDateTime transactionTime, String request, List<String> output) {
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

    public boolean isRequiresAccessToken() {
        return requiresAccessToken;
    }

    public List<String> getOutput() {
        return output;
    }

    public void setOutput(List<String> output) {
        this.output = output;
    }

    public List<String> getError() {
        return error;
    }
}
