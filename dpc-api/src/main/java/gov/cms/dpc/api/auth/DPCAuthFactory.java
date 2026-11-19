package gov.cms.dpc.api.auth;

import gov.cms.dpc.api.auth.annotations.PathAuthorizer;
import gov.cms.dpc.api.auth.filters.AdminAuthFilter;
import gov.cms.dpc.api.auth.filters.PathAuthorizationFilter;
import gov.cms.dpc.api.auth.filters.PrincipalInjectionAuthFilter;
import gov.cms.dpc.api.auth.macaroonauth.r4.MacaroonsAuthenticator;
import gov.cms.dpc.api.jdbi.TokenDAO;
import gov.cms.dpc.macaroons.MacaroonBakery;
import io.dropwizard.auth.AuthFilter;
import io.dropwizard.auth.Authenticator;
import jakarta.inject.Inject;

public class DPCAuthFactory implements AuthFactory {

    private final MacaroonBakery bakery;
    private final TokenDAO dao;
    private final gov.cms.dpc.api.auth.macaroonauth.stu3.MacaroonsAuthenticator stu3authenticator;
    private final MacaroonsAuthenticator r4Authenticator;
    private final DPCUnauthorizedHandler dpc401handler;

    @Inject
    public DPCAuthFactory(MacaroonBakery bakery, gov.cms.dpc.api.auth.macaroonauth.stu3.MacaroonsAuthenticator stu3authenticator,
                          MacaroonsAuthenticator r4Authenticator, TokenDAO dao, DPCUnauthorizedHandler dpc401handler) {
        this.bakery = bakery;
        this.stu3authenticator = stu3authenticator;
        this.r4Authenticator = r4Authenticator;
        this.dao = dao;
        this.dpc401handler = dpc401handler;
    }

    @Override
    public DPCAuthFilter createPathAuthorizer(PathAuthorizer pa) {
        return new PathAuthorizationFilter(bakery, pathAuthenticator(), dao, pa, dpc401handler);
    }

    Authenticator<DPCAuthCredentials, OrganizationPrincipal> pathAuthenticator() {
        return credentials -> {
            final String version = credentials.getApiVersion();
            if ("v1".equals(version)) {
                return stu3authenticator.authenticate(credentials);
            }
            if ("v3".equals(version)) {
                return r4Authenticator.authenticate(credentials);
            }
            throw new IllegalStateException("No path authenticator configured for API version: " + version);
        };
    }

    @Override
    public DPCAuthFilter createStandardAuthorizer() {
        return new PrincipalInjectionAuthFilter(bakery, stu3authenticator, dao, dpc401handler);
    }

    @Override
    public AuthFilter<DPCAuthCredentials, OrganizationPrincipal> createAdminAuthorizer() {
        return new AdminAuthFilter(bakery, stu3authenticator);
    }
}
