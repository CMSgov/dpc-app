package gov.cms.dpc.api.auth;

import gov.cms.dpc.api.auth.annotations.PathAuthorizer;
import io.dropwizard.auth.Authenticator;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import java.io.IOException;

/**
 * WARNING: DO NOT USE IN PRODUCTION
 */
@Priority(Priorities.AUTHENTICATION)
public class StaticAuthFilter extends DPCAuthFilter {

    // Default organization ID to use, if no override is passed
    private static final String DEFAULT_ORG_ID = "46ac7ad6-7487-4dd0-baa0-6e2c8cae76a0";
    private static final String ORG_HEADER = "Organization";

    @Inject
    StaticAuthFilter(Authenticator<String, OrganizationPrincipal> auth) {
        this.authenticator = auth;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        // We accept everything and pass it along to the authenticator

        final String orgID = requestContext.getHeaderString(ORG_HEADER);
        this.authenticate(requestContext, orgID == null ? DEFAULT_ORG_ID : orgID, null);
    }

    @Override
    void setPathAuthorizer(PathAuthorizer pa) {
        // Not used
    }


}
