package gov.cms.dpc.fhir.dropwizard.handlers;

import gov.cms.dpc.fhir.FHIRMediaTypes;
import io.dropwizard.jersey.errors.LoggingExceptionMapper;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.OperationOutcome;

import javax.inject.Inject;
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
    Response handleFHIRException(Throwable exception) {
        final Response response = super.toResponse(exception);

        final int status = response.getStatus();
        final OperationOutcome outcome = new OperationOutcome();
        outcome.addIssue()
                .setSeverity(OperationOutcome.IssueSeverity.FATAL)
                .setDetails(new CodeableConcept().setText(exception.getMessage()));


        return Response.fromResponse(response)
                .status(status)
                .type(FHIRMediaTypes.FHIR_JSON)
                .entity(outcome)
                .build();
    }

    @Override
    Response handleNonFHIRException(Throwable exception) {
        return super.toResponse(exception);
    }
}
