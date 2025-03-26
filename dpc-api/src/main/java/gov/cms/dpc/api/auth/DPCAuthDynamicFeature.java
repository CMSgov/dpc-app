package gov.cms.dpc.api.auth;

import gov.cms.dpc.api.auth.annotations.AdminOperation;
import gov.cms.dpc.api.auth.annotations.Authorizer;
import gov.cms.dpc.api.auth.annotations.PathAuthorizer;
import io.dropwizard.auth.AuthDynamicFeature;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.DynamicFeature;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.FeatureContext;
import jakarta.ws.rs.ext.Provider;
import org.glassfish.jersey.server.model.AnnotatedMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;

/**
 * Wrapper class which injects the DPC specific authenticators into the {@link AuthDynamicFeature} provider.
 * This way we can avoid authn/authz on public endpoints and dynamically determine when to apply security.
 */
@Provider
public class DPCAuthDynamicFeature implements DynamicFeature {

    private static final Logger logger = LoggerFactory.getLogger(DPCAuthDynamicFeature.class);

    private final AuthFactory factory;

    @Inject
    public DPCAuthDynamicFeature(AuthFactory factory) {
        this.factory = factory;
    }

    @Override
    public void configure(ResourceInfo resourceInfo, FeatureContext context) {


        final AnnotatedMethod am = new AnnotatedMethod(resourceInfo.getResourceMethod());

        // Check for Admin annotated params
        if (isMethodClassAnnotated(AdminOperation.class, resourceInfo, am)) {
            logger.trace("Registering Admin authorizer on method {}", am);
            context.register(this.factory.createAdminAuthorizer());
            return;
        }

        // Check for @PathAuthorizer annotated param
        if (isMethodClassAnnotated(PathAuthorizer.class, resourceInfo, am)) {
            logger.trace("Registering PathAuthorizer param on method {}", am);
            final PathAuthorizer pa = am.getAnnotation(PathAuthorizer.class);
            context.register(this.factory.createPathAuthorizer(pa));
            return;
        }

        // Check for @Authorized annotated param
        if (isMethodClassAnnotated(Authorizer.class, resourceInfo, am)) {
            logger.trace("Registering Auth param on method {}", am);
            context.register(this.factory.createStandardAuthorizer());
        }
    }

    private boolean isMethodClassAnnotated(Class<? extends Annotation> annotation, ResourceInfo resourceInfo, AnnotatedMethod am) {
        return am.isAnnotationPresent(annotation)
                || (resourceInfo.getResourceClass().getAnnotation(annotation) != null);
    }
}
