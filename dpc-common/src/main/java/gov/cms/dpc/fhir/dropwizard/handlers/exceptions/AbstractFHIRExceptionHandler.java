package gov.cms.dpc.fhir.dropwizard.handlers.exceptions;

import gov.cms.dpc.fhir.annotations.FHIR;
import io.dropwizard.jersey.errors.LoggingExceptionMapper;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Response;

import java.lang.reflect.Method;

public abstract class AbstractFHIRExceptionHandler<E extends Throwable> extends LoggingExceptionMapper<E> {

    private final ResourceInfo info;

    protected static final String ERROR_MSG_FMT = "There was an error processing your request. It has been logged (ID %016x): %s";

    protected AbstractFHIRExceptionHandler(ResourceInfo info) {
        super();
        this.info = info;
    }

    protected abstract Response handleFHIRException(E exception);

    protected abstract Response handleNonFHIRException(E exception);

    protected boolean isFHIRResource() {
        if (this.info.getResourceClass() != null && this.info.getResourceClass().isAnnotationPresent(FHIR.class)) {
            return true;
        }

        Method method = this.info.getResourceMethod();
        // Make sure we check if a parent class has the @FHIR annotation
        while (method != null) {
            if (method.isAnnotationPresent(FHIR.class)) {
                return true;
            }
            Class<?> parent = method.getDeclaringClass().getSuperclass();
            if (parent == null) {
                return false;
            }
            try {
                method = parent.getMethod(method.getName(), method.getParameterTypes());
            } catch (NoSuchMethodException e) {
                return false;
            }
        }
        // We shouldn't get here, but we need to be exhaustive
        return false;
    }

    /**
     * Format the randomly generated exception ID as a 16byte Hex value, padding with zeros if required
     * @param logID - {@link Long} exception ID to format
     * @return - {@link String} Hex formatted exception ID
     */
    protected String exceptionIDtoHex(long logID) {
        return String.format("%016x", logID);
    }
}
