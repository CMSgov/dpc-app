package gov.cms.dpc.fhir.dropwizard.handlers;

import gov.cms.dpc.fhir.FHIRMediaTypes;
import gov.cms.dpc.fhir.annotations.FHIR;
import io.dropwizard.jersey.errors.LoggingExceptionMapper;
import io.dropwizard.jersey.validation.ConstraintMessage;
import io.dropwizard.jersey.validation.JerseyViolationException;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.OperationOutcome;

import javax.inject.Inject;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

/**
 * Custom error handling for translating {@link JerseyViolationException}s into {@link OperationOutcome}s.
 * We need this because for some reason, our default {@link FHIRExceptionHandler} doesn't correctly map against the specific exception type.
 */
@Provider
public class FHIRValidationExceptionHandler extends LoggingExceptionMapper<JerseyViolationException> {

    @Context
    private ResourceInfo info;

    @Inject
    public FHIRValidationExceptionHandler() {
        super();
    }

    @Override
    public Response toResponse(JerseyViolationException exception) {
        final Response response = super.toResponse(exception);

        // If it's a FHIR resource, create a custom operation outcome, when it's an error
        if (isFHIRResource()) {
            final OperationOutcome outcome = new OperationOutcome();

            // Do some custom handling of the validation errors
            exception
                    .getConstraintViolations()
                    .forEach(violation -> {
                        final OperationOutcome.OperationOutcomeIssueComponent component = new OperationOutcome.OperationOutcomeIssueComponent();

                        component.setDetails(new CodeableConcept().setText(violation.getMessage()))
                                .setSeverity(OperationOutcome.IssueSeverity.ERROR);

                        outcome.addIssue(component);
                    });

            // Determine the status from the violations
            final int status = ConstraintMessage.determineStatus(exception.getConstraintViolations(), exception.getInvocable());

            return Response.fromResponse(response)
                    .type(FHIRMediaTypes.FHIR_JSON)
                    .status(status)
                    .entity(outcome)
                    .build();
        }

        return response;
    }

    private boolean isFHIRResource() {
        return (this.info.getResourceClass() != null && this.info.getResourceClass().getAnnotation(FHIR.class) != null) ||
                (this.info.getResourceMethod() != null && this.info.getResourceMethod().getAnnotation(FHIR.class) != null);
    }
}
