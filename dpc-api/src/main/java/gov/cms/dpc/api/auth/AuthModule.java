package gov.cms.dpc.api.auth;

import com.google.inject.Binder;
import com.google.inject.TypeLiteral;
import gov.cms.dpc.api.DPCAPIConfiguration;
import gov.cms.dpc.api.auth.filters.PathAuthorizationFilter;
import gov.cms.dpc.api.auth.jwt.CaffeineJTICache;
import gov.cms.dpc.api.auth.jwt.IJTICache;
import gov.cms.dpc.api.auth.jwt.JwtKeyResolver;
import gov.cms.dpc.api.auth.macaroonauth.MacaroonsAuthenticator;
import gov.cms.dpc.api.auth.staticauth.StaticAuthFactory;
import gov.cms.dpc.api.auth.staticauth.StaticAuthFilter;
import gov.cms.dpc.api.auth.staticauth.StaticAuthenticator;
import gov.cms.dpc.macaroons.thirdparty.BakeryKeyPair;
import io.dropwizard.auth.Authenticator;
import io.dropwizard.auth.UnauthorizedHandler;
import io.jsonwebtoken.SigningKeyResolverAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.vyarus.dropwizard.guice.module.support.DropwizardAwareModule;

/**
 * {@link DropwizardAwareModule} for determining which authentication system to use.
 * if {@link DPCAPIConfiguration#isAuthenticationDisabled()} returns {@code true} then the {@link StaticAuthFilter} is used.
 * Otherwise, {@link PathAuthorizationFilter} is loaded.
 * <p>
 * The {@link StaticAuthFilter} should ONLY be used for testing
 */
public class AuthModule extends DropwizardAwareModule<DPCAPIConfiguration> {

    private static final Logger logger = LoggerFactory.getLogger(AuthModule.class);

    @Override
    public void configure(Binder binder) {

        final var authenticatorTypeLiteral = new TypeLiteral<Authenticator<DPCAuthCredentials, OrganizationPrincipal>>() {
        };

        if (configuration().isAuthenticationDisabled()) {
            logger.warn("AUTHENTICATION IS DISABLED!!! USE ONLY IN DEVELOPMENT");
            binder.bind(AuthFactory.class).to(StaticAuthFactory.class);
            binder.bind(authenticatorTypeLiteral).to(StaticAuthenticator.class);

        } else {
            binder.bind(DPCUnauthorizedHandler.class);
            binder.bind(UnauthorizedHandler.class).to(DPCUnauthorizedHandler.class);
            binder.bind(AuthFactory.class).to(DPCAuthFactory.class);
            binder.bind(authenticatorTypeLiteral).to(MacaroonsAuthenticator.class);
        }
        binder.bind(DPCAuthDynamicFeature.class);
        binder.bind(SigningKeyResolverAdapter.class).to(JwtKeyResolver.class);
        binder.bind(IJTICache.class).to(CaffeineJTICache.class);
        binder.bind(BakeryKeyPair.class).toProvider(new BakeryKeyPairProvider(this.getConfiguration()));
    }
}
