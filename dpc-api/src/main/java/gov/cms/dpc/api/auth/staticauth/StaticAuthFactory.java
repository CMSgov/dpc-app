package gov.cms.dpc.api.auth.staticauth;

import gov.cms.dpc.api.auth.AuthFactory;
import gov.cms.dpc.api.auth.DPCAuthCredentials;
import gov.cms.dpc.api.auth.annotations.PathAuthorizer;
import io.dropwizard.auth.AuthFilter;
import io.dropwizard.auth.Authenticator;
import jakarta.inject.Inject;

import java.security.Principal;

/**
 * Implementation of {@link AuthFactory} that always injects a {@link StaticAuthFilter}
 */
public class StaticAuthFactory implements AuthFactory {

    private final AuthFilter<DPCAuthCredentials, Principal> authFilter;

    @Inject
    public StaticAuthFactory(Authenticator<DPCAuthCredentials, Principal> auth) {
        this.authFilter = new StaticAuthFilter(auth);
    }

    @Override
    public AuthFilter<DPCAuthCredentials, Principal> createPathAuthorizer(PathAuthorizer pa) {
        return this.authFilter;
    }

    @Override
    public AuthFilter<DPCAuthCredentials, Principal> createStandardAuthorizer() {
        return this.authFilter;
    }

    @Override
    public AuthFilter<DPCAuthCredentials, Principal> createAdminAuthorizer() {
        return this.authFilter;
    }
}
