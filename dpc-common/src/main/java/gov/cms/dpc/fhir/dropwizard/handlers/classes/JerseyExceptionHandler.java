package gov.cms.dpc.fhir.dropwizard.handlers.classes;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import io.dropwizard.jersey.validation.ConstraintMessage;
import io.dropwizard.jersey.validation.JerseyViolationException;
import io.dropwizard.jersey.validation.ValidationErrorMessage;
import org.glassfish.jersey.server.model.Invocable;

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
        return Response.status(status)
                .entity(new ValidationErrorMessage(errors))
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
