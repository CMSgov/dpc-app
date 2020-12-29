package gov.cms.dpc.common.logging.filters;

import gov.cms.dpc.common.Constants;
import gov.cms.dpc.common.MDCConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import java.io.IOException;

public class ExtractRequestIdFilter implements ContainerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(ExtractRequestIdFilter.class);

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        MDC.clear();
        String requestId = requestContext.getHeaderString(Constants.DPC_REQUEST_ID_HEADER);
        if(requestId!=null) {
            MDC.put(MDCConstants.DPC_REQUEST_ID, requestId);
        }
        logger.info("event_type=request-received resource_requested={}, method={}",requestContext.getUriInfo().getPath(),requestContext.getMethod());
    }
}
