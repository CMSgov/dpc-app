package gov.cms.dpc.api.auth;

import gov.cms.dpc.api.auth.annotations.PathAuthorizer;
import io.dropwizard.auth.Authenticator;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Request;
import java.io.IOException;

/**
 * WARNING: DO NOT USE IN PRODUCTION
 */
@Priority(Priorities.AUTHENTICATION)
public class StaticAuthFilter extends DPCAuthFilter {

    @Inject
    StaticAuthFilter(Authenticator<String, OrganizationPrincipal> auth) {
        this.authenticator = auth;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        final Request request = requestContext.getRequest();

        this.authenticate(requestContext, "", null);
    }

    @Override
    void setPathAuthorizer(PathAuthorizer pa) {
        // Not used
    }


}
