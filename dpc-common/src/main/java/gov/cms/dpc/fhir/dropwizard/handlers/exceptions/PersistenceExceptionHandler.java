package gov.cms.dpc.fhir.dropwizard.handlers.exceptions;

import gov.cms.dpc.fhir.FHIRMediaTypes;
import jakarta.inject.Inject;
import jakarta.persistence.PersistenceException;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.apache.commons.lang3.tuple.Pair;
import org.hibernate.exception.ConstraintViolationException;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.OperationOutcome;
import org.postgresql.util.PSQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.BatchUpdateException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Custom exception handler for {@link PersistenceException} handling.
 * It checks to see if the underlying exception is a {@link ConstraintViolationException} and if so, returns a {@link Response.Status#BAD_REQUEST} rather than a {@link Response.Status#INTERNAL_SERVER_ERROR}
 * It also makes sure we don't leak any DB information to the end user.
 */
@Provider
public class PersistenceExceptionHandler extends AbstractFHIRExceptionHandler<PersistenceException> {

    private static final Logger logger = LoggerFactory.getLogger(PersistenceExceptionHandler.class);
    private static final Pattern MSG_PATTERN = Pattern.compile("ERROR:\\s(duplicate\\s[a-zA-Z_]*\\svalue\\sviolates\\sunique\\sconstraint)");

    @Inject
    PersistenceExceptionHandler(@Context ResourceInfo info) {
        super(info);
    }

    @Override
    public Response toResponse(PersistenceException exception) {
        if (isFHIRResource()) {
            return handleFHIRException(exception);
        } else {
            return handleNonFHIRException(exception);
        }
    }

    @Override
    Response handleFHIRException(PersistenceException exception) {
        final Pair<Response.Status, String> statusStringPair = handleResponseGeneration(exception);
        final long exceptionID = this.logException(exception);

        final OperationOutcome outcome = new OperationOutcome();
        outcome.setId(exceptionIDtoHex(exceptionID));
        outcome.addIssue()
                .setSeverity(OperationOutcome.IssueSeverity.FATAL)
                .setDetails(new CodeableConcept().setText(statusStringPair.getRight()));

        return Response
                .status(statusStringPair.getLeft())
                .type(FHIRMediaTypes.FHIR_JSON)
                .entity(outcome)
                .build();
    }

    @Override
    Response handleNonFHIRException(PersistenceException exception) {
        final long exceptionID = super.logException(exception);
        final Pair<Response.Status, String> statusStringPair = handleResponseGeneration(exception);
        return Response
                .status(statusStringPair.getLeft())
                .entity(String.format(ERROR_MSG_FMT, exceptionID, statusStringPair.getRight()))
                .build();
    }

    private Pair<Response.Status, String> handleResponseGeneration(PersistenceException exception) {
        final String message;
        Response.Status status = Response.Status.BAD_REQUEST;
        if (exception.getCause() instanceof ConstraintViolationException constraintViolationException) {
            message = generateErrorMessage(constraintViolationException);
        } else if (exception.getCause() instanceof PSQLException
                || exception.getCause() instanceof BatchUpdateException) {
            message = exception.getMessage();
        } else {
            logger.error("Cannot persist to DB", exception);
            message = "Internal server error";
            status = Response.Status.INTERNAL_SERVER_ERROR;
        }

        return Pair.of(status, message);
    }

    private String generateErrorMessage(ConstraintViolationException exception) {

        String message;

        // Try to see if we can pull anything meaningful out of the constraint violation
        final String inputMessage = exception.getSQLException().getMessage();
        final Matcher matcher = MSG_PATTERN.matcher(inputMessage);
        if (matcher.lookingAt()) {
            try {
                message = matcher.group(1);
            } catch (IndexOutOfBoundsException e) {
                logger.error("Failed to parse constraint from error message: {}", inputMessage, e);
                message = inputMessage;
            }
        } else {
            message = inputMessage;
        }

        return message;
    }
}
