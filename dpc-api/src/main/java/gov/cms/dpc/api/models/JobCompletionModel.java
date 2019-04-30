package gov.cms.dpc.api.models;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import gov.cms.dpc.api.converters.OffsetDateTimeToStringConverter;
import org.hl7.fhir.dstu3.model.ResourceType;

import java.time.OffsetDateTime;
import java.util.ArrayList;
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

        public OutputEntry(ResourceType type, String url) {
            this.type = type;
            this.url = url;
        }

        public String getUrl() {
            return url;
        }

        public ResourceType getType() {
            return type;
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
    private final boolean requiresAccessToken = false;
    private List<OutputEntry> output;
    // FIXME(rickhawes): DPC-205 will fill in this array.
    private final List<String> error = new ArrayList<>();
    private Map<String, Object> encryptionParameters;

    public JobCompletionModel(OffsetDateTime transactionTime, String request, List<OutputEntry> output) {
        this.transactionTime = transactionTime;
        this.request = request;
        this.output = output;
    }

    public JobCompletionModel(OffsetDateTime transactionTime, String request, List<OutputEntry> output, Map<String, Object> encryptionParameters) {
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

    public List<OutputEntry> getOutput() {
        return output;
    }

    public void setOutput(List<OutputEntry> output) {
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
