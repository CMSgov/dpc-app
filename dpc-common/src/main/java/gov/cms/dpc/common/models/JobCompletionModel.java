package gov.cms.dpc.common.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import gov.cms.dpc.common.converters.jackson.OffsetDateTimeToStringConverter;
import gov.cms.dpc.fhir.DPCResourceType;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Implements the model for the JSON response for a complete job.
 * See https://github.com/smart-on-fhir/fhir-bulk-data-docs/blob/master/export.md for details.
 */
public class JobCompletionModel {

    public static final String CHECKSUM_URL = "https://dpc.cms.gov/checksum";
    public static final String FILE_LENGTH_URL = "https://dpc.cms.gov/file_length";
    public static final String SUBMIT_TIME_URL = "https://dpc.cms.gov/submit_time";
    public static final String COMPLETE_TIME_URL = "https://dpc.cms.gov/complete_time";

    /**
     * An entry in the {@link JobCompletionModel} output field.
     */
    public static class OutputEntry {
        /**
         * the FHIR resource type that is contained in the file
         */
        private DPCResourceType type;

        /**
         * the path to the file
         */
        private String url;

        /**
         * The number of resources in the file
         */
        private Integer count;

        /**
         * Extension object to hold additional information.
         */
        private List<FhirExtension> extension;

        public OutputEntry() {
            // Jackson required
        }

        public OutputEntry(DPCResourceType type, String url, Integer count, List<FhirExtension> extension) {
            this.type = type;
            this.url = url;
            this.count = count;
            this.extension = extension;
        }

        public String getUrl() {
            return url;
        }

        public DPCResourceType getType() {
            return type;
        }

        public Integer getCount() {
            return count;
        }

        public List<FhirExtension> getExtension() {
            return extension;
        }
    }

    /**
     * An extension field for additional information in an {@link OutputEntry}.
     */
    @JsonInclude(Include.NON_EMPTY)
    public static class FhirExtension {
        private String url;
        private String valueString;
        private Long valueDecimal;
        @JsonSerialize(converter = OffsetDateTimeToStringConverter.class)
        private OffsetDateTime valueDateTime;

        public FhirExtension() {
            // Jackson required
        }

        public FhirExtension(String url, String valueString) {
            this.url = url;
            this.valueString = valueString;
        }

        public FhirExtension(String url, Long valueDecimal) {
            this.url = url;
            this.valueDecimal = valueDecimal;
        }

        public FhirExtension(String url, OffsetDateTime date) {
            this.url = url;
            this.valueDateTime = date;
        }

        public String getUrl() {
            return url;
        }

        public String getValueString() {
            return valueString;
        }

        public Long getValueDecimal() {
            return valueDecimal;
        }

        public OffsetDateTime getValueDateTime() {
            return valueDateTime;
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

    /**
     * Do requests to NDJSON files require an access token. Part of the FHIR spec.
     */
    private final boolean requiresAccessToken = true;

    /**
     * The output entries
     */
    private List<OutputEntry> output;

    /**
     * The Operational Outcome entries
     */
    private List<OutputEntry> error;

    /**
     * The FHIR extensions associated with this job
     */
    private List<FhirExtension> extension;

    public JobCompletionModel() {
        // Jackson required
    }

    public JobCompletionModel(OffsetDateTime transactionTime, String request, List<OutputEntry> output, List<OutputEntry> error, List<FhirExtension> extension) {
        this.transactionTime = transactionTime;
        this.request = request;
        this.output = output;
        this.error = error;
        this.extension = extension != null && extension.isEmpty() ? null : extension;
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

    public List<FhirExtension> getExtension() {
        return extension;
    }

    public void setExtension(List<FhirExtension> extension) {
        this.extension = extension;
    }
}
