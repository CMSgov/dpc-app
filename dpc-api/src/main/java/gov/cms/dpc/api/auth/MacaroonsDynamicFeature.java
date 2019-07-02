package gov.cms.dpc.api.auth;

import io.dropwizard.auth.AuthDynamicFeature;

import javax.inject.Inject;
import javax.ws.rs.ext.Provider;

@Provider
public class MacaroonsDynamicFeature extends AuthDynamicFeature {

    @Inject
    public MacaroonsDynamicFeature(MacaroonsAuthFilter filter) {
        super(filter);
    }
}
