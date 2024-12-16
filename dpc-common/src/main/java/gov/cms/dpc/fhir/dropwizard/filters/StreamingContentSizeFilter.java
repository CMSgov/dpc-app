package gov.cms.dpc.fhir.dropwizard.filters;

import org.apache.http.HttpHeaders;

import javax.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

/**
 * {@link ContainerResponseFilter} that handles setting the {@link HttpHeaders#CONTENT_LENGTH} header, if the {@link StreamingContentSizeFilter#X_CONTENT_LENGTH} header is also set.
 * This works around Jersey's insistence on chunking everything up and screwing with our streaming downloads
 *
 * @see <a href="https://stackoverflow.com/questions/24572771/how-do-i-set-content-length-when-returning-large-objects-in-jersey-jax-rs-server">Stackoverflow answer</a>
 */
@Provider
public class StreamingContentSizeFilter implements ContainerResponseFilter {

    public static final String X_CONTENT_LENGTH = "X-Content-Length";

    @Inject
    public StreamingContentSizeFilter() {
        // Not used
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        final String contentLength = responseContext.getHeaderString(X_CONTENT_LENGTH);
        if (contentLength != null) {
            responseContext.getHeaders().remove(HttpHeaders.TRANSFER_ENCODING);
            responseContext.getHeaders().remove(X_CONTENT_LENGTH);
            responseContext.getHeaders().putSingle(HttpHeaders.CONTENT_LENGTH, contentLength);
        }
    }
}
