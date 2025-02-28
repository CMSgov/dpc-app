package gov.cms.dpc.fhir.dropwizard.handlers.exceptions;

import gov.cms.dpc.fhir.FHIRMediaTypes;
import io.dropwizard.jersey.errors.LoggingExceptionMapper;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.OperationOutcome;

/**
 * Core error handler for differentiating between FHIR and standard HTTP errors.
 * This overrides *all* of Dropwizard's error handling, but for any non-FHIR resources, we simply delegate back to the root {@link LoggingExceptionMapper}
 */
@Provider
public class DefaultFHIRExceptionHandler extends AbstractFHIRExceptionHandler<Throwable> {

    @Inject
    public DefaultFHIRExceptionHandler(@Context ResourceInfo info) {
        super(info);
    }

    @Override
    public Response toResponse(Throwable exception) {
        if (isFHIRResource()) {
            return handleFHIRException(exception);
        } else {
            return handleNonFHIRException(exception);
        }
    }

    @Override
    Response handleFHIRException(Throwable exception) {
        final int statusCode;
        // Duplicating some of the logic from the parent LoggingExceptionMapper, because we need to get the logged ID
        // We just pass along redirects
        if (exception instanceof WebApplicationException webAppException) {
            final Response response = webAppException.getResponse();
            Response.Status.Family family = response.getStatusInfo().getFamily();
            if (family.equals(Response.Status.Family.REDIRECTION)) {
                return response;
            }
            // If it's any other type of web application exception, use the status as the response code.
            statusCode = ((WebApplicationException) exception).getResponse().getStatus();
        } else {
            // For any other types of errors, just set a 500 and move along
            statusCode = Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
        }
        // Log the exception and generate the OperationOutcome
        final long exceptionID = super.logException(exception);
        final OperationOutcome outcome = new OperationOutcome();
        outcome.setId(exceptionIDtoHex(exceptionID));
        outcome.addIssue()
                .setSeverity(OperationOutcome.IssueSeverity.FATAL)
                .setDetails(new CodeableConcept().setText(exception.getMessage()));

        return Response
                .status(statusCode)
                .type(FHIRMediaTypes.FHIR_JSON)
                .entity(outcome)
                .build();
    }

    @Override
    Response handleNonFHIRException(Throwable exception) {
        return super.toResponse(exception);
    }
}
