package gov.cms.dpc.api.auth;

import gov.cms.dpc.api.auth.annotations.PathAuthorizer;
import gov.cms.dpc.api.auth.annotations.Public;
import io.dropwizard.auth.Auth;
import io.dropwizard.auth.AuthDynamicFeature;
import org.glassfish.jersey.server.model.AnnotatedMethod;

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

    private final DPCAuthFilter filter;

    @Inject
    public DPCAuthDynamicFeature(DPCAuthFilter filter) {
        this.filter = filter;
    }

    @Override
    public void configure(ResourceInfo resourceInfo, FeatureContext context) {
        // Reset Path authorizer
        filter.setPathAuthorizer(null);

        final AnnotatedMethod am = new AnnotatedMethod(resourceInfo.getResourceMethod());

        // If we're public don't do anything
        if (isPublic(resourceInfo, am)) {
            return;
        }

        // Check for any @Auth annotated params
        if (authAnnotated(am)) {
            context.register(this.filter);
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
