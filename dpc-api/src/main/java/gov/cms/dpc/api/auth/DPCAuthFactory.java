package gov.cms.dpc.api.auth;

import gov.cms.dpc.api.auth.annotations.PathAuthorizer;
import gov.cms.dpc.common.hibernate.auth.DPCAuthManagedSessionFactory;
import gov.cms.dpc.macaroons.MacaroonBakery;
import io.dropwizard.auth.Authenticator;

import javax.inject.Inject;

public class DPCAuthFactory implements AuthFactory {

    private final MacaroonBakery bakery;
    private final DPCAuthManagedSessionFactory factory;
    private final Authenticator<DPCAuthCredentials, OrganizationPrincipal> authenticator;

    @Inject
    public DPCAuthFactory(MacaroonBakery bakery, Authenticator<DPCAuthCredentials, OrganizationPrincipal> authenticator, DPCAuthManagedSessionFactory factory) {
        this.bakery = bakery;
        this.authenticator = authenticator;
        this.factory = factory;
    }

    @Override
    public DPCAuthFilter createPathAuthorizer(PathAuthorizer pa) {
        return new PathAuthorizationFilter(bakery, authenticator, factory, pa);
    }

    @Override
    public DPCAuthFilter createStandardAuthorizer() {
        return new PrincipalInjectionAuthFilter(bakery, authenticator, factory);
    }
}
