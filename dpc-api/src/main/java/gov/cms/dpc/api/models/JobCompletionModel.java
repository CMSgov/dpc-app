package gov.cms.dpc.api.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import gov.cms.dpc.common.converters.OffsetDateTimeToStringConverter;
import org.hl7.fhir.dstu3.model.ResourceType;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * Implements the model for the JSON response for a complete job.
 * See https://github.com/smart-on-fhir/fhir-bulk-data-docs/blob/master/export.md for details.
 */
public class JobCompletionModel {
    /**
     * An entry in the {@link JobCompletionModel} output field.
     */
    public static class OutputEntry {
        /**
         * the FHIR resource type that is contained in the file
         */
        private ResourceType type;

        /**
         * the path to the file
         */
        private String url;

        /**
         * The number of resources in the file
         */
        private Integer count;

        public OutputEntry() {
            // Jackson required
        }

        public OutputEntry(ResourceType type, String url, Integer count) {
            this.type = type;
            this.url = url;
            this.count = count;
        }

        public String getUrl() {
            return url;
        }

        public ResourceType getType() {
            return type;
        }

        public Integer getCount() {
            return count;
        }
    }

    /**
     * An instant type that indicates the server's time when the query is run. No resources that have a modified data after this instant should be in the response.
     */
    @JsonSerialize(converter = OffsetDateTimeToStringConverter.class)
    private OffsetDateTime transactionTime;

    /**
     * The full request of the original request URL
     */
    private String request;
    private final boolean requiresAccessToken = true;
    private List<OutputEntry> output;
    private List<OutputEntry> error;

    @JsonInclude(Include.NON_NULL)
    private Map<String, Object> encryptionParameters;

    public JobCompletionModel() {
        // Jackson required
    }

    public JobCompletionModel(OffsetDateTime transactionTime, String request, List<OutputEntry> output, List<OutputEntry> error) {
        this.transactionTime = transactionTime;
        this.request = request;
        this.output = output;
        this.error = error;
    }

    public JobCompletionModel(OffsetDateTime transactionTime, String request, List<OutputEntry> output, List<OutputEntry> error, Map<String, Object> encryptionParameters) {
        this.transactionTime = transactionTime;
        this.request = request;
        this.output = output;
        this.error = error;
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

    public List<OutputEntry> getOutput() {
        return output;
    }

    public void setOutput(List<OutputEntry> output) {
        this.output = output;
    }

    public List<OutputEntry> getError() {
        return error;
    }

    public void setError(List<OutputEntry> error) {
        this.error = error;
    }

    public Map<String, Object> getEncryptionParameters() {
        return encryptionParameters;
    }

    public void setEncryptionParameters(Map<String, Object> encryptionParameters) {
        this.encryptionParameters = encryptionParameters;
    }
}
