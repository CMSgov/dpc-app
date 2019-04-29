package gov.cms.dpc.api.models;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class JobCompletionModel {


    // FIXME(nickrobison): This needs to the time that the export request actually started, rather than just when it was retrieved.
    private OffsetDateTime transactionTime;
    private String request;
    private final boolean requiresAccessToken = false;
    private List<String> output;
    // FIXME(nickrobison): Should return errors as well. OperationOutcomes, serialized as NDJSON
    private final List<String> error = new ArrayList<>();
    private Map<String, Object> encryptionParameters;

    public JobCompletionModel() {

    }

    public JobCompletionModel(OffsetDateTime transactionTime, String request, List<String> output) {
        this.transactionTime = transactionTime;
        this.request = request;
        this.output = output;
    }

    public JobCompletionModel(OffsetDateTime transactionTime, String request, List<String> output, Map<String, Object> encryptionParameters) {
        this.transactionTime = transactionTime;
        this.request = request;
        this.output = output;
        this.encryptionParameters = encryptionParameters;
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

    public Map<String, Object> getEncryptionParameters() {
        return encryptionParameters;
    }

    public void setEncryptionParameters(Map<String, Object> encryptionParameters) {
        this.encryptionParameters = encryptionParameters;
    }
}
