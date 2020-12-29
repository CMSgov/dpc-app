package gov.cms.dpc.attribution;

import gov.cms.dpc.common.Constants;
import gov.cms.dpc.common.MDCConstants;
import org.slf4j.MDC;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;

public class RequestFilter implements ContainerRequestFilter {
    @Override
    public void filter(ContainerRequestContext containerRequestContext){
        final String requestId = containerRequestContext.getHeaderString(Constants.DPC_REQUEST_ID_HEADER);
        if(requestId!=null)
        {
            MDC.put(MDCConstants.DPC_REQUEST_ID, requestId);
        }

    }
}
