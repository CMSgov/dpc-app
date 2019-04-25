package gov.cms.dpc.api.models;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import gov.cms.dpc.api.converters.OffsetDateTimeToStringConverter;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Implements the model for the JSON response for a complete job.
 * See https://github.com/smart-on-fhir/fhir-bulk-data-docs/blob/master/export.md for details.
 */
public class JobCompletionModel {

    /**
     * An instant type that indicates the server's time when the query is run. No resources that have a modified data after this instant should be in the response.
     */
    @JsonSerialize(converter = OffsetDateTimeToStringConverter.class)
    private OffsetDateTime transactionTime;

    /**
     * The full request of the original request URL
     */
    private String request;
    private final boolean requiresAccessToken = false;
    private List<OutputEntryModel> output;
    // FIXME(rickhawes): DPC-205 will fill in this array.
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

    public boolean isRequiresAccessToken() {
        return requiresAccessToken;
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
