package gov.cms.dpc.api.auth.staticauth;

import gov.cms.dpc.api.auth.DPCAuthCredentials;
import gov.cms.dpc.api.auth.DPCAuthFilter;
import gov.cms.dpc.api.auth.OrganizationPrincipal;
import gov.cms.dpc.common.MDCConstants;
import io.dropwizard.auth.AuthFilter;
import io.dropwizard.auth.Authenticator;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Organization;
import org.slf4j.MDC;

/**
 * WARNING: DO NOT USE IN PRODUCTION
 * <p>
 * This {@link DPCAuthFilter} always succeeds and passes an Organization ID as the credential to the {@link Authenticator}.
 * By default, it returns {@link StaticAuthFilter#DEFAULT_ORG_ID}, but if the {@link StaticAuthFilter#ORG_HEADER} is specified, the provided value is used instead.
 */
@Priority(Priorities.AUTHENTICATION)
public class StaticAuthFilter extends AuthFilter<DPCAuthCredentials, OrganizationPrincipal> {

    // Default organization ID to use, if no override is passed
    private static final String DEFAULT_ORG_ID = "46ac7ad6-7487-4dd0-baa0-6e2c8cae76a0";
    private static final String ORG_HEADER = "Organization";

    @Inject
    public StaticAuthFilter(Authenticator<DPCAuthCredentials, OrganizationPrincipal> auth) {
        this.authenticator = auth;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) {
        // We accept everything and pass it along to the authenticator

        final String headerString = requestContext.getHeaderString(ORG_HEADER);
        final String orgID = headerString == null ? DEFAULT_ORG_ID : headerString;

        // Now that we have the organization_id, set it in the logging context
        MDC.clear();
        MDC.put(MDCConstants.ORGANIZATION_ID, orgID);

        final Organization org = new Organization();
        org.setId(new IdType("Organization", orgID));
        this.authenticate(requestContext, new DPCAuthCredentials(null, org, null, ""), null);
    }
}
