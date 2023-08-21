package gov.cms.dpc.common.logging.filters;

import gov.cms.dpc.common.Constants;
import gov.cms.dpc.common.MDCConstants;
import gov.cms.dpc.common.utils.XSSSanitizerUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;

public class LogResponseFilter implements ContainerResponseFilter{
    private static final Logger logger = LoggerFactory.getLogger(LogResponseFilter.class);

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext){
        String requestId = MDC.get(MDCConstants.DPC_REQUEST_ID);
        String resourceRequested = XSSSanitizerUtil.sanitize(requestContext.getUriInfo().getPath());
        String mediaType = requestContext.getMediaType() == null
                ? null
                : requestContext.getMediaType().toString();
        String method = requestContext.getMethod();
        int status = responseContext.getStatus();

        if(requestId!=null){
            responseContext.getHeaders().add(Constants.DPC_REQUEST_ID_HEADER, requestId);
        }
        logger.info("resource_requested={}, media_type={}, method={}, status={}", resourceRequested, mediaType, method, status);
    }
}
