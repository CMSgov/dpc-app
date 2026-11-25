package gov.cms.dpc.api.auth;

import gov.cms.dpc.api.auth.annotations.PathAuthorizer;
import io.dropwizard.auth.AuthFilter;
import jakarta.ws.rs.container.ContainerRequestFilter;

/**
 * Interface for creating and injecting implementations of {@link AuthFilter}.
 */
public interface AuthFactory {

    ContainerRequestFilter createPathAuthorizer(PathAuthorizer pa);

    ContainerRequestFilter createAdminAuthorizer();

    ContainerRequestFilter createStandardAuthorizer();
}
