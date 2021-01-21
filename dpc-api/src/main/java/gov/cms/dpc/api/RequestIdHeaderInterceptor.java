package gov.cms.dpc.api;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.rest.client.api.IHttpRequest;
import gov.cms.dpc.common.Constants;
import gov.cms.dpc.common.MDCConstants;
import org.slf4j.MDC;

public class RequestIdHeaderInterceptor {

    @Hook(Pointcut.CLIENT_REQUEST)
    public void interceptRequest(IHttpRequest theRequest) {
        final String requestId = MDC.get(MDCConstants.DPC_REQUEST_ID);
        if(requestId!=null){
            theRequest.addHeader(Constants.DPC_REQUEST_ID_HEADER, requestId);
        }
    }
}
