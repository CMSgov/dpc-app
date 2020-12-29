package gov.cms.dpc.api;

import com.google.inject.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Context;

public class ResponseLoggingFilter implements ContainerResponseFilter {

    @Context
    private Provider<HttpServletRequest> request;

    private static final Logger logger = LoggerFactory.getLogger(ResponseLoggingFilter.class);

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext){
        logger.info("resource_requested={}, method={}, status={}",requestContext.getUriInfo().getPath(),requestContext.getMethod(),responseContext.getStatus());
    }
}
