package gov.cms.dpc.api.auth;

import gov.cms.dpc.api.auth.annotations.PathAuthorizer;
import gov.cms.dpc.api.auth.filters.AdminAuthFilter;
import gov.cms.dpc.api.auth.filters.PathAuthorizationFilter;
import gov.cms.dpc.api.auth.filters.PrincipalInjectionAuthFilter;
import gov.cms.dpc.api.jdbi.TokenDAO;
import gov.cms.dpc.macaroons.MacaroonBakery;
import io.dropwizard.auth.AuthFilter;
import io.dropwizard.auth.Authenticator;

import javax.inject.Inject;

public class DPCAuthFactory implements AuthFactory {

    private final MacaroonBakery bakery;
    private final TokenDAO dao;
    private final Authenticator<DPCAuthCredentials, OrganizationPrincipal> authenticator;

    @Inject
    public DPCAuthFactory(MacaroonBakery bakery, Authenticator<DPCAuthCredentials, OrganizationPrincipal> authenticator, TokenDAO dao) {
        this.bakery = bakery;
        this.authenticator = authenticator;
        this.dao = dao;
    }

    @Override
    public DPCAuthFilter createPathAuthorizer(PathAuthorizer pa) {
        return new PathAuthorizationFilter(bakery, authenticator, dao, pa);
    }

    @Override
    public DPCAuthFilter createStandardAuthorizer() {
        return new PrincipalInjectionAuthFilter(bakery, authenticator, dao);
    }

    @Override
    public AuthFilter<DPCAuthCredentials, OrganizationPrincipal> createAdminAuthorizer() {
        return new AdminAuthFilter(bakery, authenticator);
    }
}
