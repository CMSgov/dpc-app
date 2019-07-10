package gov.cms.dpc.api.auth;

import io.dropwizard.auth.Authorizer;

import javax.inject.Inject;

/**
 * Barebones authorizer for handling role-based access requests.
 * This always returns {@code true} (authorized) because we're not really using this right now, but it's wired up in case we want to do so in the future.
 * Authentication is still performed and handled by the {@link MacaroonsAuthenticator} class.
 */
public class MacaroonsAuthorizer implements Authorizer<OrganizationPrincipal> {

    @Inject
    MacaroonsAuthorizer() {
        // Not used
    }

    @Override
    public boolean authorize(OrganizationPrincipal principal, String role) {
        // Everything goes
        return true;
    }
}
