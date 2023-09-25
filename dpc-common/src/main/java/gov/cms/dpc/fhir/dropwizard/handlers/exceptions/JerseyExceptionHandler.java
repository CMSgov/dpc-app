package gov.cms.dpc.fhir.dropwizard.handlers.exceptions;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import io.dropwizard.jersey.validation.ConstraintMessage;
import io.dropwizard.jersey.validation.JerseyViolationException;
import org.apache.commons.lang3.tuple.Pair;
import org.glassfish.jersey.server.model.Invocable;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.OperationOutcome;

import javax.inject.Inject;
import javax.validation.ConstraintViolation;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.util.Set;

/**
 * Custom exception handler for {@link JerseyViolationException}.
 * It provides human readable error messages which can help pinpoint which
 * violates the user needs to address.
 */
@Provider
public class JerseyExceptionHandler extends AbstractFHIRExceptionHandler<JerseyViolationException> {

    @Inject
    JerseyExceptionHandler(@Context ResourceInfo info) {
        super(info);
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
        final long exceptionID = this.logException(exception);
        final Pair<ImmutableList<String>, Integer> errorStatusPair = processConstraintViolations(exception);
        final OperationOutcome outcome = new OperationOutcome();
        outcome.setId(exceptionIDtoHex(exceptionID));

        errorStatusPair.getLeft().forEach(error -> {
            final OperationOutcome.OperationOutcomeIssueComponent component = new OperationOutcome.OperationOutcomeIssueComponent();
            component.setSeverity(OperationOutcome.IssueSeverity.ERROR);
            component.setCode(OperationOutcome.IssueType.INVALID);
            component.setDetails(new CodeableConcept().setText(error));
            outcome.addIssue(component);
        });

        return Response.status(errorStatusPair.getRight())
                .entity(outcome)
                .build();
    }

    @Override
    Response handleNonFHIRException(JerseyViolationException exception) {
        final Pair<ImmutableList<String>, Integer> errorStatusPair = processConstraintViolations(exception);
        final long exceptionID = super.logException(exception);

        return Response.status(errorStatusPair.getRight())
                .entity(new DPCValidationErrorMessage(exceptionIDtoHex(exceptionID), errorStatusPair.getLeft()))
                .build();
    }

    private Pair<ImmutableList<String>, Integer> processConstraintViolations(JerseyViolationException exception) {
        final Set<ConstraintViolation<?>> violations = exception.getConstraintViolations();
        final Invocable invocable = exception.getInvocable();
        // noinspection Guava Replacing with Streams means a redundent copy when
        // building the response
        final ImmutableList<String> errors = FluentIterable.from(exception.getConstraintViolations())
                .transform(violation -> {
                    assert violation != null;
                    return ConstraintMessage.getMessage(violation, invocable);
                }).toList();
        final int status = ConstraintMessage.determineStatus(violations, invocable);

        return Pair.of(errors, status);
    }
}
