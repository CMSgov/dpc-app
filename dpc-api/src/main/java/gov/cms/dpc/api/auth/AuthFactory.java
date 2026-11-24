package gov.cms.dpc.api.auth;

import gov.cms.dpc.api.auth.annotations.PathAuthorizer;
import io.dropwizard.auth.AuthFilter;

import java.security.Principal;

/**
 * Interface for creating and injecting implementations of {@link AuthFilter}.
 */
public interface AuthFactory {

    AuthFilter<DPCAuthCredentials, ? extends Principal> createPathAuthorizer(PathAuthorizer pa);

    AuthFilter<DPCAuthCredentials, ? extends Principal> createAdminAuthorizer();

    AuthFilter<DPCAuthCredentials, ? extends Principal> createStandardAuthorizer();
}
