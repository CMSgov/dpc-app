package gov.cms.dpc.api.auth;

import com.google.inject.Binder;
import com.google.inject.TypeLiteral;
import com.hubspot.dropwizard.guicier.DropwizardAwareModule;
import gov.cms.dpc.api.DPCAPIConfiguration;
import io.dropwizard.auth.Authenticator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthModule extends DropwizardAwareModule<DPCAPIConfiguration> {

    private static final Logger logger = LoggerFactory.getLogger(AuthModule.class);

    @Override
    public void configure(Binder binder) {

        final var authenticatorTypeLiteral = new TypeLiteral<Authenticator<String, OrganizationPrincipal>>() {
        };

        if (getConfiguration().isAuthenticationDisabled()) {
            logger.warn("AUTHENTICATION IS DISABLED!!! USE ONLY IN DEVELOPMENT");
            binder.bind(authenticatorTypeLiteral).to(StaticAuthenticator.class);
            binder.bind(DPCAuthFilter.class).to(StaticAuthFilter.class);
        } else {
            binder.bind(DPCAuthFilter.class).to(MacaroonsAuthFilter.class);
            binder.bind(authenticatorTypeLiteral).to(MacaroonsAuthenticator.class);
        }
        binder.bind(DPCAuthDynamicFeature.class);
    }
}
