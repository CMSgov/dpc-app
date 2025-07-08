package gov.cms.dpc.common.logging.filters;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.UriInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class LogHeaderFilter implements ContainerRequestFilter {
	private static final Logger logger = LoggerFactory.getLogger(LogHeaderFilter.class);
	final String headerKey;

	public LogHeaderFilter(String headerKey) { this.headerKey = headerKey; }

	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {
		String headerValue = requestContext.getHeaderString(headerKey);

		// Escape commas so they don't get confused with field separators
		if(headerValue != null) {
			headerValue = headerValue.replace(",", "\\,");
		}

        UriInfo uriInfo = requestContext.getUriInfo();
        String uriValue = null;
        if(uriInfo != null) {
            uriValue = String.valueOf(uriInfo.getRequestUri());
            uriValue = uriValue.replaceAll("[\n\r]", "_");
        }
		logger.info("{}={}, uri={}", headerKey, headerValue, uriValue);
	}
}
