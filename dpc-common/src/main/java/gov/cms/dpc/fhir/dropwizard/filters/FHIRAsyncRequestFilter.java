package gov.cms.dpc.fhir.dropwizard.filters;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;

import java.util.List;

import static gov.cms.dpc.fhir.FHIRHeaders.PREFER_HEADER;
import static gov.cms.dpc.fhir.FHIRHeaders.PREFER_RESPOND_ASYNC;
import static gov.cms.dpc.fhir.FHIRMediaTypes.FHIR_JSON;

/**
 * Checks that the headers of the request has an 'accept' and 'prefer' header that matches
 * the https://hl7.org/fhir/2018May/async.html specification.
 */
public class FHIRAsyncRequestFilter implements ContainerRequestFilter {
    @Override
    public void filter(ContainerRequestContext requestContext) {
        final var headers = requestContext.getHeaders();

        // Accept
        final List<String> accepts = headers.get(HttpHeaders.ACCEPT);
        if (accepts == null || accepts.stream().noneMatch(value -> value.startsWith(FHIR_JSON))) {
            throw new BadRequestException("'application/fhir+json' is the only supported response format");
        }

        // Prefer
        final List<String> prefers = headers.get(PREFER_HEADER);
        if (prefers == null || prefers.size() != 1 || !PREFER_RESPOND_ASYNC.equalsIgnoreCase(prefers.get(0))) {
            throw new BadRequestException("One 'Prefer' header is required with a 'respond-async' value");
        }
    }
}
