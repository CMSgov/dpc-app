package gov.cms.dpc.fhir.dropwizard.handlers.exceptions;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import io.dropwizard.jersey.validation.ValidationErrorMessage;

/**
 * Subclass of {@link ValidationErrorMessage} which adds an ID field for correlating the response to the underlying exception
 */
public class DPCValidationErrorMessage extends ValidationErrorMessage {

    private final String exceptionID;

    @JsonCreator
    DPCValidationErrorMessage(@JsonProperty("id") String exceptionID, @JsonProperty("errors") ImmutableList<String> errors) {
        super(errors);
        this.exceptionID = exceptionID;
    }

    @JsonProperty
    public String getId() {
        return exceptionID;
    }
}
