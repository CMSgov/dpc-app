package gov.cms.dpc.common.logging.filters;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;
//import com.google.inject.Provider;
//import gov.cms.dpc.common.Constants;
//import gov.cms.dpc.common.MDCConstants;
//import gov.cms.dpc.common.utils.XSSSanitizerUtil;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.slf4j.MDC;
//
//import javax.servlet.http.HttpServletRequest;
//import javax.ws.rs.container.ContainerRequestContext;
//import javax.ws.rs.container.ContainerResponseContext;
//import javax.ws.rs.container.ContainerResponseFilter;
//import javax.ws.rs.core.Context;

public class LogMaskingFilter extends Filter<ILoggingEvent> {
    @Override
    public FilterReply decide(ILoggingEvent event) {
        if (event.getMessage() != null && event.getMessage().contains("potato")) {
            String newMessage = event.getMessage().replaceAll("potato","<(*_*)>");
            ((LoggingEvent) event).setMessage(newMessage);
//            ((ch.qos.logback.classic.spi.LoggingEvent) event).setMessage(maskedMessage);
        }
        return FilterReply.NEUTRAL;
    }
}
