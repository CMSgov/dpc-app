package gov.cms.dpc.api.auth;

import gov.cms.dpc.api.auth.annotations.PathAuthorizer;
import gov.cms.dpc.api.auth.annotations.Public;
import io.dropwizard.auth.Auth;
import io.dropwizard.auth.AuthDynamicFeature;
import org.glassfish.jersey.server.model.AnnotatedMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.ext.Provider;
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

        // If we're public don't do anything
        if (isPublic(resourceInfo, am)) {
            return;
        }

        // Check for any @Auth annotated params
        if (authAnnotated(am)) {
            logger.trace("Registering Auth param on method {}", am.toString());
            context.register(this.factory.createStandardAuthorizer());
            return;
        }

        // Next, check for any authorization annotations on the class or method.
        // This should include all annotations specified in gov.cms.dpc.api.auth.annotations
        final boolean annotationOnClass = (resourceInfo.getResourceClass().getAnnotation(PathAuthorizer.class) != null);
        final boolean annotationOnMethod = am.isAnnotationPresent(PathAuthorizer.class);

        if (annotationOnClass || annotationOnMethod) {
            logger.warn("Setting path authorizer");
            final PathAuthorizer pa = am.getAnnotation(PathAuthorizer.class);
            context.register(this.factory.createPathAuthorizer(pa));
        }
    }

    private boolean isPublic(ResourceInfo resourceInfo, AnnotatedMethod am) {
        return am.isAnnotationPresent(Public.class)
                || (resourceInfo.getResourceClass().getAnnotation(Public.class) != null);
    }

    private boolean authAnnotated(AnnotatedMethod am) {
        final Annotation[][] parameterAnnotations = am.getParameterAnnotations();

        for (Annotation[] parameterAnnotation : parameterAnnotations) {
            for (final Annotation annotation : parameterAnnotation) {
                if (annotation instanceof Auth) {
                    return true;
                }
            }
        }
        return false;
    }
}
