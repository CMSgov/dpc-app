package gov.cms.dpc.fhir.dropwizard.filters;

import gov.cms.dpc.fhir.FHIRMediaTypes;
import org.eclipse.jetty.server.Response;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

public class FHIRRequestFilter implements ContainerRequestFilter {

    FHIRRequestFilter() {
        // Not used
    }

    @Override
    public void filter(ContainerRequestContext requestContext) {
        // Ensure the Accepts header is set to a FHIR media type
        checkAccepts(requestContext);
        // Ensure the Content-Type header is set to a FHIR media type
        checkContentType(requestContext);
    }

    private void checkAccepts(ContainerRequestContext requestContext) {
        final List<MediaType> contentHeader = requestContext.getAcceptableMediaTypes();

        if (!shortCircuitBooleanCheck(contentHeader, FHIRMediaTypes::isFHIRContent)) {
            throw new WebApplicationException("`Accept:` header must specify valid FHIR content type", Response.SC_UNSUPPORTED_MEDIA_TYPE);
        }
    }

    private void checkContentType(ContainerRequestContext requestContext) {
        final List<String> typeHeaders = requestContext.getHeaders().get(HttpHeaders.CONTENT_TYPE);

        if (!shortCircuitBooleanCheck(typeHeaders, (typeHeader) -> FHIRMediaTypes.isFHIRContent(MediaType.valueOf(typeHeader)))) {
            throw new WebApplicationException("`Accept:` header must specify valid FHIR content type", Response.SC_UNSUPPORTED_MEDIA_TYPE);
        }
    }

    private static <T> boolean shortCircuitBooleanCheck(Collection<T> collection, Predicate<T> checker) {
        boolean valid = false;
        for (T element : collection) {
            valid = checker.test(element);
            if (valid)
                break;
        }

        return valid;
    }
}
