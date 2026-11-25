package gov.cms.dpc.api.auth.filters;

import gov.cms.dpc.api.auth.DPCAuthCredentials;
import gov.cms.dpc.api.auth.DPCAuthFilter;
import gov.cms.dpc.api.auth.DPCUnauthorizedHandler;
import gov.cms.dpc.api.jdbi.TokenDAO;
import gov.cms.dpc.macaroons.MacaroonBakery;
import io.dropwizard.auth.Authenticator;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import java.security.Principal;
import java.util.UUID;

/**
 * Implementation of {@link DPCAuthFilter} that is use when an {@link io.dropwizard.auth.Auth} annotated method is called.
 * This passes the organization ID to the authenticator, which creates the correct OrganizationPrincipal.
 */
@Priority(Priorities.AUTHENTICATION)
public class PrincipalInjectionAuthFilter extends DPCAuthFilter {

    public PrincipalInjectionAuthFilter(MacaroonBakery bakery, Authenticator<DPCAuthCredentials, Principal> auth, TokenDAO dao, DPCUnauthorizedHandler dpc401handler) {
        super(bakery, auth, dao, dpc401handler);
    }

    @Override
    protected DPCAuthCredentials buildCredentials(String macaroon, UUID organizationID, UriInfo uriInfo) {
        if (uriInfo.getPathSegments().isEmpty()) {
            throw new WebApplicationException("Unable to determine API version from request path",
                    Response.Status.INTERNAL_SERVER_ERROR);
        }
        return new DPCAuthCredentials(macaroon, organizationID.toString(), null, null,
                uriInfo.getPathSegments().getFirst().getPath());
    }
}
