package gov.cms.dpc.fhir.dropwizard.filters;

import gov.cms.dpc.fhir.FHIRMediaTypes;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotSupportedException;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

public class FHIRRequestFilter implements ContainerRequestFilter {

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
            throw new BadRequestException("`Accept:` header is required.");
        }

        if (!shortCircuitBooleanCheck(acceptHeaders, FHIRMediaTypes::isFHIRContent) || (isExportRequest && acceptHeaders.contains(MediaType.WILDCARD_TYPE))) {
            throw new NotSupportedException("`Accept:` header must specify valid FHIR content type");
        }
    }

    private void checkContentType(ContainerRequestContext requestContext) {
        final List<String> typeHeaders = requestContext.getHeaders().get(HttpHeaders.CONTENT_TYPE);

        if (typeHeaders != null && !shortCircuitBooleanCheck(typeHeaders, (typeHeader) -> FHIRMediaTypes.isFHIRContent(MediaType.valueOf(typeHeader)))) {
            throw new NotSupportedException("`Content-Type:` header must specify valid FHIR content type");
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
