package gov.cms.dpc.api.auth;

import gov.cms.dpc.api.auth.annotations.PathAuthorizer;
import io.dropwizard.auth.Authenticator;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Organization;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import java.io.IOException;

/**
 * WARNING: DO NOT USE IN PRODUCTION
 * <p>
 * This {@link DPCAuthFilter} always succeeds and passes an Organization ID as the credential to the {@link Authenticator}.
 * By default, it returns {@link StaticAuthFilter#DEFAULT_ORG_ID}, but if the {@link StaticAuthFilter#ORG_HEADER} is specified, the provided value is used instead.
 */
@Priority(Priorities.AUTHENTICATION)
public class StaticAuthFilter extends DPCAuthFilter {

    // Default organization ID to use, if no override is passed
    private static final String DEFAULT_ORG_ID = "46ac7ad6-7487-4dd0-baa0-6e2c8cae76a0";
    private static final String ORG_HEADER = "Organization";

    @Inject
    StaticAuthFilter(Authenticator<DPCAuthCredentials, OrganizationPrincipal> auth) {
        this.authenticator = auth;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        // We accept everything and pass it along to the authenticator

        final String headerString = requestContext.getHeaderString(ORG_HEADER);
        final String orgID = headerString == null ? DEFAULT_ORG_ID : headerString;

        final Organization org = new Organization();
        org.setId(new IdType("Organization", orgID));
        this.authenticate(requestContext, new DPCAuthCredentials(null, org, null, ""), null);
    }

    @Override
    void setPathAuthorizer(PathAuthorizer pa) {
        // Not used
    }


}
