package gov.cms.dpc.fhir.dropwizard.handlers.exceptions;

import gov.cms.dpc.fhir.annotations.FHIR;
import io.dropwizard.jersey.errors.LoggingExceptionMapper;

import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

@Provider
public abstract class AbstractFHIRExceptionHandler<E extends Throwable> extends LoggingExceptionMapper<E> {

    protected static final String ERROR_MSG_FMT = "There was an error processing your request. It has been logged (ID %016x): %s";
    @Context
    private ResourceInfo info;

    AbstractFHIRExceptionHandler() {
        super();
    }

    abstract Response handleFHIRException(E exception);

    abstract Response handleNonFHIRException(E exception);

    protected boolean isFHIRResource() {
        return (this.info.getResourceClass() != null && this.info.getResourceClass().getAnnotation(FHIR.class) != null) ||
                (this.info.getResourceMethod() != null && this.info.getResourceMethod().getAnnotation(FHIR.class) != null);
    }
}
