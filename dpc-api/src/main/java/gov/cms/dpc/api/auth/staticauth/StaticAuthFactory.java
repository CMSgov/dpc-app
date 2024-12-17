package gov.cms.dpc.api.auth.staticauth;

import com.google.inject.Inject;
import gov.cms.dpc.api.auth.AuthFactory;
import gov.cms.dpc.api.auth.DPCAuthCredentials;
import gov.cms.dpc.api.auth.OrganizationPrincipal;
import gov.cms.dpc.api.auth.annotations.PathAuthorizer;
import io.dropwizard.auth.AuthFilter;
import io.dropwizard.auth.Authenticator;


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
