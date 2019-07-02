package gov.cms.dpc.api.auth;

import io.dropwizard.auth.AuthDynamicFeature;

import javax.inject.Inject;
import javax.ws.rs.ext.Provider;

/**
 * Wrapper class which injects the {@link MacaroonsAuthFilter} into the {@link AuthDynamicFeature} provider.
 * This way we can avoid authn/authz on public endpoints and dynamically determine when to apply security.
 */
@Provider
public class MacaroonsDynamicFeature extends AuthDynamicFeature {

    @Inject
    public MacaroonsDynamicFeature(MacaroonsAuthFilter filter) {
        super(filter);
    }
}
