package gov.cms.dpc.web;

import ca.uhn.fhir.context.FhirContext;
import gov.cms.dpc.web.core.FHIRMediaTypes;
import gov.cms.dpc.web.core.annotations.FHIR;
import io.dropwizard.jersey.errors.LoggingExceptionMapper;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.OperationOutcome;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
//@Singleton
public class FHIRExceptionHandler extends LoggingExceptionMapper<Throwable> {

    @Inject
    private FhirContext ctx;
    @Context
    private ResourceInfo info;

    @Inject
    public FHIRExceptionHandler() {
        super();
    }

    @Override
    public Response toResponse(Throwable exception) {
        final Response response = super.toResponse(exception);
        // If FHIR, do things
        if (isFHIRResource()) {
            final OperationOutcome outcome = new OperationOutcome();
            outcome.addIssue()
                    .setSeverity(OperationOutcome.IssueSeverity.FATAL)
                    .setDetails(new CodeableConcept().setText(exception.getMessage()));

            return Response.fromResponse(response)
                    .type(FHIRMediaTypes.FHIR_JSON)
                    .entity(outcome)
                    .build();
        }

        return response;
    }

    private boolean isFHIRResource() {
        return this.info.getResourceClass().getAnnotation(FHIR.class) != null ||
                this.info.getResourceMethod().getAnnotation(FHIR.class) != null;
    }
}
