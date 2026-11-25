package gov.cms.dpc.api.auth;

import io.dropwizard.auth.Auth;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.model.Parameter.Source;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.internal.inject.AbstractValueParamProvider;
import org.glassfish.jersey.server.internal.inject.MultivaluedParameterExtractorProvider;
import org.glassfish.jersey.server.model.Parameter;
import org.glassfish.jersey.server.spi.internal.ValueParamProvider;

import java.lang.reflect.ParameterizedType;
import java.security.Principal;
import java.util.Optional;
import java.util.function.Function;

/**
 * DropWizard's normal AuthValueFactoryProvider can only handle one Principal, and we need to support both an stu3 and
 * r4 based OrganizationPrincipal.  This class handles that.
 *
 * Once we remove stu3 support we can get rid of this class entirely and go back to using AuthValueFactoryProvider.
 */
// TODO: Remove this class when we get rid of v1
public class OrganizationAuthValueFactoryProvider extends AbstractValueParamProvider {

    @Inject
    public OrganizationAuthValueFactoryProvider(MultivaluedParameterExtractorProvider extractorProvider) {
        super(() -> extractorProvider, Source.UNKNOWN);
    }

    @Override
    protected Function<ContainerRequest, ?> createValueProvider(Parameter parameter) {
        // We only care about authenticated endpoints
        if (!parameter.isAnnotationPresent(Auth.class)) {
            return null;
        }

        // What was the class passed into the method? (OrganizationPrincipal, or Optional)
        final Class<?> rawType = parameter.getRawType();

        // Do we have an Optional, and is its type a concrete class? (Not something like Optional<?>)
        final boolean optional = rawType == Optional.class
                && parameter.getType() instanceof ParameterizedType type
                && type.getActualTypeArguments()[0] instanceof Class<?>;

        // Get the parameter type whether it's wrapped in an Optional or not.
        final Class<?> principalType = optional
                ? (Class<?>) ((ParameterizedType) parameter.getType()).getActualTypeArguments()[0]
                : rawType;

        // Make sure the principalType is one of the two OrganizationPrinciples we support. (stu3 or r4)
        if (principalType != OrganizationPrincipal.class
                && principalType != gov.cms.dpc.api.auth.r4.OrganizationPrincipal.class) {
            return null;
        }

        return request -> {
            // If this endpoint isn't authenticated the principal can be null.  If it is authenticated and there's no
            // principal we have a problem.
            final Principal principal = request.getSecurityContext().getUserPrincipal();
            if (principal == null) {
                if (optional) {
                    return Optional.empty();
                }
                throw new IllegalStateException("Cannot inject a custom principal into unauthenticated request");
            }

            // Make sure we're using the right principal (stu3 OrganizationPrincipal vs r4)
            if (!principalType.isInstance(principal)) {
                throw new IllegalStateException("Authenticated principal does not match " + principalType.getName());
            }

            // Finally, if the parameter is an Optional wrap it and return
            return optional ? Optional.of(principal) : principal;
        };
    }

    public static class Binder extends AbstractBinder {
        @Override
        protected void configure() {
            bind(OrganizationAuthValueFactoryProvider.class).to(ValueParamProvider.class).in(Singleton.class);
        }
    }
}
