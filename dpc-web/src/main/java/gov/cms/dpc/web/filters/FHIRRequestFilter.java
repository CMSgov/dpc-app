package gov.cms.dpc.web.filters;

import gov.cms.dpc.web.core.FHIRMediaTypes;
import org.eclipse.jetty.server.Response;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.MediaType;
import java.util.List;

public class FHIRRequestFilter implements ContainerRequestFilter {

    @Override
    public void filter(ContainerRequestContext requestContext) {
        final List<MediaType> contentHeader = requestContext.getAcceptableMediaTypes();

        boolean valid = false;

        for (MediaType header : contentHeader) {
            valid = FHIRMediaTypes.isFHIRContent(header);
            if (valid)
                break;
        }

        if (!valid) {
            final IllegalArgumentException cause = new IllegalArgumentException("Must specify valid FHIR content type");
            throw new WebApplicationException(cause, Response.SC_UNSUPPORTED_MEDIA_TYPE);
        }
    }
}
