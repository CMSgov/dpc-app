package gov.cms.dpc.fhir.dropwizard.handlers.classes;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import io.dropwizard.jersey.validation.ConstraintMessage;
import io.dropwizard.jersey.validation.JerseyViolationException;
import io.dropwizard.jersey.validation.ValidationErrorMessage;
import org.glassfish.jersey.server.model.Invocable;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.OperationOutcome;

import javax.inject.Inject;
import javax.validation.ConstraintViolation;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.util.Set;

@Provider
public class JerseyExceptionHandler extends AbstractFHIRExceptionHandler<JerseyViolationException> {

    @Inject
    JerseyExceptionHandler() {
        super();
    }

    @Override
    public Response toResponse(JerseyViolationException exception) {
        if (isFHIRResource()) {
            return handleFHIRException(exception);
        } else {
            return handleNonFHIRException(exception);
        }
    }

    @Override
    Response handleFHIRException(JerseyViolationException exception) {
        // TODO: Need to log and correlate this exception
        // FIXME: This needs to be handled specially for FHIR exceptions
        final Set<ConstraintViolation<?>> violations = exception.getConstraintViolations();
        final Invocable invocable = exception.getInvocable();
        //noinspection Guava Replacing with Streams means a redundent copy when building the response
        final ImmutableList<String> errors = FluentIterable.from(exception.getConstraintViolations())
                .transform(violation -> ConstraintMessage.getMessage(violation, invocable)).toList();

        final int status = ConstraintMessage.determineStatus(violations, invocable);
        final OperationOutcome outcome = new OperationOutcome();

        errors.forEach(error -> {
            final OperationOutcome.OperationOutcomeIssueComponent component = new OperationOutcome.OperationOutcomeIssueComponent();
            component.setSeverity(OperationOutcome.IssueSeverity.ERROR);
            component.setCode(OperationOutcome.IssueType.INVALID);
            component.setDetails(new CodeableConcept().setText(error));
            outcome.addIssue(component);
        });

        // Create an Operation Outcome and add all the exceptions
        return Response.status(status)
                .entity(outcome)
                .build();
    }

    @Override
    Response handleNonFHIRException(JerseyViolationException exception) {
        // TODO: Need to log and correlate this exception
        final Set<ConstraintViolation<?>> violations = exception.getConstraintViolations();
        final Invocable invocable = exception.getInvocable();
        //noinspection Guava Replacing with Streams means a redundent copy when building the response
        final ImmutableList<String> errors = FluentIterable.from(exception.getConstraintViolations())
                .transform(violation -> ConstraintMessage.getMessage(violation, invocable)).toList();

        final int status = ConstraintMessage.determineStatus(violations, invocable);
        return Response.status(status)
                .entity(new ValidationErrorMessage(errors))
                .build();
    }
}
