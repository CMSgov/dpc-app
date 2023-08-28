package gov.cms.dpc.fhir.dropwizard.handlers.exceptions;

import gov.cms.dpc.fhir.FHIRMediaTypes;
import io.dropwizard.jersey.errors.LoggingExceptionMapper;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.OperationOutcome;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

/**
 * Core error handler for differentiating between FHIR and standard HTTP errors.
 * This overrides *all* of Dropwizard's error handling, but for any non-FHIR resources, we simply delegate back to the root {@link LoggingExceptionMapper}
 */
@Provider
public class DefaultFHIRExceptionHandler extends AbstractFHIRExceptionHandler<Throwable> {

    @Context
    private ResourceInfo info;

    @Inject
    public DefaultFHIRExceptionHandler() {
        super();
    }

    @Override
    public Response toResponse(Throwable exception) {
        if (isFHIRResource(this.info)) {
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
        if (exception instanceof WebApplicationException) {
            final Response response = ((WebApplicationException) exception).getResponse();
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
