package gov.cms.dpc.common.logging.filters;

import gov.cms.dpc.common.Constants;
import gov.cms.dpc.common.MDCConstants;
import gov.cms.dpc.common.utils.XSSSanitizerUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import javax.annotation.Priority;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import java.io.IOException;
import java.util.UUID;

@Priority(100)
public class GenerateRequestIdFilter implements ContainerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(GenerateRequestIdFilter.class);

    private final boolean useProvidedRequestId;

    public GenerateRequestIdFilter(boolean useProvidedRequestId){
        this.useProvidedRequestId = useProvidedRequestId;
    }


    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        MDC.clear();
        String requestId = requestContext.getHeaderString(Constants.DPC_REQUEST_ID_HEADER);
        if(requestId!=null && useProvidedRequestId) {
            MDC.put(MDCConstants.DPC_REQUEST_ID, requestId);
        }else{
            MDC.put(MDCConstants.DPC_REQUEST_ID, UUID.randomUUID().toString());
        }
        logger.info("resource_requested={}, method={}", XSSSanitizerUtil.sanitize(requestContext.getUriInfo().getPath()),requestContext.getMethod());
    }
}
