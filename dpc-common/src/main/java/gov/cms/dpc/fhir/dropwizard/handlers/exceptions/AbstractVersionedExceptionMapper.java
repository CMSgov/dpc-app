package gov.cms.dpc.fhir.dropwizard.handlers.exceptions;

import gov.cms.dpc.fhir.annotations.FHIR;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;

import java.lang.reflect.Method;

/**
 * Routes exceptions by the application-relative URL, including failures before resource matching completes.
 */
public abstract class AbstractVersionedExceptionMapper<E extends Throwable> implements ExceptionMapper<E> {

    private final ResourceInfo info;
    private final UriInfo uriInfo;

    protected AbstractVersionedExceptionMapper(ResourceInfo info, UriInfo uriInfo) {
        this.info = info;
        this.uriInfo = uriInfo;
    }

    @Override
    public Response toResponse(E exception) {
        String path = uriInfo.getPath();
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        boolean r4 = path.equals("r4") || path.startsWith("r4/");
        ResourceInfo resourceInfo = info;
        if (info.getResourceClass() == null && info.getResourceMethod() == null
                && (r4 || path.equals("v1") || path.startsWith("v1/"))) {

            // If we can't figure out if the error came from an stu3 or r4 endpoint, just punt.
            resourceInfo = new ResourceInfo() {
                @Override
                public Method getResourceMethod() {
                    return null;
                }

                @Override
                public Class<?> getResourceClass() {
                    return UnmatchedFHIRResource.class;
                }
            };
        }
        ExceptionMapper<E> handler = r4 ? createR4Handler(resourceInfo) : createStu3Handler(resourceInfo);
        return handler.toResponse(exception);
    }

    protected abstract ExceptionMapper<E> createStu3Handler(ResourceInfo info);

    protected abstract ExceptionMapper<E> createR4Handler(ResourceInfo info);

    @FHIR
    private static class UnmatchedFHIRResource {
    }
}
