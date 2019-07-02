package gov.cms.dpc.api.auth;

import gov.cms.dpc.api.DPCAPIConfiguration;
import gov.cms.dpc.api.auth.annotations.PathAuthorizer;
import gov.cms.dpc.api.auth.annotations.Public;
import io.dropwizard.auth.AuthDynamicFeature;
import org.glassfish.jersey.server.model.AnnotatedMethod;

import javax.inject.Inject;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.ext.Provider;

/**
 * Wrapper class which injects the {@link MacaroonsAuthFilter} into the {@link AuthDynamicFeature} provider.
 * This way we can avoid authn/authz on public endpoints and dynamically determine when to apply security.
 */
@Provider
public class MacaroonsDynamicFeature implements DynamicFeature {

    private final MacaroonsAuthFilter filter;
    private final DPCAPIConfiguration config;

    @Inject
    public MacaroonsDynamicFeature(MacaroonsAuthFilter filter, DPCAPIConfiguration config) {
        this.filter = filter;
        this.config = config;
    }

    @Override
    public void configure(ResourceInfo resourceInfo, FeatureContext context) {

        // If encryption is disabled via config, skip it.
        if (this.config.isEncryptionDisabled()) {
            return;
        }

        final AnnotatedMethod am = new AnnotatedMethod(resourceInfo.getResourceMethod());

        // If we're public don't do anything
        if (am.isAnnotationPresent(Public.class)
                || (resourceInfo.getResourceClass().getAnnotation(Public.class) != null)) {
            return;
        }

        // Next, check for any authorization annotations on the class or method.
        // This should include all annotations specified in gov.cms.dpc.api.auth.annotations
        final boolean annotationOnClass = (resourceInfo.getResourceClass().getAnnotation(PathAuthorizer.class) != null);
        final boolean annotationOnMethod = am.isAnnotationPresent(PathAuthorizer.class);

        if (annotationOnClass || annotationOnMethod) {
            this.filter.setPathAuthorizer(am.getAnnotation(PathAuthorizer.class));
            context.register(this.filter);
        }
    }
}
