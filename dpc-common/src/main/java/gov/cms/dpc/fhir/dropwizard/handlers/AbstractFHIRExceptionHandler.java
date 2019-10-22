package gov.cms.dpc.fhir.dropwizard.handlers;

import gov.cms.dpc.fhir.annotations.FHIR;
import io.dropwizard.jersey.errors.LoggingExceptionMapper;
import org.hl7.fhir.dstu3.model.OperationOutcome;

import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

@Provider
public abstract class AbstractFHIRExceptionHandler<E extends Throwable> extends LoggingExceptionMapper<E> {

    @Context
    private ResourceInfo info;

    AbstractFHIRExceptionHandler() {
        super();
    }

    abstract Response handleFHIRException(E exception);

    abstract Response handleNonFHIRException(E exception);

    @Override
    public Response toResponse(E exception) {
        final Response response = super.toResponse(exception);
        if (isFHIRResource()) {
            return handleFHIRException(exception);
        }

        return handleNonFHIRException(exception);
    }

    protected boolean isFHIRResource() {
        return (this.info.getResourceClass() != null && this.info.getResourceClass().getAnnotation(FHIR.class) != null) ||
                (this.info.getResourceMethod() != null && this.info.getResourceMethod().getAnnotation(FHIR.class) != null);
    }
}
