package gov.cms.dpc.api.auth;

import gov.cms.dpc.api.auth.annotations.PathAuthorizer;
import io.dropwizard.auth.AuthFilter;
import io.dropwizard.auth.Authenticator;

import javax.inject.Inject;

/**
 * Implementation of {@link AuthFactory} that always injects a {@link StaticAuthFilter}
 */
public class StaticAuthFactory implements AuthFactory {

    private final Authenticator<DPCAuthCredentials, OrganizationPrincipal> authenticator;
    private final AuthFilter<DPCAuthCredentials, OrganizationPrincipal> authFilter;

    @Inject
    public StaticAuthFactory(Authenticator<DPCAuthCredentials, OrganizationPrincipal> auth) {
        this.authenticator = auth;
        this.authFilter = new StaticAuthFilter(this.authenticator);
    }

    @Override
    public AuthFilter<DPCAuthCredentials, OrganizationPrincipal> createPathAuthorizer(PathAuthorizer pa) {
        return this.authFilter;
    }

    @Override
    public AuthFilter<DPCAuthCredentials, OrganizationPrincipal> createStandardAuthorizer() {
        return this.authFilter;
    }

    @Override
    public AuthFilter<DPCAuthCredentials, OrganizationPrincipal> createAdminAuthorizer() {
        return this.authFilter;
    }
}
