package gov.cms.dpc.api.auth;

import io.dropwizard.auth.Authorizer;

import javax.inject.Inject;

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
