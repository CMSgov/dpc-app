package gov.cms.dpc.common.hibernate;

import io.dropwizard.jersey.errors.ErrorMessage;
import org.hibernate.exception.ConstraintViolationException;

import javax.inject.Inject;
import javax.persistence.PersistenceException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * Custom Dropwizard {@link ExceptionMapper} for handling database errors.
 * It unwraps the underlying exception and builds a meaningful error message.
 *
 */
@Provider
public class DatabaseExceptionHandler implements ExceptionMapper<PersistenceException> {

    @Inject
    DatabaseExceptionHandler() {
        // Not used
    }

    @Override
    public Response toResponse(PersistenceException exception) {
        int status = 500;
        String message = Response.Status.BAD_REQUEST.getReasonPhrase();

        // If it's a Hibernate validation exception, return a bad data code with the underlying cause
        if (exception.getCause() instanceof ConstraintViolationException) {
            status = Response.Status.BAD_REQUEST.getStatusCode();
            // FIXME: This should not be returned to the user.
            message = ((ConstraintViolationException) exception.getCause()).getMessage();
        }

        return Response.status(status)
                .entity(new ErrorMessage(status, message))
                .build();
    }
}
