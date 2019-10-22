package gov.cms.dpc.fhir.dropwizard.handlers;

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
    Response handleFHIRException(JerseyViolationException exception) {
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
