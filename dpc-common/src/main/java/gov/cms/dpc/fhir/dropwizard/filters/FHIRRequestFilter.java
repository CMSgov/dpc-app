package gov.cms.dpc.fhir.dropwizard.filters;

import gov.cms.dpc.fhir.FHIRMediaTypes;
import org.eclipse.jetty.server.Response;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

public class FHIRRequestFilter implements ContainerRequestFilter {

    @Inject
    FHIRRequestFilter() {
        // Not used
    }

    @Override
    public void filter(ContainerRequestContext requestContext) {
        // Ensure the `Accept` header is set to a FHIR media type
        checkAccepts(requestContext);
        // The content type header is optional, but if it's present, it has to be a FHIR resource type
        // HAPI does NOT set the Content-Type by default, so we can't require it as part of our requests, otherwise our test suite breaks
        checkContentType(requestContext);
    }

    private void checkAccepts(ContainerRequestContext requestContext) {
        final List<MediaType> acceptHeaders = requestContext.getAcceptableMediaTypes();
        final boolean isExportRequest = requestContext.getUriInfo().getRequestUri().toString().contains("$export");

        if (isExportRequest && acceptHeaders == null) {
            throw new WebApplicationException("`Accept:` header is required.", Response.SC_BAD_REQUEST);
        }

        if (!shortCircuitBooleanCheck(acceptHeaders, FHIRMediaTypes::isFHIRContent) || (isExportRequest && acceptHeaders.contains(MediaType.WILDCARD_TYPE))) {
            throw new WebApplicationException("`Accept:` header must specify valid FHIR content type", Response.SC_UNSUPPORTED_MEDIA_TYPE);
        }
    }

    private void checkContentType(ContainerRequestContext requestContext) {
        final List<String> typeHeaders = requestContext.getHeaders().get(HttpHeaders.CONTENT_TYPE);

        if (typeHeaders != null && !shortCircuitBooleanCheck(typeHeaders, (typeHeader) -> FHIRMediaTypes.isFHIRContent(MediaType.valueOf(typeHeader)))) {
            throw new WebApplicationException("`Content-Type:` header must specify valid FHIR content type", Response.SC_UNSUPPORTED_MEDIA_TYPE);
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
