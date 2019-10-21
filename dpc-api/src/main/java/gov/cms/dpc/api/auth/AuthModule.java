package gov.cms.dpc.api.auth;

import com.google.inject.Binder;
import com.google.inject.TypeLiteral;
import com.hubspot.dropwizard.guicier.DropwizardAwareModule;
import gov.cms.dpc.api.DPCAPIConfiguration;
import gov.cms.dpc.api.auth.jwt.JTICache;
import gov.cms.dpc.api.auth.jwt.JwtKeyResolver;
import gov.cms.dpc.macaroons.BakeryProvider;
import gov.cms.dpc.macaroons.MacaroonBakery;
import io.dropwizard.auth.Authenticator;
import io.jsonwebtoken.SigningKeyResolverAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

        if (getConfiguration().isAuthenticationDisabled()) {
            logger.warn("AUTHENTICATION IS DISABLED!!! USE ONLY IN DEVELOPMENT");
            binder.bind(AuthFactory.class).to(StaticAuthFactory.class);
            binder.bind(authenticatorTypeLiteral).to(StaticAuthenticator.class);

        } else {
            binder.bind(AuthFactory.class).to(DPCAuthFactory.class);
            binder.bind(authenticatorTypeLiteral).to(MacaroonsAuthenticator.class);
        }
        binder.bind(DPCAuthDynamicFeature.class);
        binder.bind(SigningKeyResolverAdapter.class).to(JwtKeyResolver.class);
        binder.bind(JTICache.class);
    }
}
