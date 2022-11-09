package gov.cms.dpc.common.logging.filters;

import gov.cms.dpc.common.Constants;
import gov.cms.dpc.common.MDCConstants;
import gov.cms.dpc.common.utils.XSSSanitizerUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import javax.annotation.Priority;
import javax.ws.rs.WebApplicationException;
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
        String resourceRequested = XSSSanitizerUtil.sanitize(requestContext.getUriInfo().getPath());
        String method = requestContext.getMethod();
        String mediaType = requestContext.getMediaType() == null
                ? null
                : requestContext.getMediaType().getType();
        try {
            MDC.clear();
        } catch(IllegalStateException exception) {
            logger.info("mdc_clear_error={}, resource_requested={}, method={}, media_type={}, use_provided_request_id={}", exception.getMessage(), resourceRequested, method, mediaType, useProvidedRequestId);
            throw new WebApplicationException("Something went wrong, please try again. If this continues, contact DPC admin.");
        }
        String requestId = requestContext.getHeaderString(Constants.DPC_REQUEST_ID_HEADER) != null && useProvidedRequestId
                ? requestContext.getHeaderString(Constants.DPC_REQUEST_ID_HEADER)
                : UUID.randomUUID().toString();
        MDC.put(MDCConstants.DPC_REQUEST_ID, requestId);
        logger.info("resource_requested={}, method={}, media_type={}, request_id={}, use_provided_request_id={}", resourceRequested, method, mediaType, requestId, useProvidedRequestId);
    }
}
