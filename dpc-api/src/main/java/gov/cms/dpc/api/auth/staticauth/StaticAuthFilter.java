package gov.cms.dpc.api.auth.staticauth;

import gov.cms.dpc.api.auth.DPCAuthCredentials;
import gov.cms.dpc.common.MDCConstants;
import io.dropwizard.auth.AuthFilter;
import io.dropwizard.auth.Authenticator;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Response;
import org.slf4j.MDC;

import java.security.Principal;

/**
 * WARNING: DO NOT USE IN PRODUCTION
 * <p>
 * This filter passes an Organization ID and API version to the {@link Authenticator} without verifying a token.
 * By default, it returns {@link StaticAuthFilter#DEFAULT_ORG_ID}, but if the {@link StaticAuthFilter#ORG_HEADER} is specified, the provided value is used instead.
 */
@Priority(Priorities.AUTHENTICATION)
public class StaticAuthFilter extends AuthFilter<DPCAuthCredentials, Principal> {

    // Default organization ID to use, if no override is passed
    public static final String DEFAULT_ORG_ID = "46ac7ad6-7487-4dd0-baa0-6e2c8cae76a0";
    private static final String ORG_HEADER = "Organization";

    @Inject
    public StaticAuthFilter(Authenticator<DPCAuthCredentials, Principal> auth) {
        this.authenticator = auth;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) {
        // We accept everything and pass it along to the authenticator

        final String headerString = requestContext.getHeaderString(ORG_HEADER);
        final String orgID = headerString == null ? DEFAULT_ORG_ID : headerString;
        if (headerString == null) {
            logger.warn("AUTHENTICATION IS DISABLED - do not use in production");
        }

        // Now that we have the organization_id, set it in the logging context
        MDC.clear();
        MDC.put(MDCConstants.ORGANIZATION_ID, orgID);

        if (requestContext.getUriInfo().getPathSegments().isEmpty()) {
            throw new WebApplicationException("Unable to determine API version from request path",
                    Response.Status.INTERNAL_SERVER_ERROR);
        }
        final String version = requestContext.getUriInfo().getPathSegments().getFirst().getPath();
        this.authenticate(requestContext, new DPCAuthCredentials(null, orgID, null, "", version), null);
    }
}
