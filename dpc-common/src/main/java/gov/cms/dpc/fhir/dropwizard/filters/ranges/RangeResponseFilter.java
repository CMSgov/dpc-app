package gov.cms.dpc.fhir.dropwizard.filters.ranges;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.io.OutputStream;

/**
 * A {@link ContainerResponseFilter} capable to handle ranged requests.
 * Shamelessly stolen from: https://github.com/heruan/jaxrs-range-filter/blob/master/src/main/java/to/lova/jaxrs/filter/RangeResponseFilter.java
 */
public class RangeResponseFilter implements ContainerResponseFilter {

    private static final Logger logger = LoggerFactory.getLogger(RangeResponseFilter.class);

    private static final String ACCEPT_RANGES = "Accept-Ranges";
    private static final String BYTES_RANGE = "bytes";
    private static final String RANGE = "Range";
    private static final String IF_RANGE = "If-Range";

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {

        responseContext.getHeaders().add(ACCEPT_RANGES, BYTES_RANGE);

        if (!requestContext.getHeaders().containsKey(RANGE)) {
            logger.trace("Missing Range header, not applying filter");
        } else if (requestContext.getHeaders().containsKey(IF_RANGE)) {
            String ifRangeHeader = requestContext.getHeaderString(IF_RANGE);
            if (responseContext.getHeaders().containsKey(HttpHeaders.ETAG)
                    && responseContext.getHeaderString(HttpHeaders.ETAG).equals(ifRangeHeader)) {
                this.applyFilter(requestContext, responseContext);
            }
            if (responseContext.getHeaders().containsKey(HttpHeaders.LAST_MODIFIED)
                    && responseContext.getHeaderString(HttpHeaders.LAST_MODIFIED).equals(ifRangeHeader)) {
                this.applyFilter(requestContext, responseContext);
            }
        } else {
            this.applyFilter(requestContext, responseContext);
        }
    }

    private void applyFilter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {

        String rangeHeader = requestContext.getHeaderString(RANGE);
        String contentType = responseContext.getMediaType().toString();
        OutputStream originOutputStream = responseContext.getEntityStream();
        RangedOutputStream rangedOutputStream = new RangedOutputStream(originOutputStream, rangeHeader, contentType,
                responseContext.getHeaders());
        responseContext.setStatus(Response.Status.PARTIAL_CONTENT.getStatusCode());
        responseContext.setEntityStream(rangedOutputStream);

    }

}
