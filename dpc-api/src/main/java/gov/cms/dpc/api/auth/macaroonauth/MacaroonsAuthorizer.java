package gov.cms.dpc.api.auth.macaroonauth;

import gov.cms.dpc.api.auth.OrganizationPrincipal;
import io.dropwizard.auth.Authorizer;

import javax.annotation.Nullable;
import javax.ws.rs.container.ContainerRequestContext;

/**
 * Barebones authorizer for handling role-based access requests.
 * This always returns {@code true} (authorized) because we're not really using this right now, but it's wired up in case we want to do so in the future.
 * Authentication is still performed and handled by the {@link MacaroonsAuthenticator} class.
 */
public interface MacaroonsAuthorizer extends Authorizer<OrganizationPrincipal> {

    @Override
    default boolean authorize(OrganizationPrincipal principal, String role, @Nullable ContainerRequestContext context) {
        // Everything goes
        return true;
    }
}
