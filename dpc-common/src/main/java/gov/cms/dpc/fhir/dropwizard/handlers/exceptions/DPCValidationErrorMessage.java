package gov.cms.dpc.fhir.dropwizard.handlers.exceptions;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import io.dropwizard.jersey.validation.ValidationErrorMessage;

/**
 * Subclass of {@link ValidationErrorMessage} which adds an ID field for correlating the response to the underlying exception
 */
public class DPCValidationErrorMessage extends ValidationErrorMessage {

    private final long id;

    @JsonCreator
    DPCValidationErrorMessage(@JsonProperty("id") long id, @JsonProperty("errors") ImmutableList<String> errors) {
        super(errors);
        this.id = id;
    }

    @JsonProperty
    public long getId() {
        return id;
    }
}
